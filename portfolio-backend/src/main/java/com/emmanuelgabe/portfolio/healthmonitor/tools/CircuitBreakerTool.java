package com.emmanuelgabe.portfolio.healthmonitor.tools;

import com.emmanuelgabe.portfolio.healthmonitor.HealthToolCallTracker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Green tool: exposes the current state of every Resilience4j circuit breaker
 * registered in the application. Open or half-open breakers are a strong
 * signal that a downstream service is failing.
 */
@Slf4j
@RequiredArgsConstructor
public class CircuitBreakerTool {

    private final CircuitBreakerRegistry registry;
    private final HealthToolCallTracker tracker;

    @Tool(description = "Return the current state of every Resilience4j circuit breaker: "
            + "name, state (CLOSED/OPEN/HALF_OPEN), failure rate, buffered calls, "
            + "and calls not permitted. An OPEN breaker indicates a downstream failure.")
    public List<Map<String, Object>> checkCircuitBreakers() {
        tracker.record("checkCircuitBreakers");
        log.debug("[HEALTH_TOOL] checkCircuitBreakers");
        return registry.getAllCircuitBreakers().stream()
                .map(this::describe)
                .toList();
    }

    private Map<String, Object> describe(CircuitBreaker cb) {
        CircuitBreaker.Metrics m = cb.getMetrics();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", cb.getName());
        out.put("state", cb.getState().name());
        out.put("failureRate", m.getFailureRate());
        out.put("bufferedCalls", m.getNumberOfBufferedCalls());
        out.put("failedCalls", m.getNumberOfFailedCalls());
        out.put("successCalls", m.getNumberOfSuccessfulCalls());
        out.put("notPermittedCalls", m.getNumberOfNotPermittedCalls());
        return out;
    }
}
