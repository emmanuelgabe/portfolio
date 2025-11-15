package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;

import java.util.List;

/**
 * Service interface for Project operations
 * Provides business logic for project management
 */
public interface ProjectService {

    /**
     * Get all projects
     * @return List of all projects
     */
    List<ProjectResponse> getAllProjects();

    /**
     * Get project by ID
     * @param id Project ID
     * @return Project response
     */
    ProjectResponse getProjectById(Long id);

    /**
     * Create a new project
     * @param request Create project request
     * @return Created project response
     */
    ProjectResponse createProject(CreateProjectRequest request);

    /**
     * Update an existing project
     * @param id Project ID
     * @param request Update project request
     * @return Updated project response
     */
    ProjectResponse updateProject(Long id, UpdateProjectRequest request);

    /**
     * Delete a project
     * @param id Project ID
     */
    void deleteProject(Long id);

    /**
     * Get all featured projects
     * @return List of featured projects
     */
    List<ProjectResponse> getFeaturedProjects();

    /**
     * Search projects by title
     * @param title Title to search for
     * @return List of matching projects
     */
    List<ProjectResponse> searchByTitle(String title);

    /**
     * Search projects by technology
     * @param technology Technology to search for
     * @return List of matching projects
     */
    List<ProjectResponse> searchByTechnology(String technology);
}
