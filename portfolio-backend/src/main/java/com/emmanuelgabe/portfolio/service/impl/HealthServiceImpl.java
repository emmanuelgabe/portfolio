package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.dto.CompleteHealthResponse;
import com.emmanuelgabe.portfolio.dto.DatabaseHealthResponse;
import com.emmanuelgabe.portfolio.dto.HealthResponse;
import com.emmanuelgabe.portfolio.service.HealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of HealthService interface
 * Handles business logic for testing system health
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final DataSource dataSource;

    @Override
    public HealthResponse ping() {
        log.debug("[HEALTH_PING] Processing ping request");
        return new HealthResponse(
            "ok",
            "Backend API is responding",
            System.currentTimeMillis()
        );
    }

    @Override
    public DatabaseHealthResponse checkDatabase() {
        log.debug("[HEALTH_DB] Checking database connection");
        DatabaseHealthResponse response = new DatabaseHealthResponse();

        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5); // 5 second timeout

            if (isValid) {
                response.setStatus("ok");
                response.setMessage("Database connection is healthy");
                response.setDatabase(connection.getMetaData().getDatabaseProductName());
                log.debug("[HEALTH_DB] Database healthy - database={}", response.getDatabase());
            } else {
                response.setStatus("error");
                response.setMessage("Database connection is not valid");
                log.warn("[HEALTH_DB] Database connection not valid");
            }
        } catch (Exception e) {
            response.setStatus("error");
            response.setMessage("Database connection failed");
            response.setError(e.getMessage());
            log.error("[HEALTH_DB] Database connection failed - error={}", e.getMessage(), e);
        }

        return response;
    }

    @Override
    public CompleteHealthResponse getCompleteStatus() {
        log.debug("[HEALTH_STATUS] Performing complete health check");
        Map<String, Object> checks = new HashMap<>();

        // Check API
        checks.put("api", Map.of("status", "ok", "message", "API is responding"));

        // Check Database
        DatabaseHealthResponse dbHealth = checkDatabase();
        Map<String, Object> dbCheck = new HashMap<>();
        dbCheck.put("status", dbHealth.getStatus());
        dbCheck.put("message", dbHealth.getMessage());

        if (dbHealth.getDatabase() != null) {
            dbCheck.put("type", dbHealth.getDatabase());
        }
        if (dbHealth.getError() != null) {
            dbCheck.put("error", dbHealth.getError());
        }

        checks.put("database", dbCheck);

        // Overall status
        boolean allHealthy = checks.values().stream()
            .allMatch(check -> {
                if (check instanceof Map) {
                    return "ok".equals(((Map<?, ?>) check).get("status"));
                }
                return false;
            });

        String overallStatus = allHealthy ? "healthy" : "unhealthy";
        log.debug("[HEALTH_STATUS] Health check complete - status={}", overallStatus);

        return new CompleteHealthResponse(
            overallStatus,
            checks,
            System.currentTimeMillis()
        );
    }

    @Override
    public boolean isDatabaseHealthy() {
        DatabaseHealthResponse response = checkDatabase();
        return "ok".equals(response.getStatus());
    }
}
