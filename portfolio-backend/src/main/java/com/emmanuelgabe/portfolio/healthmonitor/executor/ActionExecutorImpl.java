package com.emmanuelgabe.portfolio.healthmonitor.executor;

import com.emmanuelgabe.portfolio.entity.HealthProposedAction;
import com.emmanuelgabe.portfolio.healthmonitor.HealthMonitorProperties;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionStatus;
import com.emmanuelgabe.portfolio.healthmonitor.tools.DockerServiceResolver;
import com.emmanuelgabe.portfolio.repository.HealthProposedActionRepository;
import com.emmanuelgabe.portfolio.service.SearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Single component responsible for executing approved corrective actions.
 *
 * <p>Enforces a per-hour mutation rate limit so a runaway agent (or a
 * compromised admin account) cannot restart services in a loop. Each action
 * type maps to a private method; adding a new action type requires adding a
 * case here AND updating the {@link com.emmanuelgabe.portfolio.healthmonitor.action.ActionType} enum.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.health-monitor.enabled", havingValue = "true")
public class ActionExecutorImpl implements ActionExecutor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> PARAMS_TYPE = new TypeReference<>() { };

    private final HealthProposedActionRepository actionRepository;
    private final HealthMonitorProperties properties;
    private final RedisConnectionFactory redisConnectionFactory;
    private final RabbitTemplate rabbitTemplate;
    private final SearchService searchService;
    private final ObjectProvider<JobLauncher> jobLauncherProvider;
    private final ObjectProvider<Job> imageReprocessingJobProvider;
    private final DockerClient dockerClient;
    private final DockerServiceResolver resolver;

    public ActionExecutorImpl(HealthProposedActionRepository actionRepository,
                              HealthMonitorProperties properties,
                              RedisConnectionFactory redisConnectionFactory,
                              RabbitTemplate rabbitTemplate,
                              SearchService searchService,
                              ObjectProvider<JobLauncher> jobLauncherProvider,
                              ObjectProvider<Job> imageReprocessingJobProvider,
                              DockerClient dockerClient) {
        this.actionRepository = actionRepository;
        this.properties = properties;
        this.redisConnectionFactory = redisConnectionFactory;
        this.rabbitTemplate = rabbitTemplate;
        this.searchService = searchService;
        this.jobLauncherProvider = jobLauncherProvider;
        this.imageReprocessingJobProvider = imageReprocessingJobProvider;
        this.dockerClient = dockerClient;
        this.resolver = new DockerServiceResolver(dockerClient);
    }

    @Override
    public ActionExecutionResult execute(HealthProposedAction action) {
        if (!checkRateLimit()) {
            log.warn("[HEALTH_EXECUTOR] Rate limit exceeded - action={}", action.getActionType());
            return ActionExecutionResult.failure(
                    "Rate limit atteint: max " + properties.maxMutationsPerHour()
                    + " executions par heure");
        }

        Map<String, Object> params = parseParams(action.getParamsJson());
        log.info("[HEALTH_EXECUTOR] Executing action - id={}, type={}, params={}",
                action.getId(), action.getActionType(), params);

        try {
            return switch (action.getActionType()) {
                case FLUSH_REDIS_CACHE -> flushRedisCache(params);
                case RETRY_DLQ_MESSAGES -> retryDlqMessages(params);
                case REINDEX_ELASTICSEARCH -> reindexElasticsearch();
                case TRIGGER_IMAGE_REPROCESSING -> triggerImageReprocessing();
                case RESTART_SERVICE -> restartService(params);
            };
        } catch (Exception e) {
            log.error("[HEALTH_EXECUTOR] Action failed - id={}, error={}",
                    action.getId(), e.getMessage(), e);
            return ActionExecutionResult.failure(e.getMessage());
        }
    }

    private boolean checkRateLimit() {
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        long executed = actionRepository.countByStatusAndApprovedAtAfter(
                ActionStatus.EXECUTED, since);
        return executed < properties.maxMutationsPerHour();
    }

    // ========== Action implementations ==========

    private ActionExecutionResult flushRedisCache(Map<String, Object> params) {
        Object pattern = params == null ? null : params.get("pattern");
        if (pattern == null || pattern.toString().isBlank()) {
            return ActionExecutionResult.failure(
                    "Parametre 'pattern' obligatoire (ex: 'cache:articles:*'). "
                    + "Le flush global de la base Redis est interdit.");
        }
        String patternStr = pattern.toString();
        if ("*".equals(patternStr)) {
            return ActionExecutionResult.failure(
                    "Pattern '*' interdit: viser un namespace specifique (ex: 'cache:articles:*').");
        }
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(patternStr)
                    .count(500)
                    .build();
            List<byte[]> matched = new ArrayList<>();
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    matched.add(cursor.next());
                }
            }
            if (matched.isEmpty()) {
                return ActionExecutionResult.ok("Aucune cle correspondante au pattern " + patternStr);
            }
            connection.keyCommands().del(matched.toArray(new byte[0][]));
            return ActionExecutionResult.ok("Supprime " + matched.size() + " cles du cache Redis");
        }
    }

    private ActionExecutionResult retryDlqMessages(Map<String, Object> params) {
        if (params == null || !params.containsKey("queue")) {
            return ActionExecutionResult.failure("Parametre 'queue' manquant");
        }
        String dlqName = params.get("queue").toString();
        int max = params.get("count") instanceof Number n ? n.intValue() : 10;

        String targetQueue = switch (dlqName) {
            case "portfolio.email.dlq" -> "portfolio.email.queue";
            case "portfolio.image.dlq" -> "portfolio.image.queue";
            case "portfolio.audit.dlq" -> "portfolio.audit.queue";
            case "portfolio.autoreply.dlq" -> "portfolio.autoreply.queue";
            default -> null;
        };
        if (targetQueue == null) {
            return ActionExecutionResult.failure("DLQ inconnue: " + dlqName);
        }

        // Move raw Message objects (not converted payloads) to preserve headers,
        // and republish each one before consuming the next so a crash mid-batch
        // never drops a message: at worst, the same message is delivered twice.
        int moved = 0;
        for (int i = 0; i < max; i++) {
            Message message = rabbitTemplate.receive(dlqName);
            if (message == null) {
                break;
            }
            try {
                rabbitTemplate.send(targetQueue, message);
                moved++;
            } catch (Exception e) {
                // Republish failed: put the message back on the DLQ so it is
                // not lost, then surface the failure to the caller.
                try {
                    rabbitTemplate.send(dlqName, message);
                } catch (Exception requeueError) {
                    log.error("[HEALTH_EXECUTOR] DLQ requeue failed - dlq={}, error={}",
                            dlqName, requeueError.getMessage(), requeueError);
                }
                return ActionExecutionResult.failure(
                        "Echec republication apres " + moved + " messages: " + e.getMessage());
            }
        }
        return ActionExecutionResult.ok("Rejoue " + moved + " messages depuis " + dlqName);
    }

    private ActionExecutionResult reindexElasticsearch() {
        int total = searchService.reindexAll();
        return ActionExecutionResult.ok("Reindexation Elasticsearch: " + total + " documents");
    }

    private ActionExecutionResult triggerImageReprocessing() {
        JobLauncher launcher = jobLauncherProvider.getIfAvailable();
        Job job = imageReprocessingJobProvider.getIfAvailable();
        if (launcher == null || job == null) {
            return ActionExecutionResult.failure("Batch image reprocessing indisponible");
        }
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("triggeredBy", "health-agent")
                    .toJobParameters();
            launcher.run(job, params);
            return ActionExecutionResult.ok("Job image reprocessing demarre");
        } catch (Exception e) {
            return ActionExecutionResult.failure("Echec du job: " + e.getMessage());
        }
    }

    private ActionExecutionResult restartService(Map<String, Object> params) {
        if (params == null || !params.containsKey("serviceName")) {
            return ActionExecutionResult.failure("Parametre 'serviceName' manquant");
        }
        String serviceName = params.get("serviceName").toString();
        if (!resolver.isAllowed(serviceName)) {
            return ActionExecutionResult.failure("Service non autorise: " + serviceName);
        }
        // Refuse to restart the socket proxy (would cut our own arm off)
        if ("docker-socket-proxy".equals(serviceName)) {
            return ActionExecutionResult.failure("Refus: ne peut pas redemarrer son propre proxy");
        }
        Optional<Container> container = resolver.resolve(serviceName);
        if (container.isEmpty()) {
            return ActionExecutionResult.failure("Container introuvable: " + serviceName);
        }
        dockerClient.restartContainerCmd(container.get().getId()).exec();
        return ActionExecutionResult.ok("Container " + serviceName + " redemarre");
    }

    private Map<String, Object> parseParams(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(json, PARAMS_TYPE);
            return parsed == null ? Map.of() : parsed;
        } catch (JsonProcessingException e) {
            log.warn("[HEALTH_EXECUTOR] Invalid params JSON - json={}, error={}",
                    json, e.getMessage());
            return Map.of();
        }
    }
}
