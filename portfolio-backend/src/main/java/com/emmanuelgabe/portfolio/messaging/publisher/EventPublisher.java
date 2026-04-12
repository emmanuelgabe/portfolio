package com.emmanuelgabe.portfolio.messaging.publisher;

import com.emmanuelgabe.portfolio.messaging.event.AuditEvent;
import com.emmanuelgabe.portfolio.messaging.event.AutoReplyEvent;
import com.emmanuelgabe.portfolio.messaging.event.EmailEvent;
import com.emmanuelgabe.portfolio.messaging.event.ImageProcessingEvent;

/**
 * Interface for publishing events to a message broker.
 * Supports email, image processing, audit, and auto-reply events.
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
     * Publish an auto-reply event for asynchronous RAG-powered reply generation.
     *
     * @param event the auto-reply event to publish
     */
    void publishAutoReplyEvent(AutoReplyEvent event);

    /**
     * Check if the publisher is enabled and operational.
     *
     * @return true if events can be published
     */
    boolean isEnabled();
}
