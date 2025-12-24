package com.emmanuelgabe.portfolio.messaging.publisher;

import com.emmanuelgabe.portfolio.messaging.event.AuditEvent;
import com.emmanuelgabe.portfolio.messaging.event.EmailEvent;
import com.emmanuelgabe.portfolio.messaging.event.ImageProcessingEvent;

/**
 * Interface for publishing events to a message broker.
 * Supports email, image processing, and audit events.
 */
public interface EventPublisher {

    /**
     * Publish an email event for asynchronous processing.
     *
     * @param event the email event to publish
     */
    void publishEmailEvent(EmailEvent event);

    /**
     * Publish an image processing event for asynchronous processing.
     *
     * @param event the image processing event to publish
     */
    void publishImageEvent(ImageProcessingEvent event);

    /**
     * Publish an audit event for asynchronous logging.
     *
     * @param event the audit event to publish
     */
    void publishAuditEvent(AuditEvent event);

    /**
     * Check if the publisher is enabled and operational.
     *
     * @return true if events can be published
     */
    boolean isEnabled();
}
