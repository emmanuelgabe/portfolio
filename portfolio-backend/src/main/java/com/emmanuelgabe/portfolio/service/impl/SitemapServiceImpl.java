package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.repository.ArticleRepository;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.service.SitemapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementation of SitemapService.
 * Generates sitemap.xml with all public pages, projects, and published articles.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SitemapServiceImpl implements SitemapService {

    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    private static final String URLSET_OPEN = "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n";
    private static final String URLSET_CLOSE = "</urlset>\n";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ProjectRepository projectRepository;
    private final ArticleRepository articleRepository;

    @Value("${sitemap.base-url:https://emmanuelgabe.com}")
    private String baseUrl;

    @Value("${sitemap.output-path:sitemap.xml}")
    private String outputPath;

    private LocalDateTime lastGeneratedAt;

    @Override
    public String generateSitemap() {
        log.info("[SITEMAP] Generating sitemap - baseUrl={}", baseUrl);

        StringBuilder xml = new StringBuilder();
        xml.append(XML_HEADER);
        xml.append(URLSET_OPEN);

        // Static pages
        appendUrl(xml, "", "weekly", "1.0", null);
        appendUrl(xml, "/blog", "weekly", "0.8", null);
        appendUrl(xml, "/contact", "monthly", "0.5", null);

        // Projects
        List<Project> projects = projectRepository.findAll();
        log.debug("[SITEMAP] Adding {} projects", projects.size());
        for (Project project : projects) {
            String lastMod = project.getUpdatedAt() != null
                    ? project.getUpdatedAt().format(DATE_FORMATTER)
                    : null;
            appendUrl(xml, "/projects/" + project.getId(), "monthly", "0.6", lastMod);
        }

        // Published articles
        List<Article> articles = articleRepository
                .findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(LocalDateTime.now());
        log.debug("[SITEMAP] Adding {} published articles", articles.size());
        for (Article article : articles) {
            String lastMod = article.getUpdatedAt() != null
                    ? article.getUpdatedAt().format(DATE_FORMATTER)
                    : article.getPublishedAt().format(DATE_FORMATTER);
            appendUrl(xml, "/blog/" + article.getSlug(), "monthly", "0.7", lastMod);
        }

        xml.append(URLSET_CLOSE);

        lastGeneratedAt = LocalDateTime.now();
        log.info("[SITEMAP] Sitemap generated - projectCount={}, articleCount={}, totalUrls={}",
                projects.size(), articles.size(), 3 + projects.size() + articles.size());

        return xml.toString();
    }

    @Override
    public String writeSitemapToFile() {
        String sitemapContent = generateSitemap();

        try {
            Path path = Paths.get(outputPath);

            // Create parent directories if they don't exist
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            Files.writeString(path, sitemapContent, StandardCharsets.UTF_8);
            log.info("[SITEMAP] Sitemap written to file - path={}", path.toAbsolutePath());

            return path.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("[SITEMAP] Failed to write sitemap to file - path={}, error={}",
                    outputPath, e.getMessage(), e);
            throw new RuntimeException("Failed to write sitemap file", e);
        }
    }

    @Override
    public LocalDateTime getLastGeneratedAt() {
        return lastGeneratedAt;
    }

    private void appendUrl(StringBuilder xml, String path, String changeFreq,
                           String priority, String lastMod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append(path).append("</loc>\n");
        if (lastMod != null) {
            xml.append("    <lastmod>").append(lastMod).append("</lastmod>\n");
        }
        xml.append("    <changefreq>").append(changeFreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }
}
