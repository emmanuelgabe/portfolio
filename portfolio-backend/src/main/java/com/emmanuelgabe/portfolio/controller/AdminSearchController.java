package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.search.ArticleSearchResult;
import com.emmanuelgabe.portfolio.dto.search.ExperienceSearchResult;
import com.emmanuelgabe.portfolio.dto.search.ProjectSearchResult;
import com.emmanuelgabe.portfolio.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for admin search endpoints.
 * Provides full-text search capabilities for Articles, Projects, and Experiences.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
@Tag(name = "Admin - Search", description = "Admin full-text search endpoints")
public class AdminSearchController {

    private final SearchService searchService;

    @GetMapping("/articles")
    @Operation(summary = "Search articles",
            description = "Full-text search across article title, content, excerpt, and tags")
    @ApiResponse(responseCode = "200", description = "Search results returned successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    public ResponseEntity<List<ArticleSearchResult>> searchArticles(
            @Parameter(description = "Search query (2-100 characters)")
            @RequestParam @Size(min = 2, max = 100, message = "Query must be between 2 and 100 characters") String q) {
        log.info("[ADMIN_SEARCH] Searching articles - query={}", q);
        List<ArticleSearchResult> results = searchService.searchArticles(q);
        log.info("[ADMIN_SEARCH] Found {} articles - query={}", results.size(), q);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/projects")
    @Operation(summary = "Search projects",
            description = "Full-text search across project title, description, tech stack, and tags")
    @ApiResponse(responseCode = "200", description = "Search results returned successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    public ResponseEntity<List<ProjectSearchResult>> searchProjects(
            @Parameter(description = "Search query (2-100 characters)")
            @RequestParam @Size(min = 2, max = 100, message = "Query must be between 2 and 100 characters") String q) {
        log.info("[ADMIN_SEARCH] Searching projects - query={}", q);
        List<ProjectSearchResult> results = searchService.searchProjects(q);
        log.info("[ADMIN_SEARCH] Found {} projects - query={}", results.size(), q);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/experiences")
    @Operation(summary = "Search experiences",
            description = "Full-text search across experience company, role, and description")
    @ApiResponse(responseCode = "200", description = "Search results returned successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    public ResponseEntity<List<ExperienceSearchResult>> searchExperiences(
            @Parameter(description = "Search query (2-100 characters)")
            @RequestParam @Size(min = 2, max = 100, message = "Query must be between 2 and 100 characters") String q) {
        log.info("[ADMIN_SEARCH] Searching experiences - query={}", q);
        List<ExperienceSearchResult> results = searchService.searchExperiences(q);
        log.info("[ADMIN_SEARCH] Found {} experiences - query={}", results.size(), q);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/reindex")
    @Operation(summary = "Rebuild search indices",
            description = "Rebuilds all search indices from database. Use when indices are out of sync.")
    @ApiResponse(responseCode = "200", description = "Reindex completed successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    public ResponseEntity<ReindexResponse> reindexAll() {
        log.info("[ADMIN_SEARCH] Triggering full reindex");
        int totalIndexed = searchService.reindexAll();
        log.info("[ADMIN_SEARCH] Full reindex completed - totalIndexed={}", totalIndexed);
        return ResponseEntity.ok(new ReindexResponse(totalIndexed, "Reindex completed successfully"));
    }

    /**
     * Response DTO for reindex operation.
     */
    public record ReindexResponse(int totalIndexed, String message) { }
}
