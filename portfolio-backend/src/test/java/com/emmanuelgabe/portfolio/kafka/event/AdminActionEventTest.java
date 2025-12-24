package com.emmanuelgabe.portfolio.kafka.event;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AdminActionEvent.
 */
class AdminActionEventTest {

    // ========== Factory Method Tests ==========

    @Test
    void should_createSuccessEvent_when_successFactoryMethodCalled() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Test Project");

        // Act
        AdminActionEvent event = AdminActionEvent.success(
                "CREATE", "Project", 1L, "Test Project",
                "admin", "192.168.1.1", payload);

        // Assert
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo(AdminActionEvent.EVENT_TYPE);
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getSource()).isEqualTo("portfolio-backend");
        assertThat(event.getAction()).isEqualTo("CREATE");
        assertThat(event.getEntityType()).isEqualTo("Project");
        assertThat(event.getEntityId()).isEqualTo(1L);
        assertThat(event.getEntityName()).isEqualTo("Test Project");
        assertThat(event.getUsername()).isEqualTo("admin");
        assertThat(event.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(event.getPayload()).isEqualTo(payload);
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getErrorMessage()).isNull();
    }

    @Test
    void should_createFailureEvent_when_failureFactoryMethodCalled() {
        // Act
        AdminActionEvent event = AdminActionEvent.failure(
                "DELETE", "Article", 5L, "My Article",
                "admin", "10.0.0.1", "Permission denied");

        // Assert
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo(AdminActionEvent.EVENT_TYPE);
        assertThat(event.getAction()).isEqualTo("DELETE");
        assertThat(event.getEntityType()).isEqualTo("Article");
        assertThat(event.getEntityId()).isEqualTo(5L);
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getErrorMessage()).isEqualTo("Permission denied");
        assertThat(event.getPayload()).isNull();
    }

    @Test
    void should_generateUniqueEventId_when_multipleEventsCreated() {
        // Act
        AdminActionEvent event1 = AdminActionEvent.success(
                "CREATE", "Project", 1L, "Project 1",
                "admin", "127.0.0.1", null);
        AdminActionEvent event2 = AdminActionEvent.success(
                "CREATE", "Project", 2L, "Project 2",
                "admin", "127.0.0.1", null);

        // Assert
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }

    @Test
    void should_haveCorrectEventType_when_eventCreated() {
        // Assert
        assertThat(AdminActionEvent.EVENT_TYPE).isEqualTo("ADMIN_ACTION");
    }
}
