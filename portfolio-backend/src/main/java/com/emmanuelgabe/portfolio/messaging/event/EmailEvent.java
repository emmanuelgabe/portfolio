package com.emmanuelgabe.portfolio.messaging.event;

import java.io.Serializable;
import java.time.Instant;

/**
 * Base interface for all email-related events.
 * Provides common contract for email message handling.
 */
public interface EmailEvent extends Serializable {

    /**
     * Get the unique event identifier.
     */
    String getEventId();

    /**
     * Get the event type for routing and handling.
     */
    String getEventType();

    /**
     * Get the timestamp when the event was created.
     */
    Instant getCreatedAt();

    /**
     * Get the recipient email address.
     */
    String getRecipientEmail();

    /**
     * Get the email subject.
     */
    String getSubject();

    /**
     * Get the email body content (HTML).
     */
    String getBody();
}
