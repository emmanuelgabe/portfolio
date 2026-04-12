package com.emmanuelgabe.portfolio.search.listener;

import com.emmanuelgabe.portfolio.search.event.ArticleIndexEvent;
import com.emmanuelgabe.portfolio.search.event.ExperienceIndexEvent;
import com.emmanuelgabe.portfolio.search.event.ProjectIndexEvent;
import com.emmanuelgabe.portfolio.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for vector embedding ingestion.
 * Reacts to the same entity lifecycle events as {@link SearchIndexEventListener}
 * and delegates to {@link DocumentIngestionService} for embedding updates.
 * Only active when DocumentIngestionService is available (AI + ES enabled).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"ai.enabled", "elasticsearch.enabled"}, havingValue = "true")
public class EmbeddingIndexEventListener {

    private final DocumentIngestionService documentIngestionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleArticleIndexEvent(ArticleIndexEvent event) {
        log.debug("[EMBEDDING_INDEX] Handling article event - id={}, action={}",
                event.getArticle().getId(), event.getAction());
        try {
            switch (event.getAction()) {
                case INDEX -> documentIngestionService.ingestArticle(event.getArticle());
                case REMOVE -> documentIngestionService.removeArticle(event.getArticle().getId());
            }
        } catch (Exception e) {
            log.error("[EMBEDDING_INDEX] Failed to process article event - id={}, action={}, error={}",
                    event.getArticle().getId(), event.getAction(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProjectIndexEvent(ProjectIndexEvent event) {
        log.debug("[EMBEDDING_INDEX] Handling project event - id={}, action={}",
                event.getProject().getId(), event.getAction());
        try {
            switch (event.getAction()) {
                case INDEX -> documentIngestionService.ingestProject(event.getProject());
                case REMOVE -> documentIngestionService.removeProject(event.getProject().getId());
            }
        } catch (Exception e) {
            log.error("[EMBEDDING_INDEX] Failed to process project event - id={}, action={}, error={}",
                    event.getProject().getId(), event.getAction(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleExperienceIndexEvent(ExperienceIndexEvent event) {
        log.debug("[EMBEDDING_INDEX] Handling experience event - id={}, action={}",
                event.getExperience().getId(), event.getAction());
        try {
            switch (event.getAction()) {
                case INDEX -> documentIngestionService.ingestExperience(event.getExperience());
                case REMOVE -> documentIngestionService.removeExperience(event.getExperience().getId());
            }
        } catch (Exception e) {
            log.error("[EMBEDDING_INDEX] Failed to process experience event - id={}, action={}, error={}",
                    event.getExperience().getId(), event.getAction(), e.getMessage());
        }
    }
}
