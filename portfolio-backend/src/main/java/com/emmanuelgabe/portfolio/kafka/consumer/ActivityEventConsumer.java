package com.emmanuelgabe.portfolio.kafka.consumer;

import com.emmanuelgabe.portfolio.config.KafkaConfig;
import com.emmanuelgabe.portfolio.kafka.event.ActivityEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for activity events.
 * Processes session lifecycle and navigation events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class ActivityEventConsumer {

    private static final String TOPIC_NAME = "activity_events";

    private final BusinessMetrics metrics;

    /**
     * Process activity events from Kafka.
     *
     * @param event the activity event to process
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_ACTIVITY_EVENTS,
            groupId = "${kafka.consumer.group-id:portfolio-activity-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleActivityEvent(ActivityEvent event) {
        log.debug("[KAFKA_CONSUMER] Received activity event - eventId={}, type={}, sessionId={}",
                event.getEventId(), event.getActivityType(), event.getSessionId());

        try {
            processEvent(event);
            metrics.recordKafkaEventConsumed(TOPIC_NAME, event.getActivityType().name());
            log.debug("[KAFKA_CONSUMER] Activity event processed - eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("[KAFKA_CONSUMER] Failed to process activity event - eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            metrics.recordKafkaConsumeFailure(TOPIC_NAME, event.getActivityType().name());
            throw e;
        }
    }

    private void processEvent(ActivityEvent event) {
        switch (event.getActivityType()) {
            case SESSION_START -> log.info("[ACTIVITY] Session started - sessionId={}, ip={}",
                    event.getSessionId(), maskIp(event.getIpAddress()));
            case SESSION_END -> log.info("[ACTIVITY] Session ended - sessionId={}, durationMs={}",
                    event.getSessionId(), event.getDurationMs());
            case NAVIGATION -> log.debug("[ACTIVITY] Navigation - sessionId={}, path={}, from={}",
                    event.getSessionId(), event.getPath(), event.getPreviousPath());
            case IDLE_TIMEOUT -> log.info("[ACTIVITY] Session timed out - sessionId={}, durationMs={}",
                    event.getSessionId(), event.getDurationMs());
            default -> log.warn("[ACTIVITY] Unknown activity type - type={}",
                    event.getActivityType());
        }
    }

    /**
     * Mask IP address for privacy in logs.
     */
    private String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "unknown";
        }
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".***";
        }
        return "***";
    }
}
