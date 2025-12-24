package com.emmanuelgabe.portfolio.batch;

import com.emmanuelgabe.portfolio.service.SitemapService;
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
 * Spring Batch job configuration for regenerating the sitemap.xml file.
 * Runs on a schedule to keep the sitemap up to date with new content.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.enabled", havingValue = "true", matchIfMissing = false)
public class SitemapGenerationJobConfig {

    private final SitemapService sitemapService;

    @Bean
    public Job sitemapGenerationJob(JobRepository jobRepository, Step sitemapGenerationStep) {
        return new JobBuilder("sitemapGenerationJob", jobRepository)
                .start(sitemapGenerationStep)
                .build();
    }

    @Bean
    public Step sitemapGenerationStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager) {
        return new StepBuilder("sitemapGenerationStep", jobRepository)
                .tasklet(sitemapGenerationTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet sitemapGenerationTasklet() {
        return (contribution, chunkContext) -> {
            log.info("[BATCH_SITEMAP] Starting sitemap generation");

            String outputPath = sitemapService.writeSitemapToFile();

            log.info("[BATCH_SITEMAP] Sitemap generation completed - outputPath={}", outputPath);

            contribution.incrementWriteCount(1);
            return RepeatStatus.FINISHED;
        };
    }
}
