package com.emmanuelgabe.portfolio.batch;

import com.emmanuelgabe.portfolio.entity.DailyStats;
import com.emmanuelgabe.portfolio.repository.ArticleImageRepository;
import com.emmanuelgabe.portfolio.repository.ArticleRepository;
import com.emmanuelgabe.portfolio.repository.AuditLogRepository;
import com.emmanuelgabe.portfolio.repository.DailyStatsRepository;
import com.emmanuelgabe.portfolio.repository.ExperienceRepository;
import com.emmanuelgabe.portfolio.repository.ProjectImageRepository;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.repository.SkillRepository;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import com.emmanuelgabe.portfolio.service.VisitorTrackingService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Spring Batch job configuration for aggregating daily portfolio statistics.
 * Runs daily to collect and store metrics from all entities.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.enabled", havingValue = "true", matchIfMissing = false)
public class StatsAggregationJobConfig {

    private final DailyStatsRepository dailyStatsRepository;
    private final ProjectRepository projectRepository;
    private final ArticleRepository articleRepository;
    private final SkillRepository skillRepository;
    private final ExperienceRepository experienceRepository;
    private final TagRepository tagRepository;
    private final ProjectImageRepository projectImageRepository;
    private final ArticleImageRepository articleImageRepository;
    private final AuditLogRepository auditLogRepository;
    private final VisitorTrackingService visitorTrackingService;

    @Bean
    public Job statsAggregationJob(JobRepository jobRepository, Step statsAggregationStep) {
        return new JobBuilder("statsAggregationJob", jobRepository)
                .start(statsAggregationStep)
                .build();
    }

    @Bean
    public Step statsAggregationStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager) {
        return new StepBuilder("statsAggregationStep", jobRepository)
                .tasklet(statsAggregationTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet statsAggregationTasklet() {
        return (contribution, chunkContext) -> {
            LocalDate yesterday = LocalDate.now().minusDays(1);

            log.info("[BATCH_STATS] Starting stats aggregation - date={}", yesterday);

            // Check if stats already exist for this date
            if (dailyStatsRepository.existsByStatsDate(yesterday)) {
                log.info("[BATCH_STATS] Stats already exist for date={}, skipping", yesterday);
                return RepeatStatus.FINISHED;
            }

            // Aggregate all statistics
            DailyStats stats = new DailyStats(yesterday);

            // Entity counts
            stats.setTotalProjects(projectRepository.count());
            stats.setTotalArticles(articleRepository.count());
            stats.setTotalSkills(skillRepository.count());
            stats.setTotalExperiences(experienceRepository.count());
            stats.setTotalTags(tagRepository.count());

            // Published vs draft articles
            LocalDateTime now = LocalDateTime.now();
            long publishedCount = articleRepository
                    .findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(now)
                    .size();
            stats.setPublishedArticles(publishedCount);
            stats.setDraftArticles(stats.getTotalArticles() - publishedCount);

            // Image counts
            stats.setTotalProjectImages(projectImageRepository.count());
            stats.setTotalArticleImages(articleImageRepository.count());

            // Unique visitors for the day (from Redis tracking)
            long uniqueVisitors = visitorTrackingService.getDailyUniqueVisitorsCount(yesterday);
            stats.setUniqueVisitors(uniqueVisitors);

            // Clear Redis set after aggregation to free memory
            visitorTrackingService.clearDailyUniqueVisitors(yesterday);

            // Audit events since start of yesterday
            LocalDateTime startOfDay = yesterday.atStartOfDay();
            Object[] auditStats = auditLogRepository.countTotalAndFailedSince(startOfDay);
            if (auditStats != null && auditStats[0] != null) {
                stats.setAuditEventsCount(((Number) auditStats[0]).longValue());
                stats.setFailedAuditEvents(auditStats[1] != null
                        ? ((Number) auditStats[1]).longValue() : 0L);
            } else {
                stats.setAuditEventsCount(0L);
                stats.setFailedAuditEvents(0L);
            }

            // Contact submissions are not persisted, set to 0
            // Could be enhanced by adding a contact_submissions table
            stats.setContactSubmissions(0L);

            // Save the stats
            dailyStatsRepository.save(stats);

            log.info("[BATCH_STATS] Stats aggregation completed - date={}, projects={}, "
                            + "articles={}, skills={}, experiences={}, tags={}, "
                            + "projectImages={}, articleImages={}, uniqueVisitors={}, auditEvents={}",
                    yesterday,
                    stats.getTotalProjects(),
                    stats.getTotalArticles(),
                    stats.getTotalSkills(),
                    stats.getTotalExperiences(),
                    stats.getTotalTags(),
                    stats.getTotalProjectImages(),
                    stats.getTotalArticleImages(),
                    stats.getUniqueVisitors(),
                    stats.getAuditEventsCount());

            contribution.incrementWriteCount(1);
            return RepeatStatus.FINISHED;
        };
    }
}
