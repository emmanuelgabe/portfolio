package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.audit.AuditContext;
import com.emmanuelgabe.portfolio.dto.audit.AuditLogFilter;
import com.emmanuelgabe.portfolio.dto.audit.AuditLogResponse;
import com.emmanuelgabe.portfolio.dto.audit.AuditStatsResponse;
import com.emmanuelgabe.portfolio.entity.AuditLog;
import com.emmanuelgabe.portfolio.messaging.event.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for audit logging operations.
 */
public interface AuditService {

    /**
     * Capture current state of an entity for comparison.
     *
     * @param entityType the entity type
     * @param entityId the entity ID
     * @return the entity state as a map, or null if not found
     */
    Map<String, Object> captureEntityState(String entityType, Long entityId);

    /**
     * Convert an object to a map for storage.
     *
     * @param obj the object to convert
     * @return the object as a map
     */
    Map<String, Object> convertToMap(Object obj);

    /**
     * Create and persist an audit log entry.
     *
     * @param context the audit context
     * @param action the action type
     * @param entityType the entity type
     * @param entityId the entity ID
     * @param entityName the entity name for display
     * @param oldValues the old values (for UPDATE/DELETE)
     * @param newValues the new values (for CREATE/UPDATE)
     * @param success whether the operation succeeded
     * @param errorMessage error message if failed
     * @return the created audit log
     */
    AuditLog createAuditLog(AuditContext context, AuditAction action, String entityType,
                           Long entityId, String entityName, Map<String, Object> oldValues,
                           Map<String, Object> newValues, boolean success, String errorMessage);

    /**
     * Log authentication events (called directly, not via AOP).
     *
     * @param action the auth action (LOGIN, LOGOUT, etc.)
     * @param username the username
     * @param ipAddress client IP address
     * @param userAgent client user agent
     * @param success whether the auth succeeded
     * @param errorMessage error message if failed
     */
    void logAuthEvent(AuditAction action, String username, String ipAddress,
                      String userAgent, boolean success, String errorMessage);

    /**
     * Get paginated audit logs with filtering.
     *
     * @param filter the filter criteria
     * @param pageable pagination parameters
     * @return page of audit log responses
     */
    Page<AuditLogResponse> getAuditLogs(AuditLogFilter filter, Pageable pageable);

    /**
     * Get audit history for a specific entity.
     *
     * @param entityType the entity type
     * @param entityId the entity ID
     * @return list of audit log responses for the entity
     */
    List<AuditLogResponse> getEntityHistory(String entityType, Long entityId);

    /**
     * Get audit statistics for dashboard.
     *
     * @param since start date for stats calculation
     * @return statistics response
     */
    AuditStatsResponse getStats(LocalDateTime since);

    /**
     * Export audit logs to CSV format.
     *
     * @param filter the filter criteria
     * @return CSV content as byte array
     */
    byte[] exportToCsv(AuditLogFilter filter);

    /**
     * Export audit logs to JSON format.
     *
     * @param filter the filter criteria
     * @return JSON content as byte array
     */
    byte[] exportToJson(AuditLogFilter filter);

    /**
     * Persist an audit event received from RabbitMQ.
     * Converts the event to an AuditLog entity and saves to database.
     *
     * @param event the audit event to persist
     * @return the persisted audit log
     */
    AuditLog persistAuditEvent(AuditEvent event);
}
