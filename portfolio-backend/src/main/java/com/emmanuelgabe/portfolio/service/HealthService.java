package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CompleteHealthResponse;
import com.emmanuelgabe.portfolio.dto.DatabaseHealthResponse;
import com.emmanuelgabe.portfolio.dto.HealthResponse;

/**
 * Service interface for health check operations
 * Provides business logic for testing system health
 */
public interface HealthService {

    /**
     * Simple ping to test if the service is responding
     * @return HealthResponse with ok status
     */
    HealthResponse ping();

    /**
     * Check database connection health
     * @return DatabaseHealthResponse with connection status
     */
    DatabaseHealthResponse checkDatabase();

    /**
     * Get complete system health status
     * @return CompleteHealthResponse with all health checks
     */
    CompleteHealthResponse getCompleteStatus();

    /**
     * Check if the database connection is healthy (boolean)
     * @return true if database is reachable and valid
     */
    boolean isDatabaseHealthy();
}
