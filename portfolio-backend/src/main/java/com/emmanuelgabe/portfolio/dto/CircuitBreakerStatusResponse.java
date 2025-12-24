package com.emmanuelgabe.portfolio.dto;

import java.time.Instant;

/**
 * Response DTO for circuit breaker status.
 * Used in admin dashboard monitoring card.
 */
public record CircuitBreakerStatusResponse(
        String name,
        String state,
        CircuitBreakerMetrics metrics,
        Instant timestamp
) {
    /**
     * Metrics for a circuit breaker instance.
     */
    public record CircuitBreakerMetrics(
            int failureCount,
            int successCount,
            int bufferedCalls,
            float failureRate,
            long notPermittedCalls
    ) { }
}
