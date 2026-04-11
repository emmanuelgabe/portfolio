package com.emmanuelgabe.portfolio.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.time.Instant;
import java.util.UUID;

/**
 * Email alert emitted by the AI Health Monitor when a diagnostic cycle
 * produces a CRITICAL severity report. Sent through the existing email
 * queue so it benefits from the same retry/DLQ guarantees as contact mails.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthAlertEmailEvent implements EmailEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String EVENT_TYPE = "HEALTH_ALERT";

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant createdAt = Instant.now();

    private String recipientEmail;
    private String subject;
    private String body;
    private Long reportId;
    private String severity;

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}
