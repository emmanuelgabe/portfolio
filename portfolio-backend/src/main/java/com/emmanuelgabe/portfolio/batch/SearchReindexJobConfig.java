package com.emmanuelgabe.portfolio.batch;

import com.emmanuelgabe.portfolio.service.SearchService;
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

/**
 * Spring Batch job configuration for rebuilding Elasticsearch search indices.
 * Runs weekly (Sunday at 3:00 AM) to ensure search index consistency with database.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.enabled", havingValue = "true", matchIfMissing = false)
public class SearchReindexJobConfig {

    private final SearchService searchService;

    @Bean
    public Job searchReindexJob(JobRepository jobRepository, Step searchReindexStep) {
        return new JobBuilder("searchReindexJob", jobRepository)
                .start(searchReindexStep)
                .build();
    }

    @Bean
    public Step searchReindexStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager) {
        return new StepBuilder("searchReindexStep", jobRepository)
                .tasklet(searchReindexTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet searchReindexTasklet() {
        return (contribution, chunkContext) -> {
            log.info("[REINDEX_JOB] Starting search reindex job");

            try {
                int totalIndexed = searchService.reindexAll();

                log.info("[REINDEX_JOB] Search reindex completed - totalIndexed={}", totalIndexed);
                contribution.incrementWriteCount(totalIndexed);

            } catch (Exception e) {
                log.error("[REINDEX_JOB] Search reindex failed - error={}", e.getMessage(), e);
                throw e;
            }

            return RepeatStatus.FINISHED;
        };
    }
}
