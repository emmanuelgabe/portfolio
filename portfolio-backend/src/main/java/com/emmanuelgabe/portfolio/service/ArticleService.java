package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.ReorderRequest;
import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.dto.article.CreateArticleRequest;
import com.emmanuelgabe.portfolio.dto.article.UpdateArticleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

/**
 * Service interface for managing blog articles.
 */
public interface ArticleService {

    /**
     * Retrieves articles by IDs (batch).
     *
     * @param ids collection of article IDs
     * @return list of articles
     */
    List<ArticleResponse> getArticlesByIds(Collection<Long> ids);

    /**
     * Retrieves all published articles visible to the public.
     *
     * @return list of published articles
     */
    List<ArticleResponse> getAllPublished();

    /**
     * Retrieves published articles with pagination.
     *
     * @param pageable pagination information (page number, size, sort)
     * @return page of published articles
     */
    Page<ArticleResponse> getAllPublishedPaginated(Pageable pageable);

    /**
     * Retrieves a published article by its slug.
     *
     * @param slug the article slug
     * @return the article response
     * @throws ResourceNotFoundException if article not found or not published
     */
    ArticleResponse getBySlug(String slug);

    /**
     * Retrieves an article by ID (admin only, includes drafts).
     *
     * @param id the article ID
     * @return the article response
     */
    ArticleResponse getById(Long id);

    /**
     * Retrieves all articles including drafts (admin only).
     *
     * @return list of all articles
     */
    List<ArticleResponse> getAllArticles();

    /**
     * Retrieves all articles with pagination (admin only).
     *
     * @param pageable pagination information (page number, size, sort)
     * @return page of all articles
     */
    Page<ArticleResponse> getAllArticlesPaginated(Pageable pageable);

    /**
     * Creates a new article.
     *
     * @param request the create article request
     * @param username the author username
     * @return the created article response
     */
    ArticleResponse createArticle(CreateArticleRequest request, String username);

    /**
     * Updates an existing article.
     *
     * @param id the article ID
     * @param request the update article request
     * @return the updated article response
     */
    ArticleResponse updateArticle(Long id, UpdateArticleRequest request);

    /**
     * Deletes an article.
     *
     * @param id the article ID
     */
    void deleteArticle(Long id);

    /**
     * Publishes an article (sets draft to false and records publication timestamp).
     *
     * @param id the article ID
     * @return the published article response
     */
    ArticleResponse publishArticle(Long id);

    /**
     * Unpublishes an article (sets draft to true).
     *
     * @param id the article ID
     * @return the unpublished article response
     */
    ArticleResponse unpublishArticle(Long id);

    /**
     * Adds an image to an article with async processing.
     *
     * @param articleId the article ID
     * @param file the image file to upload
     * @return the article image response with ID and status
     */
    com.emmanuelgabe.portfolio.dto.article.ArticleImageResponse addImageToArticle(Long articleId, org.springframework.web.multipart.MultipartFile file);

    /**
     * Removes an image from an article.
     *
     * @param articleId the article ID
     * @param imageId the image ID
     */
    void removeImageFromArticle(Long articleId, Long imageId);

    /**
     * Reorder articles by updating their display order.
     *
     * @param request Reorder request with ordered IDs
     */
    void reorderArticles(ReorderRequest request);
}
