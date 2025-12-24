package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.ContactResponse;
import com.emmanuelgabe.portfolio.messaging.event.ContactEmailEvent;
import com.emmanuelgabe.portfolio.messaging.publisher.EventPublisher;
import com.emmanuelgabe.portfolio.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation of ContactService.
 * Handles contact form submissions by publishing events for async email processing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final EventPublisher eventPublisher;

    @Value("${spring.mail.username}")
    private String recipientEmail;

    @Override
    public ContactResponse sendContactEmail(ContactRequest request) {
        log.info("[CONTACT] Processing contact form - from={}, subject={}",
                request.getEmail(), request.getSubject());

        String htmlContent = buildEmailContent(request);

        ContactEmailEvent event = ContactEmailEvent.of(
                recipientEmail,
                request.getName(),
                request.getEmail(),
                request.getSubject(),
                request.getMessage(),
                htmlContent
        );

        eventPublisher.publishEmailEvent(event);

        log.info("[CONTACT] Contact email queued - eventId={}, from={}",
                event.getEventId(), request.getEmail());

        return ContactResponse.success("Message sent successfully");
    }

    private String buildEmailContent(ContactRequest request) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #f4f4f4; padding: 20px; border-radius: 5px; margin-bottom: 20px; }
                        .content { padding: 20px; background-color: #fff; border: 1px solid #ddd; border-radius: 5px; }
                        .field { margin-bottom: 15px; }
                        .label { font-weight: bold; color: #555; }
                        .value { margin-top: 5px; padding: 10px; background-color: #f9f9f9; border-left: 3px solid #007bff; }
                        .footer { margin-top: 20px; font-size: 12px; color: #777; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2 style="margin: 0; color: #007bff;">New Contact Form Submission</h2>
                        </div>
                        <div class="content">
                            <div class="field">
                                <div class="label">From:</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="field">
                                <div class="label">Email:</div>
                                <div class="value">%s</div>
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
                        <div class="footer">
                            <p>This email was sent from your portfolio contact form.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(request.getName()),
                escapeHtml(request.getEmail()),
                escapeHtml(request.getSubject()),
                escapeHtml(request.getMessage()).replace("\n", "<br>")
        );
    }

    /**
     * Escape HTML special characters to prevent XSS in emails.
     */
    private String escapeHtml(String input) {
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
