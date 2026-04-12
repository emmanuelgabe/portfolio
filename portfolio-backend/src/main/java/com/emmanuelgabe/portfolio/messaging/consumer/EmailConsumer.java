package com.emmanuelgabe.portfolio.messaging.consumer;

import com.emmanuelgabe.portfolio.messaging.event.ContactEmailEvent;
import com.emmanuelgabe.portfolio.messaging.event.HealthAlertEmailEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.service.EmailSenderService;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumer for email events from RabbitMQ queue.
 *
 * <p>Uses class-level {@code @RabbitListener} + per-type {@code @RabbitHandler}
 * so Spring AMQP routes each payload class to the correct method based on the
 * {@code __TypeId__} header set by {@link org.springframework.amqp.support.converter.Jackson2JsonMessageConverter}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = "${rabbitmq.queues.email}")
public class EmailConsumer {

    private final EmailSenderService emailSenderService;
    private final BusinessMetrics metrics;

    @RabbitHandler
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

    @RabbitHandler
    public void handleHealthAlertEvent(HealthAlertEmailEvent event) {
        log.warn("[EMAIL_CONSUMER] Processing health alert - eventId={}, reportId={}, severity={}",
                event.getEventId(), event.getReportId(), event.getSeverity());

        try {
            emailSenderService.sendHtmlEmail(
                    event.getRecipientEmail(),
                    event.getRecipientEmail(),
                    event.getSubject(),
                    event.getBody()
            );
            log.info("[EMAIL_CONSUMER] Health alert sent - eventId={}, recipient={}",
                    event.getEventId(), event.getRecipientEmail());
        } catch (Exception e) {
            log.error("[EMAIL_CONSUMER] Failed to send health alert - eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            throw e;
        }
    }
}
