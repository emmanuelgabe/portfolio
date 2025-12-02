package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for public article endpoints.
 * Provides endpoints for viewing published articles.
 * Admin endpoints are in AdminArticleController under /api/admin/articles.
 */
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Articles", description = "Public blog articles endpoints")
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    @Operation(summary = "Get all published articles", description = "Retrieves all published articles visible to the public")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<ArticleResponse>> getAllPublished() {
        log.debug("[ARTICLES] Fetching all published articles");
        List<ArticleResponse> articles = articleService.getAllPublished();
        log.debug("[ARTICLES] Found {} published articles", articles.size());
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get published articles with pagination",
               description = "Retrieves published articles with pagination support. Default: page=0, size=10")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<Page<ArticleResponse>> getAllPublishedPaginated(
        @PageableDefault(size = 10, sort = "publishedAt") Pageable pageable
    ) {
        log.debug("[ARTICLES] Fetching published articles - page={}, size={}",
            pageable.getPageNumber(), pageable.getPageSize());
        Page<ArticleResponse> articles = articleService.getAllPublishedPaginated(pageable);
        log.debug("[ARTICLES] Found {} total published articles", articles.getTotalElements());
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get article by slug", description = "Retrieves a published article by its slug")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "Article not found")
    public ResponseEntity<ArticleResponse> getBySlug(@PathVariable String slug) {
        log.debug("[ARTICLES] Fetching article by slug={}", slug);
        ArticleResponse article = articleService.getBySlug(slug);
        log.debug("[ARTICLES] Found article: {}", article.getTitle());
        return ResponseEntity.ok(article);
    }
}
