package com.emmanuelgabe.portfolio.batch;

import com.emmanuelgabe.portfolio.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuditCleanupJobConfig.
 */
@ExtendWith(MockitoExtension.class)
class AuditCleanupJobConfigTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private StepContribution stepContribution;

    @Mock
    private ChunkContext chunkContext;

    @Captor
    private ArgumentCaptor<LocalDateTime> dateCaptor;

    private AuditCleanupJobConfig jobConfig;

    @BeforeEach
    void setUp() {
        jobConfig = new AuditCleanupJobConfig(auditLogRepository);
        ReflectionTestUtils.setField(jobConfig, "retentionDays", 90);
    }

    // ========== auditCleanupTasklet Tests ==========

    @Test
    void should_deleteOldLogs_when_taskletExecuted() throws Exception {
        // Arrange
        when(auditLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(100L);

        Tasklet tasklet = jobConfig.auditCleanupTasklet();

        // Act
        RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

        // Assert
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(auditLogRepository).deleteByCreatedAtBefore(dateCaptor.capture());

        LocalDateTime cutoffDate = dateCaptor.getValue();
        LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(90);

        // Allow 1 minute tolerance for test execution time
        assertThat(cutoffDate).isBetween(
                expectedCutoff.minusMinutes(1),
                expectedCutoff.plusMinutes(1)
        );
    }

    @Test
    void should_useConfiguredRetentionDays_when_taskletExecuted() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(jobConfig, "retentionDays", 30);
        when(auditLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(50L);

        Tasklet tasklet = jobConfig.auditCleanupTasklet();

        // Act
        tasklet.execute(stepContribution, chunkContext);

        // Assert
        verify(auditLogRepository).deleteByCreatedAtBefore(dateCaptor.capture());

        LocalDateTime cutoffDate = dateCaptor.getValue();
        LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(30);

        assertThat(cutoffDate).isBetween(
                expectedCutoff.minusMinutes(1),
                expectedCutoff.plusMinutes(1)
        );
    }

    @Test
    void should_incrementWriteCount_when_logsDeleted() throws Exception {
        // Arrange
        when(auditLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(75L);

        Tasklet tasklet = jobConfig.auditCleanupTasklet();

        // Act
        tasklet.execute(stepContribution, chunkContext);

        // Assert
        verify(stepContribution).incrementWriteCount(75L);
    }

    @Test
    void should_returnFinished_when_noLogsToDelete() throws Exception {
        // Arrange
        when(auditLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(0L);

        Tasklet tasklet = jobConfig.auditCleanupTasklet();

        // Act
        RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

        // Assert
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(stepContribution).incrementWriteCount(0L);
    }
}
