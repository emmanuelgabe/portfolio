package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.repository.ArticleRepository;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.service.impl.SitemapServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SitemapServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class SitemapServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ArticleRepository articleRepository;

    private SitemapServiceImpl sitemapService;

    @BeforeEach
    void setUp() {
        sitemapService = new SitemapServiceImpl(projectRepository, articleRepository);
        ReflectionTestUtils.setField(sitemapService, "baseUrl", "https://example.com");
        ReflectionTestUtils.setField(sitemapService, "outputPath", "sitemap.xml");
    }

    // ========== generateSitemap Tests ==========

    @Test
    void should_generateValidXml_when_generateSitemapCalled() {
        // Arrange
        when(projectRepository.findAll()).thenReturn(Collections.emptyList());
        when(articleRepository.findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any()))
                .thenReturn(Collections.emptyList());

        // Act
        String sitemap = sitemapService.generateSitemap();

        // Assert
        assertThat(sitemap).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(sitemap).contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        assertThat(sitemap).contains("</urlset>");
    }

    @Test
    void should_includeStaticPages_when_generateSitemapCalled() {
        // Arrange
        when(projectRepository.findAll()).thenReturn(Collections.emptyList());
        when(articleRepository.findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any()))
                .thenReturn(Collections.emptyList());

        // Act
        String sitemap = sitemapService.generateSitemap();

        // Assert
        assertThat(sitemap).contains("<loc>https://example.com</loc>");
        assertThat(sitemap).contains("<loc>https://example.com/blog</loc>");
        assertThat(sitemap).contains("<loc>https://example.com/contact</loc>");
    }

    @Test
    void should_includeProjects_when_projectsExist() {
        // Arrange
        Project project1 = new Project();
        project1.setId(1L);
        project1.setTitle("Project 1");
        project1.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 10, 0));

        Project project2 = new Project();
        project2.setId(2L);
        project2.setTitle("Project 2");

        when(projectRepository.findAll()).thenReturn(Arrays.asList(project1, project2));
        when(articleRepository.findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any()))
                .thenReturn(Collections.emptyList());

        // Act
        String sitemap = sitemapService.generateSitemap();

        // Assert
        assertThat(sitemap).contains("<loc>https://example.com/projects/1</loc>");
        assertThat(sitemap).contains("<loc>https://example.com/projects/2</loc>");
        assertThat(sitemap).contains("<lastmod>2024-01-15</lastmod>");
    }

    @Test
    void should_includePublishedArticles_when_articlesExist() {
        // Arrange
        Article article = new Article();
        article.setId(1L);
        article.setSlug("my-first-article");
        article.setPublishedAt(LocalDateTime.of(2024, 2, 20, 12, 0));
        article.setUpdatedAt(LocalDateTime.of(2024, 3, 1, 14, 0));

        when(projectRepository.findAll()).thenReturn(Collections.emptyList());
        when(articleRepository.findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any()))
                .thenReturn(Collections.singletonList(article));

        // Act
        String sitemap = sitemapService.generateSitemap();

        // Assert
        assertThat(sitemap).contains("<loc>https://example.com/blog/my-first-article</loc>");
        assertThat(sitemap).contains("<lastmod>2024-03-01</lastmod>");
    }

    @Test
    void should_setCorrectPriorities_when_generateSitemapCalled() {
        // Arrange
        when(projectRepository.findAll()).thenReturn(Collections.emptyList());
        when(articleRepository.findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any()))
                .thenReturn(Collections.emptyList());

        // Act
        String sitemap = sitemapService.generateSitemap();

        // Assert
        // Homepage has highest priority
        assertThat(sitemap).contains("<priority>1.0</priority>");
        // Blog list has 0.8
        assertThat(sitemap).contains("<priority>0.8</priority>");
        // Contact has 0.5
        assertThat(sitemap).contains("<priority>0.5</priority>");
    }

    @Test
    void should_updateLastGeneratedAt_when_generateSitemapCalled() {
        // Arrange
        when(projectRepository.findAll()).thenReturn(Collections.emptyList());
        when(articleRepository.findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any()))
                .thenReturn(Collections.emptyList());

        assertThat(sitemapService.getLastGeneratedAt()).isNull();

        // Act
        sitemapService.generateSitemap();

        // Assert
        assertThat(sitemapService.getLastGeneratedAt()).isNotNull();
        assertThat(sitemapService.getLastGeneratedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    // ========== writeSitemapToFile Tests ==========

    @Test
    void should_writeFileToPath_when_writeSitemapToFileCalled(@TempDir Path tempDir) {
        // Arrange
        Path outputFile = tempDir.resolve("sitemap.xml");
        ReflectionTestUtils.setField(sitemapService, "outputPath", outputFile.toString());

        when(projectRepository.findAll()).thenReturn(Collections.emptyList());
        when(articleRepository.findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any()))
                .thenReturn(Collections.emptyList());

        // Act
        String resultPath = sitemapService.writeSitemapToFile();

        // Assert
        assertThat(outputFile).exists();
        assertThat(outputFile).content().startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(resultPath).contains("sitemap.xml");
    }

    @Test
    void should_createParentDirectories_when_writeSitemapToFileCalled(@TempDir Path tempDir) {
        // Arrange
        Path outputFile = tempDir.resolve("subdir/sitemap.xml");
        ReflectionTestUtils.setField(sitemapService, "outputPath", outputFile.toString());

        when(projectRepository.findAll()).thenReturn(Collections.emptyList());
        when(articleRepository.findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any()))
                .thenReturn(Collections.emptyList());

        // Act
        sitemapService.writeSitemapToFile();

        // Assert
        assertThat(outputFile).exists();
        assertThat(outputFile.getParent()).exists();
    }
}
