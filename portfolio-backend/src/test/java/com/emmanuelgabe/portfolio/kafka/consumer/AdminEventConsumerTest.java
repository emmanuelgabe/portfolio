package com.emmanuelgabe.portfolio.kafka.consumer;

import com.emmanuelgabe.portfolio.kafka.event.AdminActionEvent;
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
 * Unit tests for AdminEventConsumer.
 */
@ExtendWith(MockitoExtension.class)
class AdminEventConsumerTest {

    @Mock
    private BusinessMetrics metrics;

    private AdminEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AdminEventConsumer(metrics);
    }

    // ========== handleAdminEvent Tests ==========

    @Test
    void should_processSuccessEvent_when_successEventReceived() {
        // Arrange
        AdminActionEvent event = AdminActionEvent.success(
                "CREATE", "Project", 1L, "Test Project",
                "admin", "192.168.1.1", null);

        // Act
        consumer.handleAdminEvent(event);

        // Assert
        verify(metrics).recordKafkaEventConsumed("admin_events", "CREATE");
    }

    @Test
    void should_processFailureEvent_when_failureEventReceived() {
        // Arrange
        AdminActionEvent event = AdminActionEvent.failure(
                "DELETE", "Article", 5L, "Test Article",
                "admin", "10.0.0.1", "Permission denied");

        // Act
        consumer.handleAdminEvent(event);

        // Assert
        verify(metrics).recordKafkaEventConsumed("admin_events", "DELETE");
    }

    @Test
    void should_processUpdateEvent_when_updateEventReceived() {
        // Arrange
        AdminActionEvent event = AdminActionEvent.success(
                "UPDATE", "Skill", 10L, "Java",
                "admin", "127.0.0.1", null);

        // Act
        consumer.handleAdminEvent(event);

        // Assert
        verify(metrics).recordKafkaEventConsumed("admin_events", "UPDATE");
    }

    @Test
    void should_processLoginEvent_when_loginEventReceived() {
        // Arrange
        AdminActionEvent event = AdminActionEvent.success(
                "LOGIN", "User", null, "admin",
                "admin", "192.168.0.1", null);

        // Act
        consumer.handleAdminEvent(event);

        // Assert
        verify(metrics).recordKafkaEventConsumed("admin_events", "LOGIN");
    }

    @Test
    void should_recordFailureMetric_when_exceptionOccurs() {
        // Arrange
        AdminActionEvent event = AdminActionEvent.success(
                "CREATE", "Project", 1L, "Test",
                "admin", "127.0.0.1", null);

        doThrow(new RuntimeException("Test error"))
                .when(metrics).recordKafkaEventConsumed("admin_events", "CREATE");

        // Act & Assert
        assertThatThrownBy(() -> consumer.handleAdminEvent(event))
                .isInstanceOf(RuntimeException.class);
        verify(metrics).recordKafkaConsumeFailure("admin_events", "CREATE");
    }
}
