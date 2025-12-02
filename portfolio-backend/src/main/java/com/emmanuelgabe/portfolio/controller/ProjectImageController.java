package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.ProjectImageResponse;
import com.emmanuelgabe.portfolio.dto.ReorderProjectImagesRequest;
import com.emmanuelgabe.portfolio.dto.UpdateProjectImageRequest;
import com.emmanuelgabe.portfolio.service.ProjectService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller for managing project images (admin only).
 * Supports multiple images per project with carousel functionality.
 */
@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Project Images", description = "Project image management (admin only)")
public class ProjectImageController {

    private final ProjectService projectService;

    @PostMapping("/{id}/images")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload project image", description = "Upload a new image to the project gallery")
    @ApiResponse(responseCode = "201", description = "Image uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file or image limit reached")
    @ApiResponse(responseCode = "404", description = "Project not found")
    public ResponseEntity<ProjectImageResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "caption", required = false) String caption
    ) {
        log.info("[UPLOAD_PROJECT_IMAGE] Request - projectId={}, fileName={}", id, file.getOriginalFilename());
        ProjectImageResponse response = projectService.addImageToProject(id, file, altText, caption);
        log.info("[UPLOAD_PROJECT_IMAGE] Success - projectId={}, imageId={}", id, response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/images")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get project images", description = "Get all images for a project")
    @ApiResponse(responseCode = "200", description = "Images retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Project not found")
    public ResponseEntity<List<ProjectImageResponse>> getImages(@PathVariable Long id) {
        log.debug("[GET_PROJECT_IMAGES] Request - projectId={}", id);
        List<ProjectImageResponse> images = projectService.getProjectImages(id);
        return ResponseEntity.ok(images);
    }

    @DeleteMapping("/{projectId}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete project image", description = "Remove an image from the project gallery")
    @ApiResponse(responseCode = "204", description = "Image deleted successfully")
    @ApiResponse(responseCode = "404", description = "Project or image not found")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long projectId,
            @PathVariable Long imageId
    ) {
        log.info("[DELETE_PROJECT_IMAGE] Request - projectId={}, imageId={}", projectId, imageId);
        projectService.removeImageFromProject(projectId, imageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{projectId}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update image metadata", description = "Update alt text and caption for an image")
    @ApiResponse(responseCode = "200", description = "Image updated successfully")
    @ApiResponse(responseCode = "404", description = "Project or image not found")
    public ResponseEntity<ProjectImageResponse> updateImage(
            @PathVariable Long projectId,
            @PathVariable Long imageId,
            @Valid @RequestBody UpdateProjectImageRequest request
    ) {
        log.info("[UPDATE_PROJECT_IMAGE] Request - projectId={}, imageId={}", projectId, imageId);
        ProjectImageResponse response = projectService.updateProjectImage(projectId, imageId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectId}/images/{imageId}/primary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set primary image", description = "Set an image as the primary/thumbnail image for the project")
    @ApiResponse(responseCode = "200", description = "Primary image set successfully")
    @ApiResponse(responseCode = "404", description = "Project or image not found")
    public ResponseEntity<Void> setPrimaryImage(
            @PathVariable Long projectId,
            @PathVariable Long imageId
    ) {
        log.info("[SET_PRIMARY_IMAGE] Request - projectId={}, imageId={}", projectId, imageId);
        projectService.setPrimaryImage(projectId, imageId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{projectId}/images/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reorder images", description = "Change the display order of project images")
    @ApiResponse(responseCode = "200", description = "Images reordered successfully")
    @ApiResponse(responseCode = "404", description = "Project or image not found")
    public ResponseEntity<Void> reorderImages(
            @PathVariable Long projectId,
            @Valid @RequestBody ReorderProjectImagesRequest request
    ) {
        log.info("[REORDER_IMAGES] Request - projectId={}", projectId);
        projectService.reorderImages(projectId, request);
        return ResponseEntity.ok().build();
    }
}
