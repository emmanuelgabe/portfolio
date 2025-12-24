package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.search.ArticleSearchResult;
import com.emmanuelgabe.portfolio.dto.search.ExperienceSearchResult;
import com.emmanuelgabe.portfolio.dto.search.ProjectSearchResult;
import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.Experience;
import com.emmanuelgabe.portfolio.entity.Project;

import java.util.List;

/**
 * Service interface for full-text search functionality.
 * Provides search capabilities for Articles, Projects, and Experiences.
 * Implementations include Elasticsearch and JPA fallback.
 */
public interface SearchService {

    // ========== Search operations ==========

    /**
     * Search articles by query string.
     *
     * @param query the search query
     * @return list of matching article search results
     */
    List<ArticleSearchResult> searchArticles(String query);

    /**
     * Search projects by query string.
     *
     * @param query the search query
     * @return list of matching project search results
     */
    List<ProjectSearchResult> searchProjects(String query);

    /**
     * Search experiences by query string.
     *
     * @param query the search query
     * @return list of matching experience search results
     */
    List<ExperienceSearchResult> searchExperiences(String query);

    // ========== Indexing operations ==========

    /**
     * Index an article document.
     *
     * @param article the article entity to index
     */
    void indexArticle(Article article);

    /**
     * Index a project document.
     *
     * @param project the project entity to index
     */
    void indexProject(Project project);

    /**
     * Index an experience document.
     *
     * @param experience the experience entity to index
     */
    void indexExperience(Experience experience);

    // ========== Remove operations ==========

    /**
     * Remove an article from the search index.
     *
     * @param articleId the article ID to remove
     */
    void removeArticle(Long articleId);

    /**
     * Remove a project from the search index.
     *
     * @param projectId the project ID to remove
     */
    void removeProject(Long projectId);

    /**
     * Remove an experience from the search index.
     *
     * @param experienceId the experience ID to remove
     */
    void removeExperience(Long experienceId);

    // ========== Bulk operations ==========

    /**
     * Rebuild all search indices from database.
     *
     * @return total number of documents indexed
     */
    int reindexAll();
}
