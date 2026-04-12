package com.emmanuelgabe.portfolio.healthmonitor;

import com.emmanuelgabe.portfolio.entity.HealthProposedAction;
import com.emmanuelgabe.portfolio.entity.HealthReport;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionLevel;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionStatus;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionType;
import com.emmanuelgabe.portfolio.healthmonitor.action.HealthCycleType;
import com.emmanuelgabe.portfolio.healthmonitor.action.HealthReportDraft;
import com.emmanuelgabe.portfolio.healthmonitor.action.ProposedActionDraft;
import com.emmanuelgabe.portfolio.healthmonitor.action.ReportSeverity;
import com.emmanuelgabe.portfolio.repository.HealthProposedActionRepository;
import com.emmanuelgabe.portfolio.repository.HealthReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * LLM-backed health diagnostic agent. Runs one {@code analyze()} call as a
 * stateless Spring AI function-calling session:
 * <ol>
 *   <li>Builds the system prompt (with inter-cycle memory injection)</li>
 *   <li>Lets Gemini invoke any of the 7 green tools it wants</li>
 *   <li>Receives a structured {@link HealthReportDraft} via Spring AI's
 *       {@code .entity()} mapping</li>
 *   <li>Validates the draft, persists the report and proposals, updates metrics</li>
 * </ol>
 *
 * <p>Wrapped in Resilience4j {@code @CircuitBreaker} and {@code @Retry} so
 * transient Gemini failures don't crash the scheduler. On total failure the
 * service persists a report with severity {@code UNKNOWN} instead of throwing.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class AiHealthAgentService implements HealthAgentService {

    private static final String CB_NAME = "aiHealthAgent";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ChatClient chatClient;
    private final HealthToolFactory toolFactory;
    private final HealthPromptBuilder promptBuilder;
    private final HealthReportRepository reportRepository;
    private final HealthProposedActionRepository actionRepository;
    private final HealthMonitorProperties properties;
    private final HealthMonitorMetrics metrics;
    private final HealthAlertService alertService;

    @Override
    @Transactional
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "analyzeFallback")
    @Retry(name = CB_NAME)
    public HealthReport analyze(HealthCycleType cycleType) {
        Instant start = Instant.now();
        HealthToolCallTracker tracker = new HealthToolCallTracker();
        log.info("[HEALTH_AGENT] Starting diagnostic cycle - cycleType={}", cycleType);

        HealthReportDraft draft = chatClient.prompt()
                .system(promptBuilder.buildSystemPrompt())
                .user(promptBuilder.buildUserMessage())
                .tools(toolFactory.createTools(tracker))
                .call()
                .entity(HealthReportDraft.class);

        if (draft == null) {
            log.warn("[HEALTH_AGENT] LLM returned null draft - cycleType={}", cycleType);
            return persistFallback(cycleType, tracker, start, "draft_null");
        }

        HealthReport report = persistReport(cycleType, draft, tracker, start);
        persistProposals(report, draft.proposals());
        if (alertService != null) {
            alertService.maybeAlert(report);
        }

        metrics.recordCycleSuccess();
        metrics.recordCycleDuration(Duration.between(start, Instant.now()));
        log.info("[HEALTH_AGENT] Cycle completed - reportId={}, severity={}, proposals={}",
                report.getId(), report.getSeverity(),
                draft.proposals() == null ? 0 : draft.proposals().size());
        return report;
    }

    /**
     * Resilience4j fallback: persist an UNKNOWN report without throwing so
     * the scheduler keeps running across transient Gemini outages.
     */
    @SuppressWarnings("unused")
    private HealthReport analyzeFallback(HealthCycleType cycleType, Throwable error) {
        log.error("[HEALTH_AGENT] Fallback triggered - cycleType={}, error={}",
                cycleType, error == null ? "unknown" : error.getMessage());
        metrics.recordCycleFailure();
        HealthReport report = new HealthReport();
        report.setCycleType(cycleType);
        report.setSeverity(ReportSeverity.UNKNOWN);
        report.setSummary("Echec de l'analyse agentic: " + (error == null ? "?" : error.getMessage()));
        report.setAnomaliesJson("[]");
        report.setToolCallsJson("[]");
        report.setDurationMs(0L);
        return reportRepository.save(report);
    }

    private HealthReport persistFallback(HealthCycleType cycleType, HealthToolCallTracker tracker,
                                         Instant start, String reason) {
        HealthReport report = new HealthReport();
        report.setCycleType(cycleType);
        report.setSeverity(ReportSeverity.UNKNOWN);
        report.setSummary("Analyse incomplete: " + reason);
        report.setAnomaliesJson("[]");
        report.setToolCallsJson(toJson(tracker.getCalls()));
        report.setDurationMs(Duration.between(start, Instant.now()).toMillis());
        return reportRepository.save(report);
    }

    private HealthReport persistReport(HealthCycleType cycleType, HealthReportDraft draft,
                                       HealthToolCallTracker tracker, Instant start) {
        HealthReport report = new HealthReport();
        report.setCycleType(cycleType);
        report.setSeverity(draft.severity() == null ? ReportSeverity.UNKNOWN : draft.severity());
        report.setSummary(draft.summary() == null ? "(no summary)" : draft.summary());
        report.setAnomaliesJson(toJson(draft.anomalies() == null
                ? Collections.emptyList() : draft.anomalies()));
        report.setToolCallsJson(toJson(tracker.getCalls()));
        report.setDurationMs(Duration.between(start, Instant.now()).toMillis());
        return reportRepository.save(report);
    }

    private void persistProposals(HealthReport report, List<ProposedActionDraft> proposals) {
        if (proposals == null || proposals.isEmpty()) {
            return;
        }
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusMinutes(properties.actionExpirationMinutes());

        for (ProposedActionDraft draft : proposals) {
            if (draft.type() == null || draft.level() == null) {
                log.warn("[HEALTH_AGENT] Skipping invalid proposal - missing type or level");
                continue;
            }
            // Defense-in-depth against prompt injection: a mismatch between
            // the claimed level and the server-declared level for the action
            // type is treated as a sign the agent response is untrustworthy,
            // so we reject the proposal instead of silently overriding.
            ActionLevel expectedLevel = draft.type().getLevel();
            if (draft.level() != expectedLevel) {
                log.warn("[HEALTH_AGENT] Rejecting proposal - level mismatch, "
                        + "type={}, claimed={}, expected={}",
                        draft.type(), draft.level(), expectedLevel);
                metrics.recordProposalRejectedLevelMismatch();
                continue;
            }
            if (expectedLevel == ActionLevel.GREEN) {
                // GREEN is never persisted as an action.
                continue;
            }

            HealthProposedAction action = new HealthProposedAction();
            action.setReportId(report.getId());
            action.setLevel(expectedLevel);
            action.setActionType(draft.type());
            action.setParamsJson(toJson(draft.params()));
            action.setJustification(draft.justification() == null ? "" : draft.justification());
            action.setStatus(ActionStatus.PENDING);
            action.setExpiresAt(expiresAt);
            actionRepository.save(action);

            metrics.recordProposedAction(action.getLevel(), action.getActionType());
        }
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("[HEALTH_AGENT] JSON serialization failed - error={}", e.getMessage());
            return "null";
        }
    }

    // Keep ActionType import alive when no proposal is persisted
    @SuppressWarnings("unused")
    private static final ActionType[] ACTION_TYPES_MARKER = ActionType.values();
}
