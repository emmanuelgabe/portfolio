package com.emmanuelgabe.portfolio.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for running batch jobs on a schedule.
 * Uses cron expressions for job timing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.enabled", havingValue = "true", matchIfMissing = false)
public class BatchJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job auditCleanupJob;
    private final Job auditReportJob;
    private final Job statsAggregationJob;
    private final Job sitemapGenerationJob;
    private final Job searchReindexJob;

    /**
     * Run audit cleanup job daily at 2:00 AM.
     * Cron: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "${batch.audit-cleanup.cron:0 0 2 * * ?}")
    public void runAuditCleanupJob() {
        log.info("[BATCH_SCHEDULER] Starting scheduled audit cleanup job");

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(auditCleanupJob, params);

            log.info("[BATCH_SCHEDULER] Audit cleanup job completed - status={}, exitCode={}",
                    execution.getStatus(), execution.getExitStatus().getExitCode());

        } catch (Exception e) {
            log.error("[BATCH_SCHEDULER] Failed to run audit cleanup job - error={}", e.getMessage(), e);
        }
    }

    /**
     * Run monthly audit report job on the 1st of each month at 3:00 AM.
     * Generates a report for the previous month.
     */
    @Scheduled(cron = "${batch.audit-report.cron:0 0 3 1 * ?}")
    public void runAuditReportJob() {
        log.info("[BATCH_SCHEDULER] Starting scheduled audit report job");

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(auditReportJob, params);

            log.info("[BATCH_SCHEDULER] Audit report job completed - status={}, exitCode={}",
                    execution.getStatus(), execution.getExitStatus().getExitCode());

        } catch (Exception e) {
            log.error("[BATCH_SCHEDULER] Failed to run audit report job - error={}", e.getMessage(), e);
        }
    }

    /**
     * Run daily stats aggregation job at midnight.
     * Aggregates statistics from the previous day.
     */
    @Scheduled(cron = "${batch.stats-aggregation.cron:0 0 0 * * ?}")
    public void runStatsAggregationJob() {
        log.info("[BATCH_SCHEDULER] Starting scheduled stats aggregation job");

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(statsAggregationJob, params);

            log.info("[BATCH_SCHEDULER] Stats aggregation job completed - status={}, exitCode={}",
                    execution.getStatus(), execution.getExitStatus().getExitCode());

        } catch (Exception e) {
            log.error("[BATCH_SCHEDULER] Failed to run stats aggregation job - error={}", e.getMessage(), e);
        }
    }

    /**
     * Run sitemap generation job daily at 4:00 AM.
     * Regenerates the sitemap.xml with all current content.
     */
    @Scheduled(cron = "${batch.sitemap.cron:0 0 4 * * ?}")
    public void runSitemapGenerationJob() {
        log.info("[BATCH_SCHEDULER] Starting scheduled sitemap generation job");

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(sitemapGenerationJob, params);

            log.info("[BATCH_SCHEDULER] Sitemap generation job completed - status={}, exitCode={}",
                    execution.getStatus(), execution.getExitStatus().getExitCode());

        } catch (Exception e) {
            log.error("[BATCH_SCHEDULER] Failed to run sitemap generation job - error={}", e.getMessage(), e);
        }
    }

    /**
     * Run search reindex job every Sunday at 3:00 AM.
     * Rebuilds all Elasticsearch indices to ensure consistency with database.
     */
    @Scheduled(cron = "${batch.search-reindex.cron:0 0 3 * * SUN}")
    public void runSearchReindexJob() {
        log.info("[BATCH_SCHEDULER] Starting scheduled search reindex job");

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(searchReindexJob, params);

            log.info("[BATCH_SCHEDULER] Search reindex job completed - status={}, exitCode={}",
                    execution.getStatus(), execution.getExitStatus().getExitCode());

        } catch (Exception e) {
            log.error("[BATCH_SCHEDULER] Failed to run search reindex job - error={}", e.getMessage(), e);
        }
    }
}
