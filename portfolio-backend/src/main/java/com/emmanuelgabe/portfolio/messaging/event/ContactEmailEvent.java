package com.emmanuelgabe.portfolio.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.time.Instant;
import java.util.UUID;

/**
 * Event representing a contact form email to be sent asynchronously.
 * Contains all information needed to construct and send the email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactEmailEvent implements EmailEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String EVENT_TYPE = "CONTACT_EMAIL";

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant createdAt = Instant.now();

    private String recipientEmail;
    private String senderName;
    private String senderEmail;
    private String subject;
    private String message;
    private String body;

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    /**
     * Create a ContactEmailEvent from contact form data.
     *
     * @param recipientEmail the admin email to receive the contact
     * @param senderName     the name of the person submitting the form
     * @param senderEmail    the email of the person submitting the form
     * @param subject        the subject of the contact message
     * @param message        the raw message content
     * @param htmlBody       the formatted HTML body
     * @return a new ContactEmailEvent
     */
    public static ContactEmailEvent of(
            String recipientEmail,
            String senderName,
            String senderEmail,
            String subject,
            String message,
            String htmlBody
    ) {
        return ContactEmailEvent.builder()
                .recipientEmail(recipientEmail)
                .senderName(senderName)
                .senderEmail(senderEmail)
                .subject(subject)
                .message(message)
                .body(htmlBody)
                .build();
    }
}
