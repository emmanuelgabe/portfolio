package com.emmanuelgabe.portfolio.messaging.event;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.audit.AuditContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event for asynchronous audit logging via RabbitMQ.
 * Carries all audit data to be persisted by the consumer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String EVENT_TYPE = "AUDIT";

    private String eventId;
    private Instant createdAt;
    private AuditEventType auditType;

    // Action details
    private AuditAction action;
    private String entityType;
    private Long entityId;
    private String entityName;

    // Data changes
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private List<String> changedFields;

    // Result
    private boolean success;
    private String errorMessage;

    // Context (from AuditContext)
    private Long userId;
    private String username;
    private String userRole;
    private String ipAddress;
    private String userAgent;
    private String requestMethod;
    private String requestUri;
    private String requestId;

    /**
     * Audit event types.
     */
    public enum AuditEventType {
        ENTITY_AUDIT,
        AUTH_EVENT
    }

    /**
     * Create an entity audit event from context and audit data.
     */
    public static AuditEvent forEntityAudit(
            AuditContext context,
            AuditAction action,
            String entityType,
            Long entityId,
            String entityName,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            List<String> changedFields,
            boolean success,
            String errorMessage
    ) {
        AuditEventBuilder builder = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .auditType(AuditEventType.ENTITY_AUDIT)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .oldValues(oldValues)
                .newValues(newValues)
                .changedFields(changedFields)
                .success(success)
                .errorMessage(errorMessage);

        if (context != null) {
            builder.userId(context.getUserId())
                    .username(context.getUsername())
                    .userRole(context.getUserRole())
                    .ipAddress(context.getIpAddress())
                    .userAgent(context.getUserAgent())
                    .requestMethod(context.getRequestMethod())
                    .requestUri(context.getRequestUri())
                    .requestId(context.getRequestId());
        }

        return builder.build();
    }

    /**
     * Create an authentication event.
     */
    public static AuditEvent forAuthEvent(
            AuditAction action,
            String username,
            String ipAddress,
            String userAgent,
            boolean success,
            String errorMessage
    ) {
        return AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .auditType(AuditEventType.AUTH_EVENT)
                .action(action)
                .entityType("User")
                .username(username != null ? username : "unknown")
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(success)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Return the event type identifier.
     */
    public String getEventType() {
        return EVENT_TYPE;
    }
}
