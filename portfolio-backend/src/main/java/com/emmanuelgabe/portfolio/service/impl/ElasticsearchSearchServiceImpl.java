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
import com.emmanuelgabe.portfolio.search.document.ArticleDocument;
import com.emmanuelgabe.portfolio.search.document.ExperienceDocument;
import com.emmanuelgabe.portfolio.search.document.ProjectDocument;
import com.emmanuelgabe.portfolio.search.mapper.SearchDocumentMapper;
import com.emmanuelgabe.portfolio.search.repository.ArticleSearchRepository;
import com.emmanuelgabe.portfolio.search.repository.ExperienceSearchRepository;
import com.emmanuelgabe.portfolio.search.repository.ProjectSearchRepository;
import com.emmanuelgabe.portfolio.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Elasticsearch implementation of SearchService.
 * Provides full-text search using Elasticsearch indices.
 * Only active when elasticsearch.enabled=true.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ElasticsearchSearchServiceImpl implements SearchService {

    private final ArticleSearchRepository articleSearchRepository;
    private final ProjectSearchRepository projectSearchRepository;
    private final ExperienceSearchRepository experienceSearchRepository;
    private final SearchDocumentMapper searchDocumentMapper;
    private final ArticleRepository articleRepository;
    private final ProjectRepository projectRepository;
    private final ExperienceRepository experienceRepository;

    // ========== Search operations ==========

    @Override
    public List<ArticleSearchResult> searchArticles(String query) {
        log.debug("[SEARCH_ARTICLES] Searching articles - query={}", query);
        try {
            List<ArticleDocument> documents = articleSearchRepository.searchByQuery(query);
            List<ArticleSearchResult> results = documents.stream()
                    .map(searchDocumentMapper::toSearchResult)
                    .toList();
            log.info("[SEARCH_ARTICLES] Found {} results - query={}", results.size(), query);
            return results;
        } catch (Exception e) {
            log.error("[SEARCH_ARTICLES] Search failed - query={}, error={}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<ProjectSearchResult> searchProjects(String query) {
        log.debug("[SEARCH_PROJECTS] Searching projects - query={}", query);
        try {
            List<ProjectDocument> documents = projectSearchRepository.searchByQuery(query);
            List<ProjectSearchResult> results = documents.stream()
                    .map(searchDocumentMapper::toSearchResult)
                    .toList();
            log.info("[SEARCH_PROJECTS] Found {} results - query={}", results.size(), query);
            return results;
        } catch (Exception e) {
            log.error("[SEARCH_PROJECTS] Search failed - query={}, error={}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<ExperienceSearchResult> searchExperiences(String query) {
        log.debug("[SEARCH_EXPERIENCES] Searching experiences - query={}", query);
        try {
            List<ExperienceDocument> documents = experienceSearchRepository.searchByQuery(query);
            List<ExperienceSearchResult> results = documents.stream()
                    .map(searchDocumentMapper::toSearchResult)
                    .toList();
            log.info("[SEARCH_EXPERIENCES] Found {} results - query={}", results.size(), query);
            return results;
        } catch (Exception e) {
            log.error("[SEARCH_EXPERIENCES] Search failed - query={}, error={}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ========== Indexing operations ==========

    @Override
    public void indexArticle(Article article) {
        log.debug("[INDEX_ARTICLE] Indexing article - id={}, title={}", article.getId(), article.getTitle());
        try {
            ArticleDocument document = searchDocumentMapper.toDocument(article);
            articleSearchRepository.save(document);
            log.info("[INDEX_ARTICLE] Article indexed - id={}", article.getId());
        } catch (Exception e) {
            log.error("[INDEX_ARTICLE] Indexing failed - id={}, error={}", article.getId(), e.getMessage());
        }
    }

    @Override
    public void indexProject(Project project) {
        log.debug("[INDEX_PROJECT] Indexing project - id={}, title={}", project.getId(), project.getTitle());
        try {
            ProjectDocument document = searchDocumentMapper.toDocument(project);
            projectSearchRepository.save(document);
            log.info("[INDEX_PROJECT] Project indexed - id={}", project.getId());
        } catch (Exception e) {
            log.error("[INDEX_PROJECT] Indexing failed - id={}, error={}", project.getId(), e.getMessage());
        }
    }

    @Override
    public void indexExperience(Experience experience) {
        log.debug("[INDEX_EXPERIENCE] Indexing experience - id={}, company={}",
                experience.getId(), experience.getCompany());
        try {
            ExperienceDocument document = searchDocumentMapper.toDocument(experience);
            experienceSearchRepository.save(document);
            log.info("[INDEX_EXPERIENCE] Experience indexed - id={}", experience.getId());
        } catch (Exception e) {
            log.error("[INDEX_EXPERIENCE] Indexing failed - id={}, error={}", experience.getId(), e.getMessage());
        }
    }

    // ========== Remove operations ==========

    @Override
    public void removeArticle(Long articleId) {
        log.debug("[REMOVE_ARTICLE] Removing article from index - id={}", articleId);
        try {
            articleSearchRepository.deleteById(articleId);
            log.info("[REMOVE_ARTICLE] Article removed from index - id={}", articleId);
        } catch (Exception e) {
            log.error("[REMOVE_ARTICLE] Removal failed - id={}, error={}", articleId, e.getMessage());
        }
    }

    @Override
    public void removeProject(Long projectId) {
        log.debug("[REMOVE_PROJECT] Removing project from index - id={}", projectId);
        try {
            projectSearchRepository.deleteById(projectId);
            log.info("[REMOVE_PROJECT] Project removed from index - id={}", projectId);
        } catch (Exception e) {
            log.error("[REMOVE_PROJECT] Removal failed - id={}, error={}", projectId, e.getMessage());
        }
    }

    @Override
    public void removeExperience(Long experienceId) {
        log.debug("[REMOVE_EXPERIENCE] Removing experience from index - id={}", experienceId);
        try {
            experienceSearchRepository.deleteById(experienceId);
            log.info("[REMOVE_EXPERIENCE] Experience removed from index - id={}", experienceId);
        } catch (Exception e) {
            log.error("[REMOVE_EXPERIENCE] Removal failed - id={}, error={}", experienceId, e.getMessage());
        }
    }

    // ========== Bulk operations ==========

    @Override
    @Transactional(readOnly = true)
    public int reindexAll() {
        log.info("[REINDEX_ALL] Starting full reindex");
        int totalIndexed = 0;

        try {
            // Clear existing indices
            articleSearchRepository.deleteAll();
            projectSearchRepository.deleteAll();
            experienceSearchRepository.deleteAll();
            log.info("[REINDEX_ALL] Cleared existing indices");

            // Reindex articles
            List<Article> articles = articleRepository.findAll();
            for (Article article : articles) {
                indexArticle(article);
                totalIndexed++;
            }
            log.info("[REINDEX_ALL] Indexed {} articles", articles.size());

            // Reindex projects
            List<Project> projects = projectRepository.findAll();
            for (Project project : projects) {
                indexProject(project);
                totalIndexed++;
            }
            log.info("[REINDEX_ALL] Indexed {} projects", projects.size());

            // Reindex experiences
            List<Experience> experiences = experienceRepository.findAll();
            for (Experience experience : experiences) {
                indexExperience(experience);
                totalIndexed++;
            }
            log.info("[REINDEX_ALL] Indexed {} experiences", experiences.size());

            log.info("[REINDEX_ALL] Full reindex completed - totalIndexed={}", totalIndexed);
        } catch (Exception e) {
            log.error("[REINDEX_ALL] Reindex failed - error={}", e.getMessage());
        }

        return totalIndexed;
    }
}
