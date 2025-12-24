package com.emmanuelgabe.portfolio.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration for event streaming.
 * Creates topics for admin actions, analytics, and activity events.
 */
@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConfig {

    public static final String TOPIC_ADMIN_EVENTS = "portfolio.admin.events";
    public static final String TOPIC_ANALYTICS_EVENTS = "portfolio.analytics.events";
    public static final String TOPIC_ACTIVITY_EVENTS = "portfolio.activity.events";

    /**
     * Topic for admin action events (CRUD operations, authentication).
     * Used for event sourcing and audit trail.
     */
    @Bean
    public NewTopic adminEventsTopic() {
        return TopicBuilder.name(TOPIC_ADMIN_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic for analytics events (page views, interactions).
     * Used for real-time analytics processing.
     */
    @Bean
    public NewTopic analyticsEventsTopic() {
        return TopicBuilder.name(TOPIC_ANALYTICS_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic for activity events (user sessions, navigation).
     * Used for activity tracking and reporting.
     */
    @Bean
    public NewTopic activityEventsTopic() {
        return TopicBuilder.name(TOPIC_ACTIVITY_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
