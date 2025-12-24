package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.dto.search.ArticleSearchResult;
import com.emmanuelgabe.portfolio.dto.search.ExperienceSearchResult;
import com.emmanuelgabe.portfolio.dto.search.ProjectSearchResult;
import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.Experience;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.repository.ArticleRepository;
import com.emmanuelgabe.portfolio.repository.ExperienceRepository;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.search.mapper.SearchDocumentMapper;
import com.emmanuelgabe.portfolio.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * JPA fallback implementation of SearchService.
 * Uses LIKE queries when Elasticsearch is disabled.
 * Active when elasticsearch.enabled=false or not set.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
public class JpaFallbackSearchServiceImpl implements SearchService {

    private final ArticleRepository articleRepository;
    private final ProjectRepository projectRepository;
    private final ExperienceRepository experienceRepository;
    private final SearchDocumentMapper searchDocumentMapper;

    // ========== Search operations ==========

    @Override
    public List<ArticleSearchResult> searchArticles(String query) {
        log.debug("[SEARCH_ARTICLES] Fallback JPA search - query={}", query);
        List<Article> articles = articleRepository
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(query, query);
        List<ArticleSearchResult> results = articles.stream()
                .map(searchDocumentMapper::toSearchResult)
                .toList();
        log.info("[SEARCH_ARTICLES] Fallback found {} results - query={}", results.size(), query);
        return results;
    }

    @Override
    public List<ProjectSearchResult> searchProjects(String query) {
        log.debug("[SEARCH_PROJECTS] Fallback JPA search - query={}", query);
        List<Project> projects = projectRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
        List<ProjectSearchResult> results = projects.stream()
                .map(searchDocumentMapper::toSearchResult)
                .toList();
        log.info("[SEARCH_PROJECTS] Fallback found {} results - query={}", results.size(), query);
        return results;
    }

    @Override
    public List<ExperienceSearchResult> searchExperiences(String query) {
        log.debug("[SEARCH_EXPERIENCES] Fallback JPA search - query={}", query);
        List<Experience> experiences = experienceRepository
                .findByCompanyContainingIgnoreCaseOrRoleContainingIgnoreCase(query, query);
        List<ExperienceSearchResult> results = experiences.stream()
                .map(searchDocumentMapper::toSearchResult)
                .toList();
        log.info("[SEARCH_EXPERIENCES] Fallback found {} results - query={}", results.size(), query);
        return results;
    }

    // ========== Indexing operations (no-ops in fallback mode) ==========

    @Override
    public void indexArticle(Article article) {
        log.trace("[INDEX_ARTICLE] Fallback mode - no indexing performed");
    }

    @Override
    public void indexProject(Project project) {
        log.trace("[INDEX_PROJECT] Fallback mode - no indexing performed");
    }

    @Override
    public void indexExperience(Experience experience) {
        log.trace("[INDEX_EXPERIENCE] Fallback mode - no indexing performed");
    }

    // ========== Remove operations (no-ops in fallback mode) ==========

    @Override
    public void removeArticle(Long articleId) {
        log.trace("[REMOVE_ARTICLE] Fallback mode - no removal performed");
    }

    @Override
    public void removeProject(Long projectId) {
        log.trace("[REMOVE_PROJECT] Fallback mode - no removal performed");
    }

    @Override
    public void removeExperience(Long experienceId) {
        log.trace("[REMOVE_EXPERIENCE] Fallback mode - no removal performed");
    }

    // ========== Bulk operations (no-op in fallback mode) ==========

    @Override
    public int reindexAll() {
        log.info("[REINDEX_ALL] Fallback mode - no reindexing performed");
        return 0;
    }
}
