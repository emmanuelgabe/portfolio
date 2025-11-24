package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for public project endpoints.
 * Admin endpoints are in AdminProjectController under /api/admin/projects.
 */
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
        log.debug("[PROJECTS] Fetching all projects");
        List<ProjectResponse> projects = projectService.getAllProjects();
        log.debug("[PROJECTS] Found {} projects", projects.size());
        return ResponseEntity.ok(projects);
    }

    /**
     * Get project by ID
     * @param id Project ID
     * @return Project details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        log.debug("[PROJECTS] Fetching project id={}", id);
        ProjectResponse project = projectService.getProjectById(id);
        log.debug("[PROJECTS] Found project: {}", project.getTitle());
        return ResponseEntity.ok(project);
    }

    /**
     * Get all featured projects
     * @return List of featured projects
     */
    @GetMapping("/featured")
    public ResponseEntity<List<ProjectResponse>> getFeaturedProjects() {
        log.debug("[PROJECTS] Fetching featured projects");
        List<ProjectResponse> featuredProjects = projectService.getFeaturedProjects();
        log.debug("[PROJECTS] Found {} featured projects", featuredProjects.size());
        return ResponseEntity.ok(featuredProjects);
    }

    /**
     * Search projects by title
     * @param title Title to search for
     * @return List of matching projects
     */
    @GetMapping("/search/title")
    public ResponseEntity<List<ProjectResponse>> searchByTitle(@RequestParam String title) {
        log.debug("[PROJECTS] Searching by title={}", title);
        List<ProjectResponse> projects = projectService.searchByTitle(title);
        log.debug("[PROJECTS] Found {} projects matching title={}", projects.size(), title);
        return ResponseEntity.ok(projects);
    }

    /**
     * Search projects by technology
     * @param technology Technology to search for
     * @return List of matching projects
     */
    @GetMapping("/search/technology")
    public ResponseEntity<List<ProjectResponse>> searchByTechnology(@RequestParam String technology) {
        log.debug("[PROJECTS] Searching by technology={}", technology);
        List<ProjectResponse> projects = projectService.searchByTechnology(technology);
        log.debug("[PROJECTS] Found {} projects using technology={}", projects.size(), technology);
        return ResponseEntity.ok(projects);
    }
}
