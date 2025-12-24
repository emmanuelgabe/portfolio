package com.emmanuelgabe.portfolio.messaging.publisher;

import com.emmanuelgabe.portfolio.messaging.event.AuditEvent;
import com.emmanuelgabe.portfolio.messaging.event.EmailEvent;
import com.emmanuelgabe.portfolio.messaging.event.ImageProcessingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-operation EventPublisher used when RabbitMQ is disabled.
 * Logs a warning instead of publishing events.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "false")
public class NoOpEventPublisher implements EventPublisher {

    @Override
    public void publishEmailEvent(EmailEvent event) {
        log.warn("[RABBITMQ] RabbitMQ is disabled - email event not published - eventId={}, eventType={}",
                event.getEventId(), event.getEventType());
    }

    @Override
    public void publishImageEvent(ImageProcessingEvent event) {
        log.warn("[RABBITMQ] RabbitMQ is disabled - image event not published - eventId={}, type={}",
                event.getEventId(), event.getProcessingType());
    }

    @Override
    public void publishAuditEvent(AuditEvent event) {
        log.warn("[RABBITMQ] RabbitMQ is disabled - audit event not published - eventId={}, action={}",
                event.getEventId(), event.getAction());
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
