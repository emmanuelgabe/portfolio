package com.emmanuelgabe.portfolio.messaging.event;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.audit.AuditContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventTest {

    // ========== Factory Method Tests - Entity Audit ==========

    @Test
    void should_createEntityAuditEvent_when_forEntityAuditCalledWithValidData() {
        // Arrange
        AuditContext context = createTestContext();
        Map<String, Object> oldValues = Map.of("title", "Old Title");
        Map<String, Object> newValues = Map.of("title", "New Title");
        List<String> changedFields = List.of("title");

        // Act
        AuditEvent event = AuditEvent.forEntityAudit(
                context,
                AuditAction.UPDATE,
                "Project",
                1L,
                "Test Project",
                oldValues,
                newValues,
                changedFields,
                true,
                null
        );

        // Assert
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getAuditType()).isEqualTo(AuditEvent.AuditEventType.ENTITY_AUDIT);
        assertThat(event.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(event.getEntityType()).isEqualTo("Project");
        assertThat(event.getEntityId()).isEqualTo(1L);
        assertThat(event.getEntityName()).isEqualTo("Test Project");
        assertThat(event.getOldValues()).isEqualTo(oldValues);
        assertThat(event.getNewValues()).isEqualTo(newValues);
        assertThat(event.getChangedFields()).isEqualTo(changedFields);
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getErrorMessage()).isNull();
    }

    @Test
    void should_includeContextInfo_when_forEntityAuditCalledWithContext() {
        // Arrange
        AuditContext context = createTestContext();

        // Act
        AuditEvent event = AuditEvent.forEntityAudit(
                context,
                AuditAction.CREATE,
                "Article",
                5L,
                "Test Article",
                null,
                Map.of("title", "Test"),
                null,
                true,
                null
        );

        // Assert
        assertThat(event.getUserId()).isEqualTo(1L);
        assertThat(event.getUsername()).isEqualTo("admin");
        assertThat(event.getUserRole()).isEqualTo("ADMIN");
        assertThat(event.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(event.getUserAgent()).isEqualTo("TestAgent");
        assertThat(event.getRequestMethod()).isEqualTo("POST");
        assertThat(event.getRequestUri()).isEqualTo("/api/articles");
        assertThat(event.getRequestId()).isEqualTo("req-123");
    }

    @Test
    void should_handleNullContext_when_forEntityAuditCalledWithNullContext() {
        // Act
        AuditEvent event = AuditEvent.forEntityAudit(
                null,
                AuditAction.DELETE,
                "Skill",
                3L,
                "Java",
                Map.of("name", "Java"),
                null,
                null,
                true,
                null
        );

        // Assert
        assertThat(event.getUserId()).isNull();
        assertThat(event.getUsername()).isNull();
        assertThat(event.getUserRole()).isNull();
        assertThat(event.getIpAddress()).isNull();
    }

    @Test
    void should_includeErrorInfo_when_forEntityAuditCalledWithFailure() {
        // Act
        AuditEvent event = AuditEvent.forEntityAudit(
                createTestContext(),
                AuditAction.CREATE,
                "Project",
                null,
                null,
                null,
                null,
                null,
                false,
                "Validation failed"
        );

        // Assert
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getErrorMessage()).isEqualTo("Validation failed");
    }

    // ========== Factory Method Tests - Auth Event ==========

    @Test
    void should_createAuthEvent_when_forAuthEventCalledWithValidData() {
        // Act
        AuditEvent event = AuditEvent.forAuthEvent(
                AuditAction.LOGIN,
                "admin",
                "192.168.1.1",
                "Mozilla/5.0",
                true,
                null
        );

        // Assert
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getAuditType()).isEqualTo(AuditEvent.AuditEventType.AUTH_EVENT);
        assertThat(event.getAction()).isEqualTo(AuditAction.LOGIN);
        assertThat(event.getEntityType()).isEqualTo("User");
        assertThat(event.getUsername()).isEqualTo("admin");
        assertThat(event.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(event.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(event.isSuccess()).isTrue();
    }

    @Test
    void should_useUnknownUsername_when_forAuthEventCalledWithNullUsername() {
        // Act
        AuditEvent event = AuditEvent.forAuthEvent(
                AuditAction.LOGIN,
                null,
                "192.168.1.1",
                "Mozilla/5.0",
                false,
                "Invalid credentials"
        );

        // Assert
        assertThat(event.getUsername()).isEqualTo("unknown");
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getErrorMessage()).isEqualTo("Invalid credentials");
    }

    @Test
    void should_createLogoutEvent_when_forAuthEventCalledWithLogoutAction() {
        // Act
        AuditEvent event = AuditEvent.forAuthEvent(
                AuditAction.LOGOUT,
                "admin",
                "192.168.1.1",
                "Mozilla/5.0",
                true,
                null
        );

        // Assert
        assertThat(event.getAction()).isEqualTo(AuditAction.LOGOUT);
        assertThat(event.isSuccess()).isTrue();
    }

    // ========== Unique ID Generation Tests ==========

    @Test
    void should_generateUniqueEventId_when_multipleEventsCreated() {
        // Act
        AuditEvent event1 = AuditEvent.forEntityAudit(
                null, AuditAction.CREATE, "Project", 1L, "P1",
                null, null, null, true, null);
        AuditEvent event2 = AuditEvent.forEntityAudit(
                null, AuditAction.CREATE, "Project", 2L, "P2",
                null, null, null, true, null);

        // Assert
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }

    // ========== Event Type Tests ==========

    @Test
    void should_returnCorrectEventType_when_getEventTypeCalled() {
        // Arrange
        AuditEvent event = AuditEvent.forEntityAudit(
                null, AuditAction.CREATE, "Project", 1L, "Test",
                null, null, null, true, null);

        // Act
        String eventType = event.getEventType();

        // Assert
        assertThat(eventType).isEqualTo("AUDIT");
    }

    @Test
    void should_haveCorrectEventTypeConstant() {
        // Assert
        assertThat(AuditEvent.EVENT_TYPE).isEqualTo("AUDIT");
    }

    // ========== Builder Tests ==========

    @Test
    void should_createEvent_when_builderUsed() {
        // Arrange & Act
        AuditEvent event = AuditEvent.builder()
                .eventId("custom-id")
                .auditType(AuditEvent.AuditEventType.ENTITY_AUDIT)
                .action(AuditAction.UPDATE)
                .entityType("Project")
                .entityId(10L)
                .success(true)
                .build();

        // Assert
        assertThat(event.getEventId()).isEqualTo("custom-id");
        assertThat(event.getAuditType()).isEqualTo(AuditEvent.AuditEventType.ENTITY_AUDIT);
        assertThat(event.getEntityId()).isEqualTo(10L);
    }

    // ========== Helper Methods ==========

    private AuditContext createTestContext() {
        return AuditContext.builder()
                .userId(1L)
                .username("admin")
                .userRole("ADMIN")
                .ipAddress("127.0.0.1")
                .userAgent("TestAgent")
                .requestMethod("POST")
                .requestUri("/api/articles")
                .requestId("req-123")
                .build();
    }
}
