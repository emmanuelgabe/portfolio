package com.emmanuelgabe.portfolio.batch;

import com.emmanuelgabe.portfolio.service.DocumentIngestionService;
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
 * Spring Batch job for full vector embedding ingestion.
 * Indexes all portfolio content (articles, projects, experiences) into the vector store.
 * Used for initial indexation and periodic consistency re-sync.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"batch.enabled", "ai.enabled", "elasticsearch.enabled"}, havingValue = "true", matchIfMissing = false)
public class EmbeddingIngestionJobConfig {

    private final DocumentIngestionService documentIngestionService;

    @Bean
    public Job embeddingIngestionJob(JobRepository jobRepository, Step embeddingIngestionStep) {
        return new JobBuilder("embeddingIngestionJob", jobRepository)
                .start(embeddingIngestionStep)
                .build();
    }

    @Bean
    public Step embeddingIngestionStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return new StepBuilder("embeddingIngestionStep", jobRepository)
                .tasklet(embeddingIngestionTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet embeddingIngestionTasklet() {
        return (contribution, chunkContext) -> {
            log.info("[EMBEDDING_JOB] Starting embedding ingestion job");

            try {
                int totalIngested = documentIngestionService.ingestAll();

                log.info("[EMBEDDING_JOB] Embedding ingestion completed - totalIngested={}", totalIngested);
                contribution.incrementWriteCount(totalIngested);

            } catch (Exception e) {
                log.error("[EMBEDDING_JOB] Embedding ingestion failed - error={}", e.getMessage(), e);
                throw e;
            }

            return RepeatStatus.FINISHED;
        };
    }
}
