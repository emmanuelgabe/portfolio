package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;

    public ProjectService(ProjectRepository projectRepository, TagRepository tagRepository) {
        this.projectRepository = projectRepository;
        this.tagRepository = tagRepository;
    }

    /**
     * Get all projects
     * @return List of all projects
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get project by ID
     * @param id Project ID
     * @return Project response
     * @throws ResourceNotFoundException if project not found
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        return ProjectResponse.fromEntity(project);
    }

    /**
     * Create a new project
     * @param request Create project request
     * @return Created project response
     */
    public ProjectResponse createProject(CreateProjectRequest request) {
        Project project = new Project();
        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setTechStack(request.getTechStack());
        project.setGithubUrl(request.getGithubUrl());
        project.setImageUrl(request.getImageUrl());
        project.setDemoUrl(request.getDemoUrl());
        project.setFeatured(request.getFeatured() != null ? request.getFeatured() : false);

        // Associate tags if provided
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<Tag> tags = new HashSet<>();
            for (Long tagId : request.getTagIds()) {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));
                tags.add(tag);
            }
            project.setTags(tags);
        }

        Project savedProject = projectRepository.save(project);
        return ProjectResponse.fromEntity(savedProject);
    }

    /**
     * Update an existing project
     * @param id Project ID
     * @param request Update project request
     * @return Updated project response
     * @throws ResourceNotFoundException if project not found
     */
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

        // Update only provided fields
        if (request.getTitle() != null) {
            project.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getTechStack() != null) {
            project.setTechStack(request.getTechStack());
        }
        if (request.getGithubUrl() != null) {
            project.setGithubUrl(request.getGithubUrl());
        }
        if (request.getImageUrl() != null) {
            project.setImageUrl(request.getImageUrl());
        }
        if (request.getDemoUrl() != null) {
            project.setDemoUrl(request.getDemoUrl());
        }
        if (request.getFeatured() != null) {
            project.setFeatured(request.getFeatured());
        }

        // Update tags if provided
        if (request.getTagIds() != null) {
            // Clear existing tags
            project.getTags().clear();

            // Add new tags
            if (!request.getTagIds().isEmpty()) {
                Set<Tag> tags = new HashSet<>();
                for (Long tagId : request.getTagIds()) {
                    Tag tag = tagRepository.findById(tagId)
                            .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));
                    tags.add(tag);
                }
                project.setTags(tags);
            }
        }

        Project updatedProject = projectRepository.save(project);
        return ProjectResponse.fromEntity(updatedProject);
    }

    /**
     * Delete a project
     * @param id Project ID
     * @throws ResourceNotFoundException if project not found
     */
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        projectRepository.delete(project);
    }

    /**
     * Get all featured projects
     * @return List of featured projects
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getFeaturedProjects() {
        return projectRepository.findByFeaturedTrue().stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search projects by title
     * @param title Title to search for
     * @return List of matching projects
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> searchByTitle(String title) {
        return projectRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Search projects by technology
     * @param technology Technology to search for
     * @return List of matching projects
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> searchByTechnology(String technology) {
        return projectRepository.findByTechnology(technology).stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
