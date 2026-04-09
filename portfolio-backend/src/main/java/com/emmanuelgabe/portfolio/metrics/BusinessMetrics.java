package com.emmanuelgabe.portfolio.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Business metrics for portfolio application monitoring.
 * Provides counters and timers for tracking key business operations.
 */
@Component
@Getter
public class BusinessMetrics {

    private final MeterRegistry registry;
    private final Counter contactFormSubmissions;
    private final Counter contactFormFailures;
    private final Counter projectViews;
    private final Counter articleViews;
    private final Counter authAttempts;
    private final Counter authFailures;
    private final Counter rateLimitHits;
    private final Counter imagesProcessed;
    private final Counter imageProcessingFailures;
    private final Counter auditLogsCreated;
    private final Counter auditLogFailures;
    private final Counter autoReplyGenerated;
    private final Counter autoReplyFailures;
    private final Counter moderationApprovals;
    private final Counter moderationRejections;
    private final Counter moderationFailures;
    private final Timer moderationTimer;
    private final Timer imageProcessingTimer;
    private final Timer emailSendingTimer;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.contactFormSubmissions = Counter.builder("portfolio.contact.submissions")
                .description("Total contact form submissions")
                .register(registry);

        this.contactFormFailures = Counter.builder("portfolio.contact.failures")
                .description("Total contact form failures")
                .register(registry);

        this.projectViews = Counter.builder("portfolio.projects.views")
                .description("Total project page views")
                .register(registry);

        this.articleViews = Counter.builder("portfolio.articles.views")
                .description("Total article page views")
                .register(registry);

        this.authAttempts = Counter.builder("portfolio.auth.attempts")
                .description("Total authentication attempts")
                .register(registry);

        this.authFailures = Counter.builder("portfolio.auth.failures")
                .description("Total authentication failures")
                .register(registry);

        this.rateLimitHits = Counter.builder("portfolio.ratelimit.hits")
                .description("Total rate limit hits")
                .register(registry);

        this.imagesProcessed = Counter.builder("portfolio.images.processed")
                .description("Total images processed successfully")
                .register(registry);

        this.imageProcessingFailures = Counter.builder("portfolio.images.failures")
                .description("Total image processing failures")
                .register(registry);

        this.auditLogsCreated = Counter.builder("portfolio.audit.created")
                .description("Total audit logs created")
                .register(registry);

        this.auditLogFailures = Counter.builder("portfolio.audit.failures")
                .description("Total audit log creation failures")
                .register(registry);

        this.autoReplyGenerated = Counter.builder("portfolio.autoreply.generated")
                .description("Total auto-replies generated and sent")
                .register(registry);

        this.autoReplyFailures = Counter.builder("portfolio.autoreply.failures")
                .description("Total auto-reply generation failures")
                .register(registry);

        this.contactPersistenceFailures = Counter.builder("portfolio.contact.persistence.failures")
                .description("Total contact persistence failures")
                .register(registry);

        this.moderationApprovals = Counter.builder("portfolio.moderation.approvals")
                .description("Total messages approved by moderation")
                .register(registry);

        this.moderationRejections = Counter.builder("portfolio.moderation.rejections")
                .description("Total messages rejected by moderation")
                .register(registry);

        this.moderationFailures = Counter.builder("portfolio.moderation.failures")
                .description("Total moderation failures (auto-approved on error)")
                .register(registry);

        this.moderationTimer = Timer.builder("portfolio.moderation.duration")
                .description("AI moderation analysis duration")
                .register(registry);

        this.imageProcessingTimer = Timer.builder("portfolio.image.processing")
                .description("Image processing duration")
                .register(registry);

        this.emailSendingTimer = Timer.builder("portfolio.email.sending")
                .description("Email sending duration")
                .register(registry);
    }

    /**
     * Record a contact form submission
     */
    public void recordContactSubmission() {
        contactFormSubmissions.increment();
    }

    /**
     * Record a contact form failure
     */
    public void recordContactFailure() {
        contactFormFailures.increment();
    }

    /**
     * Record a project view
     */
    public void recordProjectView() {
        projectViews.increment();
    }

    /**
     * Record an article view
     */
    public void recordArticleView() {
        articleViews.increment();
    }

    /**
     * Record an authentication attempt
     */
    public void recordAuthAttempt() {
        authAttempts.increment();
    }

    /**
     * Record an authentication failure
     */
    public void recordAuthFailure() {
        authFailures.increment();
    }

    /**
     * Record a successfully generated and sent auto-reply
     */
    public void recordAutoReplyGenerated() {
        autoReplyGenerated.increment();
    }

    /**
     * Record an auto-reply generation failure
     */
    public void recordAutoReplyFailure() {
        autoReplyFailures.increment();
    }

    /**
     * Record a contact persistence failure
     */
    public void recordContactPersistenceFailure() {
        contactPersistenceFailures.increment();
    }

    /**
     * Record a moderation approval
     */
    public void recordModerationApproval() {
        moderationApprovals.increment();
    }

    /**
     * Record a moderation rejection
     */
    public void recordModerationRejection() {
        moderationRejections.increment();
    }

    /**
     * Record a moderation failure (AI error, timeout)
     */
    public void recordModerationFailure() {
        moderationFailures.increment();
    }

    /**
     * Get the moderation timer for recording duration
     *
     * @return the moderation timer
     */
    public Timer getModerationTimer() {
        return moderationTimer;
    }

    /**
     * Record a rate limit hit
     */
    public void recordRateLimitHit() {
        rateLimitHits.increment();
    }

    /**
     * Record a successfully processed image
     */
    public void recordImageProcessed() {
        imagesProcessed.increment();
    }

    /**
     * Record an image processing failure
     */
    public void recordImageProcessingFailure() {
        imageProcessingFailures.increment();
    }

    /**
     * Record a successfully created audit log
     */
    public void recordAuditLogged() {
        auditLogsCreated.increment();
    }

    /**
     * Record an audit log creation failure
     */
    public void recordAuditLogFailure() {
        auditLogFailures.increment();
    }

    /**
     * Record a message sent to dead letter queue
     *
     * @param queueType the type of queue (email, image, audit)
     */
    public void recordDeadLetterMessage(String queueType) {
        Counter.builder("portfolio.dlq.messages")
                .description("Messages sent to dead letter queue")
                .tags(Tags.of("queue_type", queueType))
                .register(registry)
                .increment();
    }

    private final Counter contactPersistenceFailures;

    // ========== Moderation Category Metrics ==========

    /**
     * Record a contact message categorized by AI moderation.
     *
     * @param category the assigned category name (MISSION, QUESTION, FEEDBACK, SPAM, INAPPROPRIATE)
     */
    public void recordModerationCategory(String category) {
        Counter.builder("portfolio.moderation.categories")
                .description("Contact messages by AI moderation category")
                .tags(Tags.of("category", category))
                .register(registry)
                .increment();
    }

    // ========== Kafka Metrics ==========

    /**
     * Record a Kafka event published successfully
     *
     * @param topic the topic name
     * @param eventType the type of event
     */
    public void recordKafkaEventPublished(String topic, String eventType) {
        Counter.builder("portfolio.kafka.events.published")
                .description("Kafka events published successfully")
                .tags(Tags.of("topic", topic, "event_type", eventType))
                .register(registry)
                .increment();
    }

    /**
     * Record a Kafka event consumed successfully
     *
     * @param topic the topic name
     * @param eventType the type of event
     */
    public void recordKafkaEventConsumed(String topic, String eventType) {
        Counter.builder("portfolio.kafka.events.consumed")
                .description("Kafka events consumed successfully")
                .tags(Tags.of("topic", topic, "event_type", eventType))
                .register(registry)
                .increment();
    }

    /**
     * Record a Kafka event publish failure
     *
     * @param topic the topic name
     * @param eventType the type of event
     */
    public void recordKafkaPublishFailure(String topic, String eventType) {
        Counter.builder("portfolio.kafka.events.publish.failed")
                .description("Kafka event publish failures")
                .tags(Tags.of("topic", topic, "event_type", eventType))
                .register(registry)
                .increment();
    }

    /**
     * Record a Kafka event consume failure
     *
     * @param topic the topic name
     * @param eventType the type of event
     */
    public void recordKafkaConsumeFailure(String topic, String eventType) {
        Counter.builder("portfolio.kafka.events.consume.failed")
                .description("Kafka event consume failures")
                .tags(Tags.of("topic", topic, "event_type", eventType))
                .register(registry)
                .increment();
    }
}
