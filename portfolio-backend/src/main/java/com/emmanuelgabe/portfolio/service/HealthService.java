package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CompleteHealthResponse;
import com.emmanuelgabe.portfolio.dto.DatabaseHealthResponse;
import com.emmanuelgabe.portfolio.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Service layer for health check operations
 * Handles the business logic for testing system health
 */
@Service
public class HealthService {

    @Autowired
    private DataSource dataSource;

    /**
     * Simple ping to test if the service is responding
     * @return HealthResponse with ok status
     */
    public HealthResponse ping() {
        return new HealthResponse(
            "ok",
            "Backend API is responding",
            System.currentTimeMillis()
        );
    }

    /**
     * Check database connection health
     * @return DatabaseHealthResponse with connection status
     */
    public DatabaseHealthResponse checkDatabase() {
        DatabaseHealthResponse response = new DatabaseHealthResponse();

        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5); // 5 second timeout

            if (isValid) {
                response.setStatus("ok");
                response.setMessage("Database connection is healthy");
                response.setDatabase(connection.getMetaData().getDatabaseProductName());
                response.setUrl(connection.getMetaData().getURL());
            } else {
                response.setStatus("error");
                response.setMessage("Database connection is not valid");
            }
        } catch (Exception e) {
            response.setStatus("error");
            response.setMessage("Database connection failed");
            response.setError(e.getMessage());
        }

        return response;
    }

    /**
     * Get complete system health status
     * @return CompleteHealthResponse with all health checks
     */
    public CompleteHealthResponse getCompleteStatus() {
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

        return new CompleteHealthResponse(
            overallStatus,
            checks,
            System.currentTimeMillis()
        );
    }

    /**
     * Check if the database connection is healthy (boolean)
     * @return true if database is reachable and valid
     */
    public boolean isDatabaseHealthy() {
        DatabaseHealthResponse response = checkDatabase();
        return "ok".equals(response.getStatus());
    }
}
