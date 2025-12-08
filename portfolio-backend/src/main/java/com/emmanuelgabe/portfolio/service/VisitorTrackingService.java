package com.emmanuelgabe.portfolio.service;

import java.time.LocalDate;

/**
 * Service for tracking active visitors on the public site.
 * Uses Redis to store visitor sessions with automatic TTL expiration.
 */
public interface VisitorTrackingService {

    /**
     * Register a heartbeat for a visitor session.
     * Updates the TTL for the session in Redis and tracks daily unique visitors.
     *
     * @param sessionId the unique session identifier
     */
    void registerHeartbeat(String sessionId);

    /**
     * Get the count of currently active visitors.
     *
     * @return the number of active visitor sessions
     */
    int getActiveVisitorsCount();

    /**
     * Get the count of unique visitors for a specific date.
     *
     * @param date the date to get unique visitors for
     * @return the number of unique visitors for that date
     */
    long getDailyUniqueVisitorsCount(LocalDate date);

    /**
     * Clear the daily unique visitors set for a specific date.
     * Called by the batch job after aggregating stats.
     *
     * @param date the date to clear
     */
    void clearDailyUniqueVisitors(LocalDate date);
}
