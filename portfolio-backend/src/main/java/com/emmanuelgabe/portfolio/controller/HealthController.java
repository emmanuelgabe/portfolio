package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CompleteHealthResponse;
import com.emmanuelgabe.portfolio.dto.DatabaseHealthResponse;
import com.emmanuelgabe.portfolio.dto.HealthResponse;
import com.emmanuelgabe.portfolio.service.HealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check controller for testing connections
 * Provides endpoints to verify:
 * - API is responding
 * - Database connection
 * - Overall application health
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    /**
     * Simple ping endpoint to test if API is reachable
     * Test: curl http://localhost:8080/api/health/ping
     */
    @GetMapping("/ping")
    public ResponseEntity<HealthResponse> ping() {
        log.debug("[HEALTH_PING] Request received");
        HealthResponse response = healthService.ping();
        log.debug("[HEALTH_PING] Success - status={}", response.getStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * Database connection test
     * Test: curl http://localhost:8080/api/health/db
     */
    @GetMapping("/db")
    public ResponseEntity<DatabaseHealthResponse> checkDatabase() {
        log.debug("[HEALTH_DB] Request received");
        DatabaseHealthResponse response = healthService.checkDatabase();

        if ("ok".equals(response.getStatus())) {
            log.debug("[HEALTH_DB] Success - status={}", response.getStatus());
            return ResponseEntity.ok(response);
        } else {
            log.warn("[HEALTH_DB] Database unhealthy - status={}, message={}", response.getStatus(), response.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Complete health check
     * Test: curl http://localhost:8080/api/health/status
     */
    @GetMapping("/status")
    public ResponseEntity<CompleteHealthResponse> getStatus() {
        log.debug("[HEALTH_STATUS] Request received");
        CompleteHealthResponse response = healthService.getCompleteStatus();

        if ("healthy".equals(response.getStatus())) {
            log.debug("[HEALTH_STATUS] Success - status={}", response.getStatus());
            return ResponseEntity.ok(response);
        } else {
            log.warn("[HEALTH_STATUS] System unhealthy - status={}", response.getStatus());
            return ResponseEntity.status(503).body(response);
        }
    }
}
