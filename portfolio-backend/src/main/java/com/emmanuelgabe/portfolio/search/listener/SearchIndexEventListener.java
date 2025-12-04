package com.emmanuelgabe.portfolio.search.listener;

import com.emmanuelgabe.portfolio.search.event.ArticleIndexEvent;
import com.emmanuelgabe.portfolio.search.event.ExperienceIndexEvent;
import com.emmanuelgabe.portfolio.search.event.ProjectIndexEvent;
import com.emmanuelgabe.portfolio.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for search index operations.
 * Handles indexing and removal of documents after transaction commit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexEventListener {

    private final SearchService searchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleArticleIndexEvent(ArticleIndexEvent event) {
        log.debug("[SEARCH_INDEX] Handling article index event - id={}, action={}",
                event.getArticle().getId(), event.getAction());
        try {
            switch (event.getAction()) {
                case INDEX -> searchService.indexArticle(event.getArticle());
                case REMOVE -> searchService.removeArticle(event.getArticle().getId());
            }
        } catch (Exception e) {
            log.error("[SEARCH_INDEX] Failed to process article index event - id={}, action={}, error={}",
                    event.getArticle().getId(), event.getAction(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProjectIndexEvent(ProjectIndexEvent event) {
        log.debug("[SEARCH_INDEX] Handling project index event - id={}, action={}",
                event.getProject().getId(), event.getAction());
        try {
            switch (event.getAction()) {
                case INDEX -> searchService.indexProject(event.getProject());
                case REMOVE -> searchService.removeProject(event.getProject().getId());
            }
        } catch (Exception e) {
            log.error("[SEARCH_INDEX] Failed to process project index event - id={}, action={}, error={}",
                    event.getProject().getId(), event.getAction(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleExperienceIndexEvent(ExperienceIndexEvent event) {
        log.debug("[SEARCH_INDEX] Handling experience index event - id={}, action={}",
                event.getExperience().getId(), event.getAction());
        try {
            switch (event.getAction()) {
                case INDEX -> searchService.indexExperience(event.getExperience());
                case REMOVE -> searchService.removeExperience(event.getExperience().getId());
            }
        } catch (Exception e) {
            log.error("[SEARCH_INDEX] Failed to process experience index event - id={}, action={}, error={}",
                    event.getExperience().getId(), event.getAction(), e.getMessage());
        }
    }
}
