package com.emmanuelgabe.portfolio.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Event representing an auto-reply to be generated and sent asynchronously.
 * Carries all contact data needed for RAG-powered reply generation.
 * Consumed by {@code AutoReplyConsumer}, not by the email queue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoReplyEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String EVENT_TYPE = "AUTO_REPLY";

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant createdAt = Instant.now();

    private String senderName;
    private String senderEmail;
    private String subject;
    private String message;
    private String recipientEmail;

    /**
     * Create an AutoReplyEvent from contact form data.
     *
     * @param senderName     the name of the person submitting the form
     * @param senderEmail    the email of the person submitting the form
     * @param subject        the subject of the contact message
     * @param message        the raw message content
     * @param recipientEmail the portfolio owner email (for enriched notification)
     * @return a new AutoReplyEvent
     */
    public static AutoReplyEvent of(
            String senderName,
            String senderEmail,
            String subject,
            String message,
            String recipientEmail
    ) {
        return AutoReplyEvent.builder()
                .senderName(senderName)
                .senderEmail(senderEmail)
                .subject(subject)
                .message(message)
                .recipientEmail(recipientEmail)
                .build();
    }
}
