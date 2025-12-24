package com.emmanuelgabe.portfolio.kafka.consumer;

import com.emmanuelgabe.portfolio.config.KafkaConfig;
import com.emmanuelgabe.portfolio.kafka.event.AdminActionEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for admin action events.
 * Processes events for event sourcing and audit trail.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class AdminEventConsumer {

    private static final String TOPIC_NAME = "admin_events";

    private final BusinessMetrics metrics;

    /**
     * Process admin action events from Kafka.
     * Events are stored for event sourcing and can be replayed for state reconstruction.
     *
     * @param event the admin action event to process
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_ADMIN_EVENTS,
            groupId = "${kafka.consumer.group-id:portfolio-admin-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAdminEvent(AdminActionEvent event) {
        log.info("[KAFKA_CONSUMER] Received admin event - eventId={}, action={}, entityType={}, "
                        + "entityId={}, username={}, success={}",
                event.getEventId(),
                event.getAction(),
                event.getEntityType(),
                event.getEntityId(),
                event.getUsername(),
                event.isSuccess());

        try {
            processEvent(event);
            metrics.recordKafkaEventConsumed(TOPIC_NAME, event.getAction());
        } catch (Exception e) {
            log.error("[KAFKA_CONSUMER] Failed to process admin event - eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            metrics.recordKafkaConsumeFailure(TOPIC_NAME, event.getAction());
            throw e;
        }
    }

    private void processEvent(AdminActionEvent event) {
        if (!event.isSuccess()) {
            log.warn("[EVENT_SOURCING] Failed action recorded - action={}, entityType={}, "
                            + "entityId={}, error={}",
                    event.getAction(),
                    event.getEntityType(),
                    event.getEntityId(),
                    event.getErrorMessage());
            return;
        }

        log.info("[EVENT_SOURCING] Action recorded - action={}, entityType={}, entityId={}, "
                        + "entityName={}",
                event.getAction(),
                event.getEntityType(),
                event.getEntityId(),
                event.getEntityName());
    }
}
