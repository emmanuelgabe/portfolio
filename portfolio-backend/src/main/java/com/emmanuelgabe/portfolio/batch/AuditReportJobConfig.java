package com.emmanuelgabe.portfolio.batch;

import com.emmanuelgabe.portfolio.service.AuditReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;

/**
 * Spring Batch job configuration for generating monthly audit reports.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.enabled", havingValue = "true", matchIfMissing = false)
public class AuditReportJobConfig {

    private final AuditReportService auditReportService;

    @Bean
    public Job auditReportJob(JobRepository jobRepository, Step auditReportStep) {
        return new JobBuilder("auditReportJob", jobRepository)
                .start(auditReportStep)
                .build();
    }

    @Bean
    public Step auditReportStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
        return new StepBuilder("auditReportStep", jobRepository)
                .tasklet(auditReportTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet auditReportTasklet() {
        return (contribution, chunkContext) -> {
            log.info("[BATCH_AUDIT_REPORT] Starting monthly report generation");

            File reportFile = auditReportService.generateMonthlyReport();

            log.info("[BATCH_AUDIT_REPORT] Report generation completed - file={}",
                    reportFile.getAbsolutePath());

            return RepeatStatus.FINISHED;
        };
    }
}
