package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.Experience;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.repository.ArticleRepository;
import com.emmanuelgabe.portfolio.repository.ExperienceRepository;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.service.DocumentIngestionService;
import com.emmanuelgabe.portfolio.service.MarkdownService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Elasticsearch-backed implementation of {@link DocumentIngestionService}.
 * Converts portfolio entities into vector embeddings stored via Spring AI VectorStore.
 * Only active when both AI and Elasticsearch are enabled.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"ai.enabled", "elasticsearch.enabled"}, havingValue = "true")
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

    private static final String TYPE_ARTICLE = "article";
    private static final String TYPE_PROJECT = "project";
    private static final String TYPE_EXPERIENCE = "experience";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final VectorStore vectorStore;
    private final ArticleRepository articleRepository;
    private final ProjectRepository projectRepository;
    private final ExperienceRepository experienceRepository;
    private final MarkdownService markdownService;

    // ========== Single entity operations ==========

    @Override
    @Transactional(readOnly = true)
    public void ingestArticle(Article article) {
        Long id = article.getId();
        log.debug("[EMBEDDING_INGEST] Ingesting article - id={}", id);
        try {
            // Re-fetch with tags to avoid LazyInitializationException (called AFTER_COMMIT)
            Article fresh = articleRepository.findByIdWithTags(id).orElse(null);
            if (fresh == null || !fresh.isPublished()) {
                removeArticle(id);
                return;
            }
            vectorStore.add(List.of(toDocument(fresh)));
            log.info("[EMBEDDING_INGEST] Article ingested - id={}", id);
        } catch (Exception e) {
            log.error("[EMBEDDING_INGEST] Failed to ingest article - id={}, error={}",
                    id, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void ingestProject(Project project) {
        Long id = project.getId();
        log.debug("[EMBEDDING_INGEST] Ingesting project - id={}", id);
        try {
            // Re-fetch with tags to avoid LazyInitializationException (called AFTER_COMMIT)
            Project fresh = projectRepository.findByIdWithTags(id).orElse(null);
            if (fresh == null) {
                removeProject(id);
                return;
            }
            vectorStore.add(List.of(toDocument(fresh)));
            log.info("[EMBEDDING_INGEST] Project ingested - id={}", id);
        } catch (Exception e) {
            log.error("[EMBEDDING_INGEST] Failed to ingest project - id={}, error={}",
                    id, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void ingestExperience(Experience experience) {
        Long id = experience.getId();
        log.debug("[EMBEDDING_INGEST] Ingesting experience - id={}", id);
        try {
            Experience fresh = experienceRepository.findById(id).orElse(null);
            if (fresh == null) {
                removeExperience(id);
                return;
            }
            vectorStore.add(List.of(toDocument(fresh)));
            log.info("[EMBEDDING_INGEST] Experience ingested - id={}", id);
        } catch (Exception e) {
            log.error("[EMBEDDING_INGEST] Failed to ingest experience - id={}, error={}",
                    id, e.getMessage(), e);
        }
    }

    // ========== Remove operations ==========

    @Override
    public void removeArticle(Long articleId) {
        log.debug("[EMBEDDING_REMOVE] Removing article embedding - id={}", articleId);
        try {
            vectorStore.delete(List.of(buildDocumentId(TYPE_ARTICLE, articleId)));
            log.info("[EMBEDDING_REMOVE] Article embedding removed - id={}", articleId);
        } catch (Exception e) {
            log.error("[EMBEDDING_REMOVE] Failed to remove article embedding - id={}, error={}",
                    articleId, e.getMessage(), e);
        }
    }

    @Override
    public void removeProject(Long projectId) {
        log.debug("[EMBEDDING_REMOVE] Removing project embedding - id={}", projectId);
        try {
            vectorStore.delete(List.of(buildDocumentId(TYPE_PROJECT, projectId)));
            log.info("[EMBEDDING_REMOVE] Project embedding removed - id={}", projectId);
        } catch (Exception e) {
            log.error("[EMBEDDING_REMOVE] Failed to remove project embedding - id={}, error={}",
                    projectId, e.getMessage(), e);
        }
    }

    @Override
    public void removeExperience(Long experienceId) {
        log.debug("[EMBEDDING_REMOVE] Removing experience embedding - id={}", experienceId);
        try {
            vectorStore.delete(List.of(buildDocumentId(TYPE_EXPERIENCE, experienceId)));
            log.info("[EMBEDDING_REMOVE] Experience embedding removed - id={}", experienceId);
        } catch (Exception e) {
            log.error("[EMBEDDING_REMOVE] Failed to remove experience embedding - id={}, error={}",
                    experienceId, e.getMessage(), e);
        }
    }

    // ========== Bulk operations ==========

    @Override
    @Transactional(readOnly = true)
    public int ingestAll() {
        log.info("[EMBEDDING_INGEST_ALL] Starting full embedding ingestion");

        List<Document> documents = new ArrayList<>();

        List<Article> articles = articleRepository.findPublishedWithTags(LocalDateTime.now());
        for (Article article : articles) {
            documents.add(toDocument(article));
        }
        log.info("[EMBEDDING_INGEST_ALL] Prepared {} published article documents", articles.size());

        List<Project> projects = projectRepository.findAllWithTags();
        for (Project project : projects) {
            documents.add(toDocument(project));
        }
        log.info("[EMBEDDING_INGEST_ALL] Prepared {} project documents", projects.size());

        List<Experience> experiences = experienceRepository.findAll();
        for (Experience experience : experiences) {
            documents.add(toDocument(experience));
        }
        log.info("[EMBEDDING_INGEST_ALL] Prepared {} experience documents", experiences.size());

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }

        log.info("[EMBEDDING_INGEST_ALL] Full ingestion completed - totalIngested={}", documents.size());
        return documents.size();
    }

    // ========== Document conversion ==========

    private Document toDocument(Article article) {
        String textContent = buildArticleText(article);
        Map<String, Object> metadata = Map.of(
                "type", TYPE_ARTICLE,
                "entityId", article.getId().toString(),
                "title", article.getTitle(),
                "slug", article.getSlug());
        return new Document(buildDocumentId(TYPE_ARTICLE, article.getId()), textContent, metadata);
    }

    private Document toDocument(Project project) {
        String textContent = buildProjectText(project);
        Map<String, Object> metadata = Map.of(
                "type", TYPE_PROJECT,
                "entityId", project.getId().toString(),
                "title", project.getTitle());
        return new Document(buildDocumentId(TYPE_PROJECT, project.getId()), textContent, metadata);
    }

    private Document toDocument(Experience experience) {
        String textContent = buildExperienceText(experience);
        String title = experience.getRole() != null ? experience.getRole()
                : experience.getCompany() != null ? experience.getCompany()
                : "Experience " + experience.getId();
        Map<String, Object> metadata = Map.of(
                "type", TYPE_EXPERIENCE,
                "entityId", experience.getId().toString(),
                "title", title);
        return new Document(buildDocumentId(TYPE_EXPERIENCE, experience.getId()), textContent, metadata);
    }

    // ========== Text building ==========

    private String buildArticleText(Article article) {
        StringBuilder sb = new StringBuilder();
        sb.append("Article: ").append(article.getTitle()).append("\n");
        if (article.getExcerpt() != null && !article.getExcerpt().isBlank()) {
            sb.append(article.getExcerpt()).append("\n");
        }
        sb.append(stripMarkdown(article.getContent()));
        String tags = formatTags(article.getTags());
        if (!tags.isEmpty()) {
            sb.append("\nTags: ").append(tags);
        }
        return sb.toString();
    }

    private String buildProjectText(Project project) {
        StringBuilder sb = new StringBuilder();
        sb.append("Project: ").append(project.getTitle()).append("\n");
        sb.append(stripMarkdown(project.getDescription()));
        if (project.getTechStack() != null && !project.getTechStack().isBlank()) {
            sb.append("\nTech Stack: ").append(project.getTechStack());
        }
        String tags = formatTags(project.getTags());
        if (!tags.isEmpty()) {
            sb.append("\nTags: ").append(tags);
        }
        return sb.toString();
    }

    private String buildExperienceText(Experience experience) {
        StringBuilder sb = new StringBuilder();
        if (experience.getType() != null) {
            sb.append(experience.getType().name()).append(": ");
        }
        if (experience.getRole() != null) {
            sb.append(experience.getRole());
        }
        if (experience.getCompany() != null) {
            sb.append(" at ").append(experience.getCompany());
        }
        sb.append("\n");
        if (experience.getStartDate() != null) {
            sb.append("Period: ").append(experience.getStartDate().format(DATE_FORMATTER));
            sb.append(" - ");
            sb.append(experience.getEndDate() != null
                    ? experience.getEndDate().format(DATE_FORMATTER) : "Present");
            sb.append("\n");
        }
        sb.append(experience.getDescription());
        return sb.toString();
    }

    // ========== Utility ==========

    private String buildDocumentId(String type, Long entityId) {
        return type + "-" + entityId;
    }

    private String formatTags(Set<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return tags.stream()
                .map(Tag::getName)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    /**
     * Converts Markdown to plain text for embedding quality.
     * Renders Markdown to HTML via flexmark, then extracts text content via Jsoup.
     */
    private String stripMarkdown(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String html = markdownService.renderToHtml(content);
        return Jsoup.parse(html).text().trim();
    }
}
