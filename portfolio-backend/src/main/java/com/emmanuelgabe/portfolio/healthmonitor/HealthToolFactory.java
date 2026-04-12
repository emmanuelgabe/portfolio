package com.emmanuelgabe.portfolio.healthmonitor;

import com.emmanuelgabe.portfolio.healthmonitor.tools.AuditStatsTool;
import com.emmanuelgabe.portfolio.healthmonitor.tools.CircuitBreakerTool;
import com.emmanuelgabe.portfolio.healthmonitor.tools.DeadLetterQueueTool;
import com.emmanuelgabe.portfolio.healthmonitor.tools.DockerServiceResolver;
import com.emmanuelgabe.portfolio.healthmonitor.tools.RabbitQueueDepthTool;
import com.emmanuelgabe.portfolio.healthmonitor.tools.ServiceHealthTool;
import com.emmanuelgabe.portfolio.healthmonitor.tools.ServiceLogsTool;
import com.emmanuelgabe.portfolio.healthmonitor.tools.ServiceMetricsTool;
import com.emmanuelgabe.portfolio.service.AuditService;
import com.github.dockerjava.api.DockerClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Factory that creates the 7 green diagnostic tool instances for one LLM call.
 *
 * <p>Tools are not Spring beans themselves: they wrap Spring services but are
 * re-created per cycle with a request-scoped {@link HealthToolCallTracker}.
 * The factory itself is a singleton injected into {@code AiHealthAgentService}.</p>
 */
@Component
@ConditionalOnProperty(name = "ai.health-monitor.enabled", havingValue = "true")
public class HealthToolFactory {

    private final DockerClient dockerClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RabbitAdmin rabbitAdmin;
    private final AuditService auditService;
    private final HealthMonitorProperties properties;
    private final DockerServiceResolver resolver;

    public HealthToolFactory(DockerClient dockerClient,
                             CircuitBreakerRegistry circuitBreakerRegistry,
                             RabbitAdmin rabbitAdmin,
                             AuditService auditService,
                             HealthMonitorProperties properties) {
        this.dockerClient = dockerClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.rabbitAdmin = rabbitAdmin;
        this.auditService = auditService;
        this.properties = properties;
        this.resolver = new DockerServiceResolver(dockerClient);
    }

    public Object[] createTools(HealthToolCallTracker tracker) {
        return new Object[]{
            new ServiceHealthTool(dockerClient, resolver, tracker),
            new ServiceLogsTool(dockerClient, resolver, properties, tracker),
            new ServiceMetricsTool(dockerClient, resolver, tracker),
            new CircuitBreakerTool(circuitBreakerRegistry, tracker),
            new DeadLetterQueueTool(rabbitAdmin, tracker),
            new RabbitQueueDepthTool(rabbitAdmin, tracker),
            new AuditStatsTool(auditService, tracker)
        };
    }

    public DockerServiceResolver resolver() {
        return resolver;
    }
}
