package com.emmanuelgabe.portfolio.batch;

import com.emmanuelgabe.portfolio.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

/**
 * Spring Batch job configuration for cleaning up old audit logs.
 * Deletes audit logs older than the configured retention period.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.enabled", havingValue = "true", matchIfMissing = false)
public class AuditCleanupJobConfig {

    private final AuditLogRepository auditLogRepository;

    @Value("${batch.audit-cleanup.retention-days:90}")
    private int retentionDays;

    @Bean
    public Job auditCleanupJob(JobRepository jobRepository, Step auditCleanupStep) {
        return new JobBuilder("auditCleanupJob", jobRepository)
                .start(auditCleanupStep)
                .build();
    }

    @Bean
    public Step auditCleanupStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager) {
        return new StepBuilder("auditCleanupStep", jobRepository)
                .tasklet(auditCleanupTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet auditCleanupTasklet() {
        return (contribution, chunkContext) -> {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            log.info("[BATCH_AUDIT_CLEANUP] Starting cleanup - cutoffDate={}, retentionDays={}",
                    cutoffDate, retentionDays);

            long deletedCount = auditLogRepository.deleteByCreatedAtBefore(cutoffDate);

            log.info("[BATCH_AUDIT_CLEANUP] Cleanup completed - deletedCount={}", deletedCount);

            contribution.incrementWriteCount(deletedCount);
            return RepeatStatus.FINISHED;
        };
    }
}
