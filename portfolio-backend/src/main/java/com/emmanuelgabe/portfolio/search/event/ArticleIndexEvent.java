package com.emmanuelgabe.portfolio.search.event;

import com.emmanuelgabe.portfolio.entity.Article;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event published when an article needs to be indexed or removed from search index.
 */
@Getter
@RequiredArgsConstructor
public class ArticleIndexEvent {

    private final Article article;
    private final IndexAction action;

    public enum IndexAction {
        INDEX,
        REMOVE
    }

    public static ArticleIndexEvent forIndex(Article article) {
        return new ArticleIndexEvent(article, IndexAction.INDEX);
    }

    public static ArticleIndexEvent forRemove(Article article) {
        return new ArticleIndexEvent(article, IndexAction.REMOVE);
    }
}
