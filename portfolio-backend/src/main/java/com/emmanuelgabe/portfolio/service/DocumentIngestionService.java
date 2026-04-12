package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.Experience;
import com.emmanuelgabe.portfolio.entity.Project;

/**
 * Service for ingesting portfolio content as vector embeddings into the vector store.
 * Converts entities (articles, projects, experiences) into Spring AI {@link org.springframework.ai.document.Document}
 * objects and stores them in Elasticsearch for RAG-based similarity search.
 */
public interface DocumentIngestionService {

    // ========== Single entity operations ==========

    /**
     * Ingest an article as a vector embedding.
     *
     * @param article the article to ingest
     */
    void ingestArticle(Article article);

    /**
     * Ingest a project as a vector embedding.
     *
     * @param project the project to ingest
     */
    void ingestProject(Project project);

    /**
     * Ingest an experience as a vector embedding.
     *
     * @param experience the experience to ingest
     */
    void ingestExperience(Experience experience);

    // ========== Remove operations ==========

    /**
     * Remove an article embedding from the vector store.
     *
     * @param articleId the article ID to remove
     */
    void removeArticle(Long articleId);

    /**
     * Remove a project embedding from the vector store.
     *
     * @param projectId the project ID to remove
     */
    void removeProject(Long projectId);

    /**
     * Remove an experience embedding from the vector store.
     *
     * @param experienceId the experience ID to remove
     */
    void removeExperience(Long experienceId);

    // ========== Bulk operations ==========

    /**
     * Ingest all portfolio content (articles, projects, experiences) into the vector store.
     * Existing documents with the same IDs are upserted.
     *
     * @return total number of documents ingested
     */
    int ingestAll();
}
