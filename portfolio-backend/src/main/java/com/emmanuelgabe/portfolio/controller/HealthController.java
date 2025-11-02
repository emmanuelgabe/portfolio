package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CompleteHealthResponse;
import com.emmanuelgabe.portfolio.dto.DatabaseHealthResponse;
import com.emmanuelgabe.portfolio.dto.HealthResponse;
import com.emmanuelgabe.portfolio.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
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
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private HealthService healthService;

    /**
     * Simple ping endpoint to test if API is reachable
     * Test: curl http://localhost:8080/api/health/ping
     */
    @GetMapping("/ping")
    public ResponseEntity<HealthResponse> ping() {
        HealthResponse response = healthService.ping();
        return ResponseEntity.ok(response);
    }

    /**
     * Database connection test
     * Test: curl http://localhost:8080/api/health/db
     */
    @GetMapping("/db")
    public ResponseEntity<DatabaseHealthResponse> checkDatabase() {
        DatabaseHealthResponse response = healthService.checkDatabase();

        if ("ok".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Complete health check
     * Test: curl http://localhost:8080/api/health/status
     */
    @GetMapping("/status")
    public ResponseEntity<CompleteHealthResponse> getStatus() {
        CompleteHealthResponse response = healthService.getCompleteStatus();

        if ("healthy".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }
}
