package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Article entity.
 * Provides methods for querying articles with various filters.
 */
@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    /**
     * Find all articles ordered by display order.
     *
     * @return list of all articles sorted by displayOrder
     */
    List<Article> findAllByOrderByDisplayOrderAsc();

    /**
     * Finds an article by its slug.
     *
     * @param slug the article slug
     * @return an Optional containing the article if found
     */
    Optional<Article> findBySlug(String slug);

    /**
     * Finds all published articles (not drafts, with published date before or equal to now).
     * Returns results ordered by publication date descending.
     *
     * @param now the current timestamp
     * @return list of published articles
     */
    List<Article> findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(LocalDateTime now);

    /**
     * Finds all published articles with pagination support.
     * Returns results ordered by publication date descending.
     *
     * @param now the current timestamp
     * @param pageable pagination information
     * @return page of published articles
     */
    @Query("SELECT a FROM Article a WHERE a.draft = false AND a.publishedAt <= :now ORDER BY a.publishedAt DESC")
    Page<Article> findPublished(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Finds all articles ordered by publication date descending.
     *
     * @return list of all articles
     */
    List<Article> findAllByOrderByPublishedAtDesc();

    /**
     * Finds all articles with pagination support (admin only).
     * Returns results ordered by publication date descending.
     *
     * @param pageable pagination information
     * @return page of all articles
     */
    Page<Article> findAllByOrderByPublishedAtDesc(Pageable pageable);

    /**
     * Checks if an article with the given slug already exists.
     *
     * @param slug the article slug
     * @return true if an article with this slug exists
     */
    boolean existsBySlug(String slug);

    /**
     * Search articles by title or content (case-insensitive).
     * Used as fallback when Elasticsearch is disabled.
     *
     * @param title the title search term
     * @param content the content search term
     * @return list of matching articles
     */
    List<Article> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String title, String content);
}
