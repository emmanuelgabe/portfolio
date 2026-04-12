package com.emmanuelgabe.portfolio.healthmonitor.tools;

import com.emmanuelgabe.portfolio.dto.audit.AuditStatsResponse;
import com.emmanuelgabe.portfolio.healthmonitor.HealthToolCallTracker;
import com.emmanuelgabe.portfolio.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditStatsToolTest {

    @Mock
    private AuditService auditService;

    private HealthToolCallTracker tracker;
    private AuditStatsTool tool;

    @BeforeEach
    void setUp() {
        tracker = new HealthToolCallTracker();
        tool = new AuditStatsTool(auditService, tracker);
    }

    // ========== Happy path ==========

    @Test
    void should_returnStatsMap_when_getAuditStatsCalledWithValidDays() {
        // Arrange
        AuditStatsResponse stats = new AuditStatsResponse(
                Map.of("LOGIN", 50L, "UPDATE", 20L),
                Map.of("User", 30L, "Project", 40L),
                Map.of("2026-04-11", 25L),
                Map.of("admin", 70L),
                3L,
                70L);
        when(auditService.getStats(any(LocalDateTime.class))).thenReturn(stats);

        // Act
        Map<String, Object> result = tool.getAuditStats(7);

        // Assert
        assertThat(result).containsEntry("daysLookback", 7);
        assertThat(result).containsEntry("totalCount", 70L);
        assertThat(result).containsEntry("failedActionsCount", 3L);
        assertThat(result).containsEntry("actionCounts", Map.of("LOGIN", 50L, "UPDATE", 20L));
        assertThat(result).containsEntry("entityTypeCounts", Map.of("User", 30L, "Project", 40L));
        assertThat(result).containsEntry("userActivity", Map.of("admin", 70L));
        assertThat(tracker.getCalls()).containsExactly("getAuditStats");
    }

    // ========== Days clamping ==========

    @Test
    void should_clampDaysToOne_when_getAuditStatsCalledWithZeroDays() {
        // Arrange
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        when(auditService.getStats(captor.capture())).thenReturn(emptyStats());

        // Act
        Map<String, Object> result = tool.getAuditStats(0);

        // Assert
        assertThat(result).containsEntry("daysLookback", 1);
        LocalDateTime now = LocalDateTime.now();
        long diffDays = Duration.between(captor.getValue(), now).toDays();
        assertThat(diffDays).isBetween(0L, 2L);
    }

    @Test
    void should_clampDaysToThirty_when_getAuditStatsCalledWithExcessiveDays() {
        // Arrange
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        when(auditService.getStats(captor.capture())).thenReturn(emptyStats());

        // Act
        Map<String, Object> result = tool.getAuditStats(365);

        // Assert
        assertThat(result).containsEntry("daysLookback", 30);
        LocalDateTime now = LocalDateTime.now();
        long diffDays = Duration.between(captor.getValue(), now).toDays();
        assertThat(diffDays).isBetween(29L, 31L);
    }

    @Test
    void should_clampDaysToOne_when_getAuditStatsCalledWithNegativeDays() {
        // Arrange
        when(auditService.getStats(any(LocalDateTime.class))).thenReturn(emptyStats());

        // Act
        Map<String, Object> result = tool.getAuditStats(-5);

        // Assert
        assertThat(result).containsEntry("daysLookback", 1);
    }

    private static AuditStatsResponse emptyStats() {
        return new AuditStatsResponse(Map.of(), Map.of(), Map.of(), Map.of(), 0L, 0L);
    }
}
