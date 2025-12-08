package com.emmanuelgabe.portfolio.batch;

import com.emmanuelgabe.portfolio.service.SitemapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SitemapGenerationJobConfig.
 */
@ExtendWith(MockitoExtension.class)
class SitemapGenerationJobConfigTest {

    @Mock
    private SitemapService sitemapService;

    @Mock
    private StepContribution stepContribution;

    @Mock
    private ChunkContext chunkContext;

    private SitemapGenerationJobConfig jobConfig;

    @BeforeEach
    void setUp() {
        jobConfig = new SitemapGenerationJobConfig(sitemapService);
    }

    // ========== sitemapGenerationTasklet Tests ==========

    @Test
    void should_generateSitemap_when_taskletExecuted() throws Exception {
        // Arrange
        when(sitemapService.writeSitemapToFile()).thenReturn("/path/to/sitemap.xml");

        Tasklet tasklet = jobConfig.sitemapGenerationTasklet();

        // Act
        RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

        // Assert
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(sitemapService).writeSitemapToFile();
    }

    @Test
    void should_incrementWriteCount_when_sitemapGenerated() throws Exception {
        // Arrange
        when(sitemapService.writeSitemapToFile()).thenReturn("/path/to/sitemap.xml");

        Tasklet tasklet = jobConfig.sitemapGenerationTasklet();

        // Act
        tasklet.execute(stepContribution, chunkContext);

        // Assert
        verify(stepContribution).incrementWriteCount(1);
    }

    @Test
    void should_returnFinished_when_sitemapGenerated() throws Exception {
        // Arrange
        when(sitemapService.writeSitemapToFile()).thenReturn("/output/sitemap.xml");

        Tasklet tasklet = jobConfig.sitemapGenerationTasklet();

        // Act
        RepeatStatus status = tasklet.execute(stepContribution, chunkContext);

        // Assert
        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    }
}
