package com.emmanuelgabe.portfolio.messaging.consumer;

import com.emmanuelgabe.portfolio.autoreply.AutoReplyService;
import com.emmanuelgabe.portfolio.messaging.event.AutoReplyEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.service.EmailSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Consumer for auto-reply events from RabbitMQ queue.
 * Generates RAG-powered replies and sends two emails:
 * one auto-reply to the sender, one enriched notification to the portfolio owner.
 *
 * <p>Follows the fail-open contract: any failure is caught and logged,
 * never rethrowing. The original contact email was already sent via the
 * separate email queue.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class AutoReplyConsumer {

    private final AutoReplyService autoReplyService;
    private final EmailSenderService emailSenderService;
    private final BusinessMetrics metrics;

    @Value("${spring.mail.username}")
    private String portfolioEmail;

    /**
     * Handle auto-reply events: generate AI reply and send emails.
     *
     * @param event the auto-reply event to process
     */
    @RabbitListener(queues = "${rabbitmq.queues.autoreply}")
    public void handleAutoReplyEvent(AutoReplyEvent event) {
        log.info("[AUTO_REPLY_CONSUMER] Processing auto-reply - eventId={}, sender={}",
                event.getEventId(), event.getSenderEmail());
        try {
            Optional<String> replyOpt = autoReplyService.generateReply(
                    event.getSenderName(),
                    event.getSenderEmail(),
                    event.getSubject(),
                    event.getMessage());

            if (replyOpt.isEmpty()) {
                log.info("[AUTO_REPLY_CONSUMER] No reply generated, skipping emails - eventId={}",
                        event.getEventId());
                return;
            }

            String reply = replyOpt.get();

            boolean senderEmailSent = trySendAutoReplyToSender(event, reply);
            trySendEnrichedNotificationToOwner(event, reply);

            if (senderEmailSent) {
                metrics.recordAutoReplyGenerated();
            }

            log.info("[AUTO_REPLY_CONSUMER] Auto-reply completed - eventId={}, senderEmailSent={}",
                    event.getEventId(), senderEmailSent);

        } catch (Exception e) {
            metrics.recordAutoReplyFailure();
            log.error("[AUTO_REPLY_CONSUMER] Auto-reply processing failed - eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
        }
    }

    private boolean trySendAutoReplyToSender(AutoReplyEvent event, String reply) {
        try {
            String htmlContent = buildAutoReplyHtml(event.getSenderName(), reply);
            emailSenderService.sendHtmlEmail(
                    portfolioEmail,
                    event.getSenderEmail(),
                    "Re: " + event.getSubject(),
                    htmlContent
            );
            log.info("[AUTO_REPLY_CONSUMER] Auto-reply sent to sender - eventId={}, to={}",
                    event.getEventId(), event.getSenderEmail());
            return true;
        } catch (Exception e) {
            log.error("[AUTO_REPLY_CONSUMER] Failed to send auto-reply to sender - eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            return false;
        }
    }

    private void trySendEnrichedNotificationToOwner(AutoReplyEvent event, String reply) {
        try {
            String htmlContent = buildEnrichedOwnerHtml(event, reply);
            emailSenderService.sendHtmlEmail(
                    portfolioEmail,
                    event.getRecipientEmail(),
                    "[Portfolio Contact + Auto-Reply] " + event.getSubject(),
                    htmlContent
            );
            log.info("[AUTO_REPLY_CONSUMER] Enriched notification sent to owner - eventId={}",
                    event.getEventId());
        } catch (Exception e) {
            log.error("[AUTO_REPLY_CONSUMER] Failed to send enriched notification to owner - eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
        }
    }

    private String buildAutoReplyHtml(String senderName, String reply) {
        String escapedReply = escapeHtml(reply).replace("\n", "<br>");

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #f4f4f4; padding: 20px; border-radius: 5px; \
                margin-bottom: 20px; }
                        .content { padding: 20px; background-color: #fff; border: 1px solid #ddd; \
                border-radius: 5px; }
                        .footer { margin-top: 20px; font-size: 12px; color: #777; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2 style="margin: 0; color: #007bff;">Portfolio - Emmanuel Gabe</h2>
                        </div>
                        <div class="content">
                            <p>%s</p>
                        </div>
                        <div class="footer">
                            <p>Ceci est une r\u00e9ponse automatique de l'assistant portfolio. \
                Emmanuel vous recontactera personnellement si n\u00e9cessaire.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(escapedReply);
    }

    private String buildEnrichedOwnerHtml(AutoReplyEvent event, String reply) {
        String escapedName = escapeHtml(event.getSenderName());
        String escapedEmail = escapeHtml(event.getSenderEmail());
        String escapedSubject = escapeHtml(event.getSubject());
        String escapedMessage = escapeHtml(event.getMessage()).replace("\n", "<br>");
        String escapedReply = escapeHtml(reply).replace("\n", "<br>");

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #f4f4f4; padding: 20px; border-radius: 5px; \
                margin-bottom: 20px; }
                        .section { padding: 20px; background-color: #fff; border: 1px solid #ddd; \
                border-radius: 5px; margin-bottom: 15px; }
                        .field { margin-bottom: 15px; }
                        .label { font-weight: bold; color: #555; }
                        .value { margin-top: 5px; padding: 10px; background-color: #f9f9f9; \
                border-left: 3px solid #007bff; }
                        .ai-reply { border-left-color: #28a745; }
                        .section-title { color: #007bff; margin-top: 0; }
                        .footer { margin-top: 20px; font-size: 12px; color: #777; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2 style="margin: 0; color: #007bff;">Contact + Auto-Reply</h2>
                        </div>
                        <div class="section">
                            <h3 class="section-title">Message original</h3>
                            <div class="field">
                                <div class="label">From:</div>
                                <div class="value">%s (%s)</div>
                            </div>
                            <div class="field">
                                <div class="label">Subject:</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="field">
                                <div class="label">Message:</div>
                                <div class="value">%s</div>
                            </div>
                        </div>
                        <div class="section">
                            <h3 class="section-title" style="color: #28a745;">R\u00e9ponse automatique envoy\u00e9e</h3>
                            <div class="field">
                                <div class="value ai-reply">%s</div>
                            </div>
                        </div>
                        <div class="footer">
                            <p>La r\u00e9ponse automatique ci-dessus a \u00e9t\u00e9 envoy\u00e9e \u00e0 %s.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                escapedName, escapedEmail, escapedSubject,
                escapedMessage, escapedReply, escapedEmail
        );
    }

    private static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
