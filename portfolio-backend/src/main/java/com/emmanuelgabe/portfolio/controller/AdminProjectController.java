package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.ImageUploadResponse;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.ReorderRequest;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;
import com.emmanuelgabe.portfolio.service.ProjectService;
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
 * REST controller for admin project management.
 * Provides CRUD operations for projects.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProjectController {

    private final ProjectService projectService;

    /**
     * Get all projects
     * @return List of all projects
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        log.debug("[ADMIN_PROJECTS] Fetching all projects");
        List<ProjectResponse> projects = projectService.getAllProjects();
        log.debug("[ADMIN_PROJECTS] Found {} projects", projects.size());
        return ResponseEntity.ok(projects);
    }

    /**
     * Get project by ID
     * @param id Project ID
     * @return Project details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        log.debug("[ADMIN_PROJECTS] Fetching project id={}", id);
        ProjectResponse project = projectService.getProjectById(id);
        log.debug("[ADMIN_PROJECTS] Found project: {}", project.getTitle());
        return ResponseEntity.ok(project);
    }

    /**
     * Create a new project
     * @param request Create project request
     * @return Created project
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        log.info("[ADMIN_PROJECTS] Creating project - title={}", request.getTitle());
        ProjectResponse createdProject = projectService.createProject(request);
        log.info("[ADMIN_PROJECTS] Created project id={}, title={}", createdProject.getId(), createdProject.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
    }

    /**
     * Update an existing project
     * @param id Project ID
     * @param request Update project request
     * @return Updated project
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request) {
        log.info("[ADMIN_PROJECTS] Updating project id={}", id);
        ProjectResponse updatedProject = projectService.updateProject(id, request);
        log.info("[ADMIN_PROJECTS] Updated project id={}, title={}", updatedProject.getId(), updatedProject.getTitle());
        return ResponseEntity.ok(updatedProject);
    }

    /**
     * Delete a project
     * @param id Project ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        log.info("[ADMIN_PROJECTS] Deleting project id={}", id);
        projectService.deleteProject(id);
        log.info("[ADMIN_PROJECTS] Deleted project id={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Upload project image
     * Generates optimized image (1200px max width, WebP) and thumbnail (300x300px square crop)
     * @param id Project ID
     * @param file Image file to upload (JPEG, PNG, or WebP, max 10MB)
     * @return Image upload response with URLs
     */
    @PostMapping("/{id}/image")
    public ResponseEntity<ImageUploadResponse> uploadProjectImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        log.info("[ADMIN_PROJECTS] Uploading image - projectId={}, fileName={}, size={}",
                id, file.getOriginalFilename(), file.getSize());
        ImageUploadResponse response = projectService.uploadAndAssignProjectImage(id, file);
        log.info("[ADMIN_PROJECTS] Uploaded image - projectId={}, imageUrl={}", id, response.getImageUrl());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete project image
     * Removes both optimized image and thumbnail
     * @param id Project ID
     * @return No content
     */
    @DeleteMapping("/{id}/image")
    public ResponseEntity<Void> deleteProjectImage(@PathVariable Long id) {
        log.info("[ADMIN_PROJECTS] Deleting image - projectId={}", id);
        projectService.deleteProjectImage(id);
        log.info("[ADMIN_PROJECTS] Deleted image - projectId={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reorder projects
     * @param request Reorder request with ordered IDs
     * @return No content
     */
    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderProjects(@Valid @RequestBody ReorderRequest request) {
        log.info("[ADMIN_PROJECTS] Reordering projects - count={}", request.getOrderedIds().size());
        projectService.reorderProjects(request);
        log.info("[ADMIN_PROJECTS] Projects reordered");
        return ResponseEntity.noContent().build();
    }
}
