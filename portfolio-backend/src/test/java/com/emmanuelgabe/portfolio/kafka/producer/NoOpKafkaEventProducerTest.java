package com.emmanuelgabe.portfolio.kafka.producer;

import com.emmanuelgabe.portfolio.kafka.event.AdminActionEvent;
import com.emmanuelgabe.portfolio.kafka.event.AnalyticsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for NoOpKafkaEventProducer.
 */
class NoOpKafkaEventProducerTest {

    private NoOpKafkaEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new NoOpKafkaEventProducer();
    }

    // ========== publishAdminEvent Tests ==========

    @Test
    void should_notThrowException_when_publishAdminEventCalled() {
        // Arrange
        AdminActionEvent event = AdminActionEvent.success(
                "CREATE", "Project", 1L, "Test",
                "admin", "127.0.0.1", null);

        // Act & Assert
        assertThatCode(() -> producer.publishAdminEvent(event))
                .doesNotThrowAnyException();
    }

    // ========== publishAnalyticsEvent Tests ==========

    @Test
    void should_notThrowException_when_publishAnalyticsEventCalled() {
        // Arrange
        AnalyticsEvent event = AnalyticsEvent.pageView(
                "/", null, null, "127.0.0.1", "session123");

        // Act & Assert
        assertThatCode(() -> producer.publishAnalyticsEvent(event))
                .doesNotThrowAnyException();
    }

}
