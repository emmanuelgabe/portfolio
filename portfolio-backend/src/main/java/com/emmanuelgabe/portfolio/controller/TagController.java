package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for public tag endpoints.
 * Admin endpoints are in AdminTagController under /api/admin/tags.
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tags", description = "Public tag endpoints")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "Get all tags", description = "Retrieves all available tags")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<TagResponse>> getAllTags() {
        log.debug("[TAGS] Fetching all tags");
        List<TagResponse> tags = tagService.getAllTags();
        log.debug("[TAGS] Found {} tags", tags.size());
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tag by ID", description = "Retrieves a single tag by its ID")
    @ApiResponse(responseCode = "200", description = "Tag found")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    public ResponseEntity<TagResponse> getTagById(@PathVariable Long id) {
        log.debug("[TAGS] Fetching tag id={}", id);
        TagResponse tag = tagService.getTagById(id);
        log.debug("[TAGS] Found tag: {}", tag.getName());
        return ResponseEntity.ok(tag);
    }
}
