package com.emmanuelgabe.portfolio.kafka.producer;

import com.emmanuelgabe.portfolio.kafka.event.ActivityEvent;
import com.emmanuelgabe.portfolio.kafka.event.AdminActionEvent;
import com.emmanuelgabe.portfolio.kafka.event.AnalyticsEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * No-op Kafka producer for when Kafka is disabled.
 * Logs events but does not publish them.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpKafkaEventProducer {

    /**
     * Log admin event without publishing.
     *
     * @param event the admin action event
     */
    public void publishAdminEvent(AdminActionEvent event) {
        log.debug("[KAFKA_NOOP] Admin event not published (Kafka disabled) - eventId={}, action={}",
                event.getEventId(), event.getAction());
    }

    /**
     * Log analytics event without publishing.
     *
     * @param event the analytics event
     */
    public void publishAnalyticsEvent(AnalyticsEvent event) {
        log.debug("[KAFKA_NOOP] Analytics event not published (Kafka disabled) - eventId={}, type={}",
                event.getEventId(), event.getAnalyticsType());
    }

    /**
     * Log activity event without publishing.
     *
     * @param event the activity event
     */
    public void publishActivityEvent(ActivityEvent event) {
        log.debug("[KAFKA_NOOP] Activity event not published (Kafka disabled) - eventId={}, type={}",
                event.getEventId(), event.getActivityType());
    }
}
