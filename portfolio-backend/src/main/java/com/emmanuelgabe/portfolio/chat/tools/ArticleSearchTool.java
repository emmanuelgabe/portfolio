package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.chat.ToolCallTracker;
import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Agent tools for listing and retrieving blog articles.
 * Only exposes published articles to visitors.
 */
@Slf4j
@RequiredArgsConstructor
public class ArticleSearchTool {

    private static final int MAX_PAGE_SIZE = 10;

    private final ArticleService articleService;
    private final ToolCallTracker tracker;

    /**
     * List published articles with pagination.
     *
     * @param page zero-based page number
     * @param size number of articles per page (capped at 10)
     * @return paginated published articles sorted by publication date
     */
    @Tool(description = "List published blog articles with pagination. "
            + "Returns article titles, slugs, excerpts, and publication dates.")
    public Page<ArticleResponse> listArticles(
            @ToolParam(description = "Page number (0-based)") int page,
            @ToolParam(description = "Number of articles per page (1 to 10)") int size) {
        tracker.record("listArticles");
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        log.debug("[CHAT_TOOL] listArticles - page={}, size={}", page, safeSize);
        return articleService.getAllPublishedPaginated(
                PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "publishedAt")));
    }

    /**
     * Retrieve the full content of a specific article by slug.
     *
     * @param slug the article URL-friendly slug
     * @return complete article including content, tags, and metadata
     */
    @Tool(description = "Get the full content of a specific blog article by its slug.")
    public ArticleResponse getArticle(
            @ToolParam(description = "The article slug (URL-friendly identifier)") String slug) {
        tracker.record("getArticle");
        log.debug("[CHAT_TOOL] getArticle - slug={}", slug);
        return articleService.getBySlug(slug);
    }
}
