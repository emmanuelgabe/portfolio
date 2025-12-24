package com.emmanuelgabe.portfolio.kafka.producer;

import com.emmanuelgabe.portfolio.config.KafkaConfig;
import com.emmanuelgabe.portfolio.kafka.event.ActivityEvent;
import com.emmanuelgabe.portfolio.kafka.event.AdminActionEvent;
import com.emmanuelgabe.portfolio.kafka.event.AnalyticsEvent;
import com.emmanuelgabe.portfolio.kafka.event.BaseEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka event producer for publishing events to topics.
 * Handles admin actions, analytics events, and activity events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BusinessMetrics metrics;

    /**
     * Publish an admin action event.
     *
     * @param event the admin action event to publish
     */
    public void publishAdminEvent(AdminActionEvent event) {
        publishEvent(KafkaConfig.TOPIC_ADMIN_EVENTS, event.getEntityType(), event, event.getAction());
    }

    /**
     * Publish an analytics event.
     *
     * @param event the analytics event to publish
     */
    public void publishAnalyticsEvent(AnalyticsEvent event) {
        String key = event.getSessionId() != null ? event.getSessionId() : event.getEventId();
        publishEvent(KafkaConfig.TOPIC_ANALYTICS_EVENTS, key, event, event.getAnalyticsType().name());
    }

    /**
     * Publish an activity event.
     *
     * @param event the activity event to publish
     */
    public void publishActivityEvent(ActivityEvent event) {
        String key = event.getSessionId() != null ? event.getSessionId() : event.getEventId();
        publishEvent(KafkaConfig.TOPIC_ACTIVITY_EVENTS, key, event, event.getActivityType().name());
    }

    /**
     * Publish an event to a specific topic.
     *
     * @param topic the topic to publish to
     * @param key the message key for partitioning
     * @param event the event to publish
     * @param eventType the event type for metrics
     */
    private void publishEvent(String topic, String key, BaseEvent event, String eventType) {
        log.debug("[KAFKA_PRODUCER] Publishing event - topic={}, key={}, eventType={}, eventId={}",
                topic, key, event.getEventType(), event.getEventId());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[KAFKA_PRODUCER] Failed to publish event - topic={}, eventId={}, error={}",
                        topic, event.getEventId(), ex.getMessage(), ex);
                metrics.recordKafkaPublishFailure(extractTopicName(topic), eventType);
            } else {
                log.debug("[KAFKA_PRODUCER] Event published successfully - topic={}, partition={}, "
                                + "offset={}, eventId={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getEventId());
                metrics.recordKafkaEventPublished(extractTopicName(topic), eventType);
            }
        });
    }

    /**
     * Extract simple topic name for metrics.
     */
    private String extractTopicName(String topic) {
        int lastDot = topic.lastIndexOf('.');
        return lastDot > 0 ? topic.substring(lastDot + 1) : topic;
    }
}
