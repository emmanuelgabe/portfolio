package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CreateTagRequest;
import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.dto.UpdateTagRequest;
import com.emmanuelgabe.portfolio.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * REST controller for admin tag management.
 * Provides CRUD operations for tags.
 */
@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Tags", description = "Admin tag management endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "Get all tags", description = "Retrieves all available tags")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<List<TagResponse>> getAllTags() {
        log.debug("[ADMIN_TAGS] Fetching all tags");
        List<TagResponse> tags = tagService.getAllTags();
        log.debug("[ADMIN_TAGS] Found {} tags", tags.size());
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tag by ID", description = "Retrieves a single tag by its ID")
    @ApiResponse(responseCode = "200", description = "Tag found")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<TagResponse> getTagById(@PathVariable Long id) {
        log.debug("[ADMIN_TAGS] Fetching tag id={}", id);
        TagResponse tag = tagService.getTagById(id);
        log.debug("[ADMIN_TAGS] Found tag: {}", tag.getName());
        return ResponseEntity.ok(tag);
    }

    @PostMapping
    @Operation(summary = "Create tag", description = "Creates a new tag")
    @ApiResponse(responseCode = "201", description = "Tag created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest request) {
        log.info("[ADMIN_TAGS] Creating tag - name={}, color={}", request.getName(), request.getColor());
        TagResponse response = tagService.createTag(request);
        log.info("[ADMIN_TAGS] Created tag id={}, name={}", response.getId(), response.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update tag", description = "Updates an existing tag")
    @ApiResponse(responseCode = "200", description = "Tag updated")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<TagResponse> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTagRequest request) {
        log.info("[ADMIN_TAGS] Updating tag id={} - name={}, color={}", id, request.getName(), request.getColor());
        TagResponse response = tagService.updateTag(id, request);
        log.info("[ADMIN_TAGS] Updated tag id={}, name={}", response.getId(), response.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete tag", description = "Deletes a tag")
    @ApiResponse(responseCode = "204", description = "Tag deleted")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        log.info("[ADMIN_TAGS] Deleting tag id={}", id);
        tagService.deleteTag(id);
        log.info("[ADMIN_TAGS] Deleted tag id={}", id);
        return ResponseEntity.noContent().build();
    }
}
