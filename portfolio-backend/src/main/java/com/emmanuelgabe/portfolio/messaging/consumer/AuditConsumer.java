package com.emmanuelgabe.portfolio.messaging.consumer;

import com.emmanuelgabe.portfolio.entity.AuditLog;
import com.emmanuelgabe.portfolio.messaging.event.AuditEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumer for audit events from RabbitMQ.
 * Delegates persistence to AuditService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class AuditConsumer {

    private final AuditService auditService;
    private final BusinessMetrics metrics;

    /**
     * Handle incoming audit events and persist to database.
     *
     * @param event the audit event to process
     */
    @RabbitListener(queues = "${rabbitmq.queues.audit}")
    public void handleAuditEvent(AuditEvent event) {
        log.debug("[AUDIT_CONSUMER] Received audit event - eventId={}, action={}, entityType={}",
                event.getEventId(), event.getAction(), event.getEntityType());

        long startTime = System.currentTimeMillis();

        try {
            AuditLog auditLog = auditService.persistAuditEvent(event);

            long duration = System.currentTimeMillis() - startTime;
            metrics.recordAuditLogged();

            log.info("[AUDIT_CONSUMER] Audit log persisted - eventId={}, auditLogId={}, duration={}ms",
                    event.getEventId(), auditLog.getId(), duration);

        } catch (Exception e) {
            metrics.recordAuditLogFailure();
            log.error("[AUDIT_CONSUMER] Failed to persist audit log - eventId={}, action={}, error={}",
                    event.getEventId(), event.getAction(), e.getMessage(), e);
            throw e;
        }
    }
}
