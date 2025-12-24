package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.ArticleImage;
import com.emmanuelgabe.portfolio.entity.ImageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository for ArticleImage entity operations.
 */
@Repository
public interface ArticleImageRepository extends JpaRepository<ArticleImage, Long> {

    /**
     * Update the processing status of an article image.
     */
    @Modifying
    @Query("UPDATE ArticleImage ai SET ai.status = :status WHERE ai.id = :imageId")
    int updateStatus(@Param("imageId") Long imageId, @Param("status") ImageStatus status);

    /**
     * Find all article images with a specific status.
     * Used by batch job to find images eligible for reprocessing.
     */
    List<ArticleImage> findByStatus(ImageStatus status);

    /**
     * Count article images by status.
     */
    long countByStatus(ImageStatus status);

    /**
     * Batch load images for multiple articles.
     * Used by DataLoader to prevent N+1 queries.
     */
    List<ArticleImage> findByArticleIdIn(Collection<Long> articleIds);
}
