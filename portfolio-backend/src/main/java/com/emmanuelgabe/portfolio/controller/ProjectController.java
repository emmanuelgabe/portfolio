package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;
import com.emmanuelgabe.portfolio.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * Get all projects
     * @return List of all projects
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        List<ProjectResponse> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * Get project by ID
     * @param id Project ID
     * @return Project details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        ProjectResponse project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    /**
     * Create a new project
     * @param request Create project request
     * @return Created project
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse createdProject = projectService.createProject(request);
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
        ProjectResponse updatedProject = projectService.updateProject(id, request);
        return ResponseEntity.ok(updatedProject);
    }

    /**
     * Delete a project
     * @param id Project ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all featured projects
     * @return List of featured projects
     */
    @GetMapping("/featured")
    public ResponseEntity<List<ProjectResponse>> getFeaturedProjects() {
        List<ProjectResponse> featuredProjects = projectService.getFeaturedProjects();
        return ResponseEntity.ok(featuredProjects);
    }

    /**
     * Search projects by title
     * @param title Title to search for
     * @return List of matching projects
     */
    @GetMapping("/search/title")
    public ResponseEntity<List<ProjectResponse>> searchByTitle(@RequestParam String title) {
        List<ProjectResponse> projects = projectService.searchByTitle(title);
        return ResponseEntity.ok(projects);
    }

    /**
     * Search projects by technology
     * @param technology Technology to search for
     * @return List of matching projects
     */
    @GetMapping("/search/technology")
    public ResponseEntity<List<ProjectResponse>> searchByTechnology(@RequestParam String technology) {
        List<ProjectResponse> projects = projectService.searchByTechnology(technology);
        return ResponseEntity.ok(projects);
    }
}
