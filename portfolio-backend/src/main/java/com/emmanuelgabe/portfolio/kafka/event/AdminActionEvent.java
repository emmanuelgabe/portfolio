package com.emmanuelgabe.portfolio.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Event representing an admin action for event sourcing.
 * Captures all CRUD operations and authentication events.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminActionEvent extends BaseEvent {

    public static final String EVENT_TYPE = "ADMIN_ACTION";

    private String action;
    private String entityType;
    private Long entityId;
    private String entityName;
    private String username;
    private String ipAddress;
    private Map<String, Object> payload;
    private boolean success;
    private String errorMessage;

    /**
     * Create a successful admin action event using builder pattern.
     */
    public static AdminActionEvent success(AdminActionParams params) {
        return createEvent(params, true, null);
    }

    /**
     * Create a successful admin action event.
     */
    public static AdminActionEvent success(String action, String entityType, Long entityId,
                                           String entityName, String username, String ipAddress,
                                           Map<String, Object> payload) {
        AdminActionParams params = new AdminActionParams(
                action, entityType, entityId, entityName, username, ipAddress, payload);
        return createEvent(params, true, null);
    }

    /**
     * Create a failed admin action event.
     */
    public static AdminActionEvent failure(String action, String entityType, Long entityId,
                                           String entityName, String username, String ipAddress,
                                           String errorMessage) {
        AdminActionParams params = new AdminActionParams(
                action, entityType, entityId, entityName, username, ipAddress, null);
        return createEvent(params, false, errorMessage);
    }

    private static AdminActionEvent createEvent(AdminActionParams params,
                                                boolean success, String errorMessage) {
        AdminActionEvent event = AdminActionEvent.builder()
                .action(params.action())
                .entityType(params.entityType())
                .entityId(params.entityId())
                .entityName(params.entityName())
                .username(params.username())
                .ipAddress(params.ipAddress())
                .payload(params.payload())
                .success(success)
                .errorMessage(errorMessage)
                .build();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(EVENT_TYPE);
        event.setTimestamp(Instant.now());
        event.setSource("portfolio-backend");
        return event;
    }

    /**
     * Parameters for creating an admin action event.
     */
    public record AdminActionParams(
            String action,
            String entityType,
            Long entityId,
            String entityName,
            String username,
            String ipAddress,
            Map<String, Object> payload
    ) { }
}
