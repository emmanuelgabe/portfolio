package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.audit.AuditStatsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuditReportService.
 */
@ExtendWith(MockitoExtension.class)
class AuditReportServiceTest {

    @Mock
    private AuditService auditService;

    private ObjectMapper objectMapper;
    private AuditReportService auditReportService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        auditReportService = new AuditReportService(auditService, objectMapper);
        ReflectionTestUtils.setField(auditReportService, "outputDir", tempDir.toString());
    }

    // ========== generateMonthlyReport Tests ==========

    @Test
    void should_generateReport_when_generateMonthlyReportCalled() throws IOException {
        // Arrange
        AuditStatsResponse stats = createMockStats();
        when(auditService.getStats(any(LocalDateTime.class))).thenReturn(stats);

        // Act
        File reportFile = auditReportService.generateMonthlyReport();

        // Assert
        assertThat(reportFile).exists();
        assertThat(reportFile.getName()).startsWith("audit-report-");
        assertThat(reportFile.getName()).endsWith(".json");

        String expectedFilename = String.format("audit-report-%s.json",
                YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM")));
        assertThat(reportFile.getName()).isEqualTo(expectedFilename);
    }

    @Test
    void should_createDirectory_when_outputDirNotExists() throws IOException {
        // Arrange
        Path newDir = tempDir.resolve("new-reports");
        ReflectionTestUtils.setField(auditReportService, "outputDir", newDir.toString());

        AuditStatsResponse stats = createMockStats();
        when(auditService.getStats(any(LocalDateTime.class))).thenReturn(stats);

        // Act
        File reportFile = auditReportService.generateMonthlyReport();

        // Assert
        assertThat(newDir.toFile()).exists();
        assertThat(reportFile).exists();
    }

    @Test
    void should_includeStatistics_when_reportGenerated() throws IOException {
        // Arrange
        AuditStatsResponse stats = createMockStats();
        stats.setTotalCount(100L);
        when(auditService.getStats(any(LocalDateTime.class))).thenReturn(stats);

        // Act
        File reportFile = auditReportService.generateMonthlyReport();

        // Assert
        String content = java.nio.file.Files.readString(reportFile.toPath());
        assertThat(content).contains("MONTHLY_AUDIT_SUMMARY");
        assertThat(content).contains("statistics");
        assertThat(content).contains("100");
    }

    @Test
    void should_throwException_when_cannotCreateDirectory() {
        // Arrange
        // Use an invalid path that can't be created
        ReflectionTestUtils.setField(auditReportService, "outputDir", "\0invalid");

        AuditStatsResponse stats = createMockStats();
        when(auditService.getStats(any(LocalDateTime.class))).thenReturn(stats);

        // Act / Assert
        assertThatThrownBy(() -> auditReportService.generateMonthlyReport())
                .isInstanceOf(IOException.class);
    }

    private AuditStatsResponse createMockStats() {
        AuditStatsResponse stats = new AuditStatsResponse();
        stats.setTotalCount(50L);
        stats.setFailedActionsCount(2L);
        stats.setActionCounts(new HashMap<>());
        stats.setEntityTypeCounts(new HashMap<>());
        stats.setDailyActivity(new HashMap<>());
        stats.setUserActivity(new HashMap<>());
        return stats;
    }
}
