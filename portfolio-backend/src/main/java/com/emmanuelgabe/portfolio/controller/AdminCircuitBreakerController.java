package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CircuitBreakerStatusResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Admin controller for monitoring circuit breaker states.
 * Provides status information for dashboard monitoring cards.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/circuit-breakers")
@RequiredArgsConstructor
public class AdminCircuitBreakerController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Get status of all registered circuit breakers.
     *
     * @return list of circuit breaker statuses
     */
    @GetMapping
    public ResponseEntity<List<CircuitBreakerStatusResponse>> getAllCircuitBreakers() {
        log.debug("[GET_CIRCUIT_BREAKERS] Fetching all circuit breaker statuses");

        List<CircuitBreakerStatusResponse> statuses = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .map(this::toResponse)
                .toList();

        log.info("[GET_CIRCUIT_BREAKERS] Retrieved {} circuit breakers", statuses.size());
        return ResponseEntity.ok(statuses);
    }

    /**
     * Get status of a specific circuit breaker by name.
     *
     * @param name circuit breaker name
     * @return circuit breaker status
     */
    @GetMapping("/{name}")
    public ResponseEntity<CircuitBreakerStatusResponse> getCircuitBreaker(
            @PathVariable String name) {
        log.debug("[GET_CIRCUIT_BREAKER] Fetching status - name={}", name);

        return circuitBreakerRegistry.find(name)
                .map(cb -> ResponseEntity.ok(toResponse(cb)))
                .orElseGet(() -> {
                    log.warn("[GET_CIRCUIT_BREAKER] Circuit breaker not found - name={}", name);
                    return ResponseEntity.notFound().build();
                });
    }

    private CircuitBreakerStatusResponse toResponse(CircuitBreaker cb) {
        var metrics = cb.getMetrics();
        return new CircuitBreakerStatusResponse(
                cb.getName(),
                cb.getState().name(),
                new CircuitBreakerStatusResponse.CircuitBreakerMetrics(
                        metrics.getNumberOfFailedCalls(),
                        metrics.getNumberOfSuccessfulCalls(),
                        metrics.getNumberOfBufferedCalls(),
                        metrics.getFailureRate(),
                        metrics.getNumberOfNotPermittedCalls()
                ),
                Instant.now()
        );
    }
}
