package com.emmanuelgabe.portfolio.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Event representing user activity for session tracking.
 * Captures session lifecycle and navigation events.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActivityEvent extends BaseEvent {

    public static final String EVENT_TYPE = "ACTIVITY";

    /**
     * Type of activity event.
     */
    public enum ActivityType {
        SESSION_START,
        SESSION_END,
        NAVIGATION,
        IDLE_TIMEOUT
    }

    private ActivityType activityType;
    private String sessionId;
    private String path;
    private String previousPath;
    private String ipAddress;
    private String userAgent;
    private Long durationMs;

    /**
     * Create a session start event.
     */
    public static ActivityEvent sessionStart(String sessionId, String ipAddress, String userAgent) {
        ActivityEvent event = ActivityEvent.builder()
                .activityType(ActivityType.SESSION_START)
                .sessionId(sessionId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        event.initializeFields();
        return event;
    }

    /**
     * Create a session end event.
     */
    public static ActivityEvent sessionEnd(String sessionId, Long durationMs) {
        ActivityEvent event = ActivityEvent.builder()
                .activityType(ActivityType.SESSION_END)
                .sessionId(sessionId)
                .durationMs(durationMs)
                .build();
        event.initializeFields();
        return event;
    }

    /**
     * Create a navigation event.
     */
    public static ActivityEvent navigation(String sessionId, String path, String previousPath) {
        ActivityEvent event = ActivityEvent.builder()
                .activityType(ActivityType.NAVIGATION)
                .sessionId(sessionId)
                .path(path)
                .previousPath(previousPath)
                .build();
        event.initializeFields();
        return event;
    }

    /**
     * Create an idle timeout event.
     */
    public static ActivityEvent idleTimeout(String sessionId, Long durationMs) {
        ActivityEvent event = ActivityEvent.builder()
                .activityType(ActivityType.IDLE_TIMEOUT)
                .sessionId(sessionId)
                .durationMs(durationMs)
                .build();
        event.initializeFields();
        return event;
    }

    private void initializeFields() {
        setEventId(UUID.randomUUID().toString());
        setEventType(EVENT_TYPE);
        setTimestamp(Instant.now());
        setSource("portfolio-backend");
    }
}
