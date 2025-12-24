package com.emmanuelgabe.portfolio.messaging.consumer;

import com.emmanuelgabe.portfolio.messaging.event.ContactEmailEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.service.EmailSenderService;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumer for email events from RabbitMQ queue.
 * Processes email events asynchronously and sends emails via EmailSenderService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class EmailConsumer {

    private final EmailSenderService emailSenderService;
    private final BusinessMetrics metrics;

    /**
     * Handle contact email events from the queue.
     * Retry is handled by Spring AMQP configuration.
     *
     * @param event the contact email event to process
     */
    @RabbitListener(queues = "${rabbitmq.queues.email}")
    public void handleContactEmailEvent(ContactEmailEvent event) {
        log.info("[EMAIL_CONSUMER] Processing contact email - eventId={}, from={}, to={}",
                event.getEventId(), event.getSenderEmail(), event.getRecipientEmail());

        Timer.Sample sample = Timer.start();
        try {
            emailSenderService.sendHtmlEmail(
                    event.getSenderEmail(),
                    event.getRecipientEmail(),
                    "[Portfolio Contact] " + event.getSubject(),
                    event.getBody()
            );

            sample.stop(metrics.getEmailSendingTimer());
            metrics.recordContactSubmission();

            log.info("[EMAIL_CONSUMER] Contact email sent - eventId={}, recipient={}",
                    event.getEventId(), event.getRecipientEmail());

        } catch (Exception e) {
            metrics.recordContactFailure();
            log.error("[EMAIL_CONSUMER] Failed to send contact email - eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            throw e;
        }
    }
}
