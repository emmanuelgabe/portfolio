package com.emmanuelgabe.portfolio.messaging.publisher;

import com.emmanuelgabe.portfolio.messaging.config.RabbitMQProperties;
import com.emmanuelgabe.portfolio.messaging.event.AuditEvent;
import com.emmanuelgabe.portfolio.messaging.event.EmailEvent;
import com.emmanuelgabe.portfolio.messaging.event.ImageProcessingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ implementation of EventPublisher.
 * Publishes events to RabbitMQ for asynchronous processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties properties;

    @Override
    public void publishEmailEvent(EmailEvent event) {
        log.debug("[RABBITMQ] Publishing email event - eventId={}, eventType={}, recipient={}",
                event.getEventId(), event.getEventType(), event.getRecipientEmail());

        try {
            rabbitTemplate.convertAndSend(
                    properties.getExchanges().getEmail(),
                    properties.getRoutingKeys().getEmail(),
                    event
            );
            log.info("[RABBITMQ] Email event published - eventId={}, eventType={}",
                    event.getEventId(), event.getEventType());
        } catch (AmqpException e) {
            log.error("[RABBITMQ] Failed to publish email event - eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void publishImageEvent(ImageProcessingEvent event) {
        log.debug("[RABBITMQ] Publishing image event - eventId={}, type={}, tempFile={}",
                event.getEventId(), event.getProcessingType(), event.getTempFilePath());

        try {
            rabbitTemplate.convertAndSend(
                    properties.getExchanges().getImage(),
                    properties.getRoutingKeys().getImage(),
                    event
            );
            log.info("[RABBITMQ] Image event published - eventId={}, type={}",
                    event.getEventId(), event.getProcessingType());
        } catch (AmqpException e) {
            log.error("[RABBITMQ] Failed to publish image event - eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void publishAuditEvent(AuditEvent event) {
        log.debug("[RABBITMQ] Publishing audit event - eventId={}, action={}, entityType={}",
                event.getEventId(), event.getAction(), event.getEntityType());

        try {
            rabbitTemplate.convertAndSend(
                    properties.getExchanges().getAudit(),
                    properties.getRoutingKeys().getAudit(),
                    event
            );
            log.debug("[RABBITMQ] Audit event published - eventId={}, action={}",
                    event.getEventId(), event.getAction());
        } catch (AmqpException e) {
            log.error("[RABBITMQ] Failed to publish audit event - eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }
}
