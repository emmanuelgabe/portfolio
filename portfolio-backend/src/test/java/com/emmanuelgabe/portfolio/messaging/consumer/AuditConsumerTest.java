package com.emmanuelgabe.portfolio.messaging.consumer;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.entity.AuditLog;
import com.emmanuelgabe.portfolio.messaging.event.AuditEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditConsumerTest {

    @Mock
    private AuditService auditService;

    @Mock
    private BusinessMetrics metrics;

    @InjectMocks
    private AuditConsumer auditConsumer;

    // ========== Handle Entity Audit Event Tests ==========

    @Test
    void should_delegateToService_when_handleAuditEventCalledWithEntityAudit() {
        // Arrange
        AuditEvent event = AuditEvent.forEntityAudit(
                null,
                AuditAction.CREATE,
                "Project",
                1L,
                "Test Project",
                null,
                Map.of("title", "Test Project"),
                null,
                true,
                null
        );

        AuditLog savedLog = new AuditLog();
        savedLog.setId(1L);
        when(auditService.persistAuditEvent(event)).thenReturn(savedLog);

        // Act
        auditConsumer.handleAuditEvent(event);

        // Assert
        verify(auditService).persistAuditEvent(event);
        verify(metrics).recordAuditLogged();
    }

    @Test
    void should_delegateToService_when_handleAuditEventCalledWithUpdate() {
        // Arrange
        Map<String, Object> oldValues = Map.of("title", "Old Title");
        Map<String, Object> newValues = Map.of("title", "New Title");
        List<String> changedFields = List.of("title");

        AuditEvent event = AuditEvent.forEntityAudit(
                null,
                AuditAction.UPDATE,
                "Article",
                5L,
                "Test Article",
                oldValues,
                newValues,
                changedFields,
                true,
                null
        );

        AuditLog savedLog = new AuditLog();
        savedLog.setId(2L);
        when(auditService.persistAuditEvent(event)).thenReturn(savedLog);

        // Act
        auditConsumer.handleAuditEvent(event);

        // Assert
        verify(auditService).persistAuditEvent(event);
        verify(metrics).recordAuditLogged();
    }

    @Test
    void should_delegateToService_when_handleAuditEventCalledWithContext() {
        // Arrange
        AuditEvent event = AuditEvent.builder()
                .eventId("test-id")
                .auditType(AuditEvent.AuditEventType.ENTITY_AUDIT)
                .action(AuditAction.DELETE)
                .entityType("Skill")
                .entityId(3L)
                .userId(1L)
                .username("admin")
                .userRole("ADMIN")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestMethod("DELETE")
                .requestUri("/api/admin/skills/3")
                .requestId("req-456")
                .success(true)
                .build();

        AuditLog savedLog = new AuditLog();
        savedLog.setId(3L);
        when(auditService.persistAuditEvent(event)).thenReturn(savedLog);

        // Act
        auditConsumer.handleAuditEvent(event);

        // Assert
        verify(auditService).persistAuditEvent(event);
        verify(metrics).recordAuditLogged();
    }

    // ========== Handle Auth Event Tests ==========

    @Test
    void should_delegateToService_when_handleAuditEventCalledWithAuthEvent() {
        // Arrange
        AuditEvent event = AuditEvent.forAuthEvent(
                AuditAction.LOGIN,
                "admin",
                "192.168.1.1",
                "Mozilla/5.0",
                true,
                null
        );

        AuditLog savedLog = new AuditLog();
        savedLog.setId(4L);
        when(auditService.persistAuditEvent(event)).thenReturn(savedLog);

        // Act
        auditConsumer.handleAuditEvent(event);

        // Assert
        verify(auditService).persistAuditEvent(event);
        verify(metrics).recordAuditLogged();
    }

    @Test
    void should_delegateToService_when_handleAuditEventCalledWithFailedAuth() {
        // Arrange
        AuditEvent event = AuditEvent.forAuthEvent(
                AuditAction.LOGIN,
                "unknown_user",
                "192.168.1.1",
                "Mozilla/5.0",
                false,
                "Invalid credentials"
        );

        AuditLog savedLog = new AuditLog();
        savedLog.setId(5L);
        when(auditService.persistAuditEvent(event)).thenReturn(savedLog);

        // Act
        auditConsumer.handleAuditEvent(event);

        // Assert
        verify(auditService).persistAuditEvent(event);
        verify(metrics).recordAuditLogged();
    }

    @Test
    void should_delegateToService_when_handleAuditEventCalledWithLogout() {
        // Arrange
        AuditEvent event = AuditEvent.forAuthEvent(
                AuditAction.LOGOUT,
                "admin",
                "192.168.1.1",
                "Mozilla/5.0",
                true,
                null
        );

        AuditLog savedLog = new AuditLog();
        savedLog.setId(6L);
        when(auditService.persistAuditEvent(event)).thenReturn(savedLog);

        // Act
        auditConsumer.handleAuditEvent(event);

        // Assert
        verify(auditService).persistAuditEvent(event);
        verify(metrics).recordAuditLogged();
    }

    // ========== Error Handling Tests ==========

    @Test
    void should_recordFailureMetric_when_persistenceFails() {
        // Arrange
        AuditEvent event = AuditEvent.forEntityAudit(
                null, AuditAction.CREATE, "Project", 1L, "Test",
                null, null, null, true, null
        );

        doThrow(new RuntimeException("DB error"))
                .when(auditService)
                .persistAuditEvent(any(AuditEvent.class));

        // Act & Assert
        assertThatThrownBy(() -> auditConsumer.handleAuditEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");

        verify(metrics).recordAuditLogFailure();
        verify(metrics, never()).recordAuditLogged();
    }

    @Test
    void should_rethrowException_when_persistenceFails() {
        // Arrange
        AuditEvent event = AuditEvent.forEntityAudit(
                null, AuditAction.UPDATE, "Article", 5L, "Test",
                null, null, null, true, null
        );

        RuntimeException expectedException = new RuntimeException("Connection lost");
        doThrow(expectedException)
                .when(auditService)
                .persistAuditEvent(any(AuditEvent.class));

        // Act & Assert
        assertThatThrownBy(() -> auditConsumer.handleAuditEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Connection lost");
    }
}
