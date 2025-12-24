package com.emmanuelgabe.portfolio.kafka.consumer;

import com.emmanuelgabe.portfolio.kafka.event.ActivityEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for ActivityEventConsumer.
 */
@ExtendWith(MockitoExtension.class)
class ActivityEventConsumerTest {

    @Mock
    private BusinessMetrics metrics;

    private ActivityEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ActivityEventConsumer(metrics);
    }

    // ========== handleActivityEvent Tests ==========

    @Test
    void should_recordMetric_when_sessionStartEventReceived() {
        // Arrange
        ActivityEvent event = ActivityEvent.sessionStart(
                "session-123", "192.168.1.100", "Mozilla/5.0");

        // Act
        consumer.handleActivityEvent(event);

        // Assert
        verify(metrics).recordKafkaEventConsumed("activity_events", "SESSION_START");
    }

    @Test
    void should_recordMetric_when_sessionEndEventReceived() {
        // Arrange
        ActivityEvent event = ActivityEvent.sessionEnd("session-456", 300000L);

        // Act
        consumer.handleActivityEvent(event);

        // Assert
        verify(metrics).recordKafkaEventConsumed("activity_events", "SESSION_END");
    }

    @Test
    void should_recordMetric_when_navigationEventReceived() {
        // Arrange
        ActivityEvent event = ActivityEvent.navigation(
                "session-789", "/projects/1", "/home");

        // Act
        consumer.handleActivityEvent(event);

        // Assert
        verify(metrics).recordKafkaEventConsumed("activity_events", "NAVIGATION");
    }

    @Test
    void should_recordMetric_when_idleTimeoutEventReceived() {
        // Arrange
        ActivityEvent event = ActivityEvent.idleTimeout("session-abc", 1800000L);

        // Act
        consumer.handleActivityEvent(event);

        // Assert
        verify(metrics).recordKafkaEventConsumed("activity_events", "IDLE_TIMEOUT");
    }

    @Test
    void should_recordFailureMetric_when_exceptionOccurs() {
        // Arrange
        ActivityEvent event = ActivityEvent.sessionStart(
                "session-error", "10.0.0.1", "Chrome");

        doThrow(new RuntimeException("Processing error"))
                .when(metrics).recordKafkaEventConsumed("activity_events", "SESSION_START");

        // Act & Assert
        assertThatThrownBy(() -> consumer.handleActivityEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Processing error");
        verify(metrics).recordKafkaConsumeFailure("activity_events", "SESSION_START");
    }
}
