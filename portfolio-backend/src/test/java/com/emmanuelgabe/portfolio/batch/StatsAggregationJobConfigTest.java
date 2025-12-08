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

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StatsAggregationJobConfig.
 */
@ExtendWith(MockitoExtension.class)
class StatsAggregationJobConfigTest {

    @Mock
    private DailyStatsRepository dailyStatsRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ProjectImageRepository projectImageRepository;

    @Mock
    private ArticleImageRepository articleImageRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private VisitorTrackingService visitorTrackingService;

    @Mock
    private StepContribution stepContribution;

    @Mock
    private ChunkContext chunkContext;

    @Captor
    private ArgumentCaptor<DailyStats> statsCaptor;

    private StatsAggregationJobConfig jobConfig;

    @BeforeEach
    void setUp() {
        jobConfig = new StatsAggregationJobConfig(
                dailyStatsRepository,
                projectRepository,
                articleRepository,
                skillRepository,
                experienceRepository,
                tagRepository,
                projectImageRepository,
                articleImageRepository,
                auditLogRepository,
                visitorTrackingService
        );
    }

    // ========== statsAggregationTasklet Tests ==========

    @Test
    void should_aggregateStats_when_taskletExecuted() throws Exception {
        // Arrange
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(dailyStatsRepository.existsByStatsDate(yesterday)).thenReturn(false);
        when(projectRepository.count()).thenReturn(10L);
        when(articleRepository.count()).thenReturn(5L);
        when(skillRepository.count()).thenReturn(20L);
        when(experienceRepository.count()).thenReturn(8L);
        when(tagRepository.count()).thenReturn(15L);
        when(projectImageRepository.count()).thenReturn(25L);
        when(articleImageRepository.count()).thenReturn(12L);
        when(articleRepository.findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any()))
                .thenReturn(Collections.emptyList());
        when(auditLogRepository.countTotalAndFailedSince(any()))
                .thenReturn(new Object[]{100L, 5L});
        when(visitorTrackingService.getDailyUniqueVisitorsCount(yesterday)).thenReturn(42L);

        Tasklet tasklet = jobConfig.statsAggregationTasklet();

        // Act
        RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

        // Assert
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(dailyStatsRepository).save(statsCaptor.capture());
        verify(visitorTrackingService).clearDailyUniqueVisitors(yesterday);

        DailyStats savedStats = statsCaptor.getValue();
        assertThat(savedStats.getStatsDate()).isEqualTo(yesterday);
        assertThat(savedStats.getTotalProjects()).isEqualTo(10L);
        assertThat(savedStats.getTotalArticles()).isEqualTo(5L);
        assertThat(savedStats.getTotalSkills()).isEqualTo(20L);
        assertThat(savedStats.getTotalExperiences()).isEqualTo(8L);
        assertThat(savedStats.getTotalTags()).isEqualTo(15L);
        assertThat(savedStats.getTotalProjectImages()).isEqualTo(25L);
        assertThat(savedStats.getTotalArticleImages()).isEqualTo(12L);
        assertThat(savedStats.getAuditEventsCount()).isEqualTo(100L);
        assertThat(savedStats.getFailedAuditEvents()).isEqualTo(5L);
        assertThat(savedStats.getUniqueVisitors()).isEqualTo(42L);
    }

    @Test
    void should_skipAggregation_when_statsAlreadyExist() throws Exception {
        // Arrange
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(dailyStatsRepository.existsByStatsDate(yesterday)).thenReturn(true);

        Tasklet tasklet = jobConfig.statsAggregationTasklet();

        // Act
        RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

        // Assert
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(dailyStatsRepository, never()).save(any());
        verify(projectRepository, never()).count();
    }

    @Test
    void should_handleNullAuditStats_when_noAuditEvents() throws Exception {
        // Arrange
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(dailyStatsRepository.existsByStatsDate(yesterday)).thenReturn(false);
        when(projectRepository.count()).thenReturn(5L);
        when(articleRepository.count()).thenReturn(3L);
        when(skillRepository.count()).thenReturn(10L);
        when(experienceRepository.count()).thenReturn(4L);
        when(tagRepository.count()).thenReturn(8L);
        when(projectImageRepository.count()).thenReturn(15L);
        when(articleImageRepository.count()).thenReturn(6L);
        when(articleRepository.findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any()))
                .thenReturn(Collections.emptyList());
        when(auditLogRepository.countTotalAndFailedSince(any()))
                .thenReturn(new Object[]{null, null});
        when(visitorTrackingService.getDailyUniqueVisitorsCount(yesterday)).thenReturn(0L);

        Tasklet tasklet = jobConfig.statsAggregationTasklet();

        // Act
        RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

        // Assert
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(dailyStatsRepository).save(statsCaptor.capture());

        DailyStats savedStats = statsCaptor.getValue();
        assertThat(savedStats.getAuditEventsCount()).isEqualTo(0L);
        assertThat(savedStats.getFailedAuditEvents()).isEqualTo(0L);
    }

    @Test
    void should_incrementWriteCount_when_statsAggregated() throws Exception {
        // Arrange
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(dailyStatsRepository.existsByStatsDate(yesterday)).thenReturn(false);
        when(projectRepository.count()).thenReturn(0L);
        when(articleRepository.count()).thenReturn(0L);
        when(skillRepository.count()).thenReturn(0L);
        when(experienceRepository.count()).thenReturn(0L);
        when(tagRepository.count()).thenReturn(0L);
        when(projectImageRepository.count()).thenReturn(0L);
        when(articleImageRepository.count()).thenReturn(0L);
        when(articleRepository.findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any()))
                .thenReturn(Collections.emptyList());
        when(auditLogRepository.countTotalAndFailedSince(any()))
                .thenReturn(new Object[]{0L, 0L});
        when(visitorTrackingService.getDailyUniqueVisitorsCount(yesterday)).thenReturn(0L);

        Tasklet tasklet = jobConfig.statsAggregationTasklet();

        // Act
        tasklet.execute(stepContribution, chunkContext);

        // Assert
        verify(stepContribution).incrementWriteCount(1);
    }

    @Test
    void should_setContactSubmissionsToZero_when_notTracked() throws Exception {
        // Arrange
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(dailyStatsRepository.existsByStatsDate(yesterday)).thenReturn(false);
        when(projectRepository.count()).thenReturn(1L);
        when(articleRepository.count()).thenReturn(1L);
        when(skillRepository.count()).thenReturn(1L);
        when(experienceRepository.count()).thenReturn(1L);
        when(tagRepository.count()).thenReturn(1L);
        when(projectImageRepository.count()).thenReturn(1L);
        when(articleImageRepository.count()).thenReturn(1L);
        when(articleRepository.findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any()))
                .thenReturn(Collections.emptyList());
        when(auditLogRepository.countTotalAndFailedSince(any()))
                .thenReturn(new Object[]{0L, 0L});
        when(visitorTrackingService.getDailyUniqueVisitorsCount(yesterday)).thenReturn(0L);

        Tasklet tasklet = jobConfig.statsAggregationTasklet();

        // Act
        tasklet.execute(stepContribution, chunkContext);

        // Assert
        verify(dailyStatsRepository).save(statsCaptor.capture());
        DailyStats savedStats = statsCaptor.getValue();
        assertThat(savedStats.getContactSubmissions()).isEqualTo(0L);
    }
}
