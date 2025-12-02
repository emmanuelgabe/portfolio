package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.ProjectImageResponse;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.ReorderProjectImagesRequest;
import com.emmanuelgabe.portfolio.dto.UpdateProjectImageRequest;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Update project image URLs
     * @param id Project ID
     * @param imageUrl Main image URL
     * @param thumbnailUrl Thumbnail image URL
     */
    void updateImageUrls(Long id, String imageUrl, String thumbnailUrl);

    /**
     * Upload and assign project image
     * Coordinates image upload and project update
     * @param id Project ID
     * @param file Image file to upload
     * @return Image upload response with URLs
     */
    com.emmanuelgabe.portfolio.dto.ImageUploadResponse uploadAndAssignProjectImage(Long id, MultipartFile file);

    /**
     * Delete project image
     * Removes physical files and clears URLs in database
     * @param id Project ID
     */
    void deleteProjectImage(Long id);

    // ========== Multi-image Management ==========

    /**
     * Add image to project gallery
     * @param projectId Project ID
     * @param file Image file to upload
     * @return Created image response
     */
    ProjectImageResponse addImageToProject(Long projectId, MultipartFile file);

    /**
     * Add image to project gallery with metadata
     * @param projectId Project ID
     * @param file Image file to upload
     * @param altText Alt text for accessibility
     * @param caption Caption for the image
     * @return Created image response
     */
    ProjectImageResponse addImageToProject(Long projectId, MultipartFile file, String altText, String caption);

    /**
     * Remove image from project gallery
     * @param projectId Project ID
     * @param imageId Image ID to remove
     */
    void removeImageFromProject(Long projectId, Long imageId);

    /**
     * Update image metadata (alt text, caption)
     * @param projectId Project ID
     * @param imageId Image ID to update
     * @param request Update request with new metadata
     * @return Updated image response
     */
    ProjectImageResponse updateProjectImage(Long projectId, Long imageId, UpdateProjectImageRequest request);

    /**
     * Set an image as the primary/thumbnail image for the project
     * @param projectId Project ID
     * @param imageId Image ID to set as primary
     */
    void setPrimaryImage(Long projectId, Long imageId);

    /**
     * Reorder project images
     * @param projectId Project ID
     * @param request Reorder request with new image order
     */
    void reorderImages(Long projectId, ReorderProjectImagesRequest request);

    /**
     * Get all images for a project
     * @param projectId Project ID
     * @return List of project images
     */
    List<ProjectImageResponse> getProjectImages(Long projectId);
}
