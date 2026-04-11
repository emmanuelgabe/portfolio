package com.emmanuelgabe.portfolio.healthmonitor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Properties;

/**
 * Fast, non-LLM pre-check that determines whether a full diagnostic cycle
 * (with its Gemini cost) is actually warranted. Used by the scheduler to
 * skip the LLM call when the system looks healthy -- typical case in
 * production, where ~95% of cycles should be no-ops.
 *
 * <p>On-demand cycles from the admin bypass this gate so the agent always
 * runs when explicitly asked (useful for demos and testing).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.health-monitor.enabled", havingValue = "true")
public class HealthPreCheckService {

    private static final List<String> DLQ_NAMES = List.of(
            "portfolio.email.dlq",
            "portfolio.image.dlq",
            "portfolio.audit.dlq",
            "portfolio.autoreply.dlq"
    );

    private final ApplicationAvailability availability;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RabbitAdmin rabbitAdmin;
    private final HealthMonitorProperties properties;

    public record PreCheckResult(boolean anomalyDetected, String reason) { }

    /**
     * @return {@code anomalyDetected=true} if any cheap signal suggests a
     * problem that warrants a full LLM diagnostic cycle
     */
    public PreCheckResult run() {
        if (properties.demoMode()) {
            return new PreCheckResult(true, "demo-mode");
        }
        if (availability.getLivenessState() != LivenessState.CORRECT) {
            return new PreCheckResult(true, "liveness=BROKEN");
        }
        if (availability.getReadinessState() != ReadinessState.ACCEPTING_TRAFFIC) {
            return new PreCheckResult(true, "readiness=REFUSING_TRAFFIC");
        }

        for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
            if (cb.getState() == CircuitBreaker.State.OPEN
                    || cb.getState() == CircuitBreaker.State.HALF_OPEN) {
                return new PreCheckResult(true, "circuitBreaker=" + cb.getName()
                        + ":" + cb.getState());
            }
        }

        for (String dlq : DLQ_NAMES) {
            try {
                Properties info = rabbitAdmin.getQueueProperties(dlq);
                if (info == null) {
                    continue;
                }
                Object count = info.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
                if (count instanceof Number n && n.intValue() > 0) {
                    return new PreCheckResult(true, "dlq=" + dlq + ":" + n.intValue());
                }
            } catch (Exception e) {
                log.debug("[HEALTH_PRECHECK] DLQ probe failed - queue={}, error={}",
                        dlq, e.getMessage());
            }
        }

        return new PreCheckResult(false, "healthy");
    }
}
