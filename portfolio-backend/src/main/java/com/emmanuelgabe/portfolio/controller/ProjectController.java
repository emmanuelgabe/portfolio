package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;
import com.emmanuelgabe.portfolio.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Get all projects
     * @return List of all projects
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        log.debug("[LIST_PROJECTS] Request received");
        List<ProjectResponse> projects = projectService.getAllProjects();
        log.info("[LIST_PROJECTS] Success - count={}", projects.size());
        return ResponseEntity.ok(projects);
    }

    /**
     * Get project by ID
     * @param id Project ID
     * @return Project details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        log.debug("[GET_PROJECT] Request received - id={}", id);
        ProjectResponse project = projectService.getProjectById(id);
        log.debug("[GET_PROJECT] Success - id={}, title={}", project.getId(), project.getTitle());
        return ResponseEntity.ok(project);
    }

    /**
     * Create a new project
     * @param request Create project request
     * @return Created project
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        log.info("[CREATE_PROJECT] Request received - title={}", request.getTitle());
        ProjectResponse createdProject = projectService.createProject(request);
        log.info("[CREATE_PROJECT] Success - id={}, title={}", createdProject.getId(), createdProject.getTitle());
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
        log.info("[UPDATE_PROJECT] Request received - id={}", id);
        ProjectResponse updatedProject = projectService.updateProject(id, request);
        log.info("[UPDATE_PROJECT] Success - id={}, title={}", updatedProject.getId(), updatedProject.getTitle());
        return ResponseEntity.ok(updatedProject);
    }

    /**
     * Delete a project
     * @param id Project ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        log.info("[DELETE_PROJECT] Request received - id={}", id);
        projectService.deleteProject(id);
        log.info("[DELETE_PROJECT] Success - id={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all featured projects
     * @return List of featured projects
     */
    @GetMapping("/featured")
    public ResponseEntity<List<ProjectResponse>> getFeaturedProjects() {
        log.debug("[LIST_FEATURED_PROJECTS] Request received");
        List<ProjectResponse> featuredProjects = projectService.getFeaturedProjects();
        log.info("[LIST_FEATURED_PROJECTS] Success - count={}", featuredProjects.size());
        return ResponseEntity.ok(featuredProjects);
    }

    /**
     * Search projects by title
     * @param title Title to search for
     * @return List of matching projects
     */
    @GetMapping("/search/title")
    public ResponseEntity<List<ProjectResponse>> searchByTitle(@RequestParam String title) {
        log.debug("[SEARCH_PROJECTS_TITLE] Request received - title={}", title);
        List<ProjectResponse> projects = projectService.searchByTitle(title);
        log.info("[SEARCH_PROJECTS_TITLE] Success - title={}, count={}", title, projects.size());
        return ResponseEntity.ok(projects);
    }

    /**
     * Search projects by technology
     * @param technology Technology to search for
     * @return List of matching projects
     */
    @GetMapping("/search/technology")
    public ResponseEntity<List<ProjectResponse>> searchByTechnology(@RequestParam String technology) {
        log.debug("[SEARCH_PROJECTS_TECH] Request received - technology={}", technology);
        List<ProjectResponse> projects = projectService.searchByTechnology(technology);
        log.info("[SEARCH_PROJECTS_TECH] Success - technology={}, count={}", technology, projects.size());
        return ResponseEntity.ok(projects);
    }
}
