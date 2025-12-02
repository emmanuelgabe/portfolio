package com.emmanuelgabe.portfolio.entity.listener;

import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.ArticleImage;
import com.emmanuelgabe.portfolio.service.ImageService;
import jakarta.persistence.PreRemove;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * JPA entity listener for Article lifecycle events.
 * Handles cleanup of associated resources before entity deletion.
 */
@Component
@Slf4j
public class ArticleEntityListener {

    private ImageService imageService;

    /**
     * Lazy injection to avoid circular dependency issues during entity initialization.
     *
     * @param imageService service for managing image files
     */
    @Autowired
    public void setImageService(@Lazy ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * Called before an article is removed from the database.
     * Deletes all associated image files from the filesystem.
     *
     * @param article the article being deleted
     */
    @PreRemove
    public void preRemove(Article article) {
        log.info("[ARTICLE_LIFECYCLE] PreRemove triggered - articleId={}, title={}, imageCount={}",
            article.getId(), article.getTitle(), article.getImages().size());

        if (article.getImages().isEmpty()) {
            log.debug("[ARTICLE_LIFECYCLE] No images to cleanup - articleId={}", article.getId());
            return;
        }

        for (ArticleImage image : article.getImages()) {
            try {
                log.debug("[ARTICLE_LIFECYCLE] Deleting image - articleId={}, imageId={}, url={}",
                    article.getId(), image.getId(), image.getImageUrl());
                imageService.deleteArticleImage(image.getImageUrl());
                log.debug("[ARTICLE_LIFECYCLE] Image deleted - articleId={}, imageId={}",
                    article.getId(), image.getId());
            } catch (Exception e) {
                log.error("[ARTICLE_LIFECYCLE] Failed to delete image - articleId={}, imageId={}, url={}, error={}",
                    article.getId(), image.getId(), image.getImageUrl(), e.getMessage(), e);
            }
        }
        log.info("[ARTICLE_LIFECYCLE] All images cleanup completed - articleId={}, deletedCount={}",
            article.getId(), article.getImages().size());
    }
}
