package com.emmanuelgabe.portfolio.messaging.consumer;

import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Consumer for dead letter queues.
 * Monitors and logs failed messages for alerting and debugging.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class DeadLetterConsumer {

    private final BusinessMetrics metrics;

    /**
     * Handle messages that failed email processing.
     */
    @RabbitListener(queues = "${rabbitmq.queues.email-dlq}")
    public void handleEmailDeadLetter(Message message) {
        logDeadLetterMessage("email", message);
        metrics.recordDeadLetterMessage("email");
    }

    /**
     * Handle messages that failed image processing.
     */
    @RabbitListener(queues = "${rabbitmq.queues.image-dlq}")
    public void handleImageDeadLetter(Message message) {
        logDeadLetterMessage("image", message);
        metrics.recordDeadLetterMessage("image");
    }

    /**
     * Handle messages that failed audit processing.
     */
    @RabbitListener(queues = "${rabbitmq.queues.audit-dlq}")
    public void handleAuditDeadLetter(Message message) {
        logDeadLetterMessage("audit", message);
        metrics.recordDeadLetterMessage("audit");
    }

    /**
     * Log dead letter message details for debugging.
     */
    private void logDeadLetterMessage(String queueType, Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");

        log.error("[DLQ] Message in {} dead letter queue - headers={}, xDeath={}, body={}",
                queueType,
                message.getMessageProperties().getHeaders(),
                xDeath,
                body.length() > 500 ? body.substring(0, 500) + "..." : body);
    }
}
