package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.dto.article.CreateArticleRequest;
import com.emmanuelgabe.portfolio.dto.article.UpdateArticleRequest;
import com.emmanuelgabe.portfolio.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for admin article management.
 * Provides CRUD operations for articles including drafts.
 */
@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Articles", description = "Admin article management endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class AdminArticleController {

    private final ArticleService articleService;

    @GetMapping
    @Operation(summary = "Get all articles", description = "Retrieves all articles including drafts")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<List<ArticleResponse>> getAllArticles() {
        log.debug("[ADMIN_ARTICLES] Fetching all articles");
        List<ArticleResponse> articles = articleService.getAllArticles();
        log.debug("[ADMIN_ARTICLES] Found {} articles", articles.size());
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get all articles with pagination",
               description = "Retrieves all articles including drafts with pagination. Default: page=0, size=10")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<Page<ArticleResponse>> getAllArticlesPaginated(
        @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        log.debug("[ADMIN_ARTICLES] Fetching articles - page={}, size={}",
            pageable.getPageNumber(), pageable.getPageSize());
        Page<ArticleResponse> articles = articleService.getAllArticlesPaginated(pageable);
        log.debug("[ADMIN_ARTICLES] Found {} total articles", articles.getTotalElements());
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get article by ID", description = "Retrieves article by ID including drafts")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "Article not found")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<ArticleResponse> getById(@PathVariable Long id) {
        log.debug("[ADMIN_ARTICLES] Fetching article id={}", id);
        ArticleResponse article = articleService.getById(id);
        log.debug("[ADMIN_ARTICLES] Found article: {}", article.getTitle());
        return ResponseEntity.ok(article);
    }

    @PostMapping
    @Operation(summary = "Create article", description = "Creates a new article")
    @ApiResponse(responseCode = "201", description = "Article created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<ArticleResponse> createArticle(
        @Valid @RequestBody CreateArticleRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("[ADMIN_ARTICLES] Creating article - title={}, user={}",
            request.getTitle(), userDetails.getUsername());
        ArticleResponse created = articleService.createArticle(request, userDetails.getUsername());
        log.info("[ADMIN_ARTICLES] Created article id={}, slug={}", created.getId(), created.getSlug());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update article", description = "Updates an existing article")
    @ApiResponse(responseCode = "200", description = "Article updated")
    @ApiResponse(responseCode = "404", description = "Article not found")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<ArticleResponse> updateArticle(
        @PathVariable Long id,
        @Valid @RequestBody UpdateArticleRequest request
    ) {
        log.info("[ADMIN_ARTICLES] Updating article id={}", id);
        ArticleResponse updated = articleService.updateArticle(id, request);
        log.info("[ADMIN_ARTICLES] Updated article id={}, slug={}", updated.getId(), updated.getSlug());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete article", description = "Deletes an article")
    @ApiResponse(responseCode = "204", description = "Article deleted")
    @ApiResponse(responseCode = "404", description = "Article not found")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        log.info("[ADMIN_ARTICLES] Deleting article id={}", id);
        articleService.deleteArticle(id);
        log.info("[ADMIN_ARTICLES] Deleted article id={}", id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "Publish article", description = "Publishes an article")
    @ApiResponse(responseCode = "200", description = "Article published")
    @ApiResponse(responseCode = "404", description = "Article not found")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<ArticleResponse> publishArticle(@PathVariable Long id) {
        log.info("[ADMIN_ARTICLES] Publishing article id={}", id);
        ArticleResponse published = articleService.publishArticle(id);
        log.info("[ADMIN_ARTICLES] Published article id={}, publishedAt={}", published.getId(), published.getPublishedAt());
        return ResponseEntity.ok(published);
    }

    @PutMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish article", description = "Unpublishes an article")
    @ApiResponse(responseCode = "200", description = "Article unpublished")
    @ApiResponse(responseCode = "404", description = "Article not found")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<ArticleResponse> unpublishArticle(@PathVariable Long id) {
        log.info("[ADMIN_ARTICLES] Unpublishing article id={}", id);
        ArticleResponse unpublished = articleService.unpublishArticle(id);
        log.info("[ADMIN_ARTICLES] Unpublished article id={}", unpublished.getId());
        return ResponseEntity.ok(unpublished);
    }
}
