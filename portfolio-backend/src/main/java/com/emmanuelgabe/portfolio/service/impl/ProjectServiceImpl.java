package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.ProjectMapper;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import com.emmanuelgabe.portfolio.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of ProjectService interface
 * Handles business logic for project management
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        log.debug("[LIST_PROJECTS] Fetching all projects");
        List<Project> projects = projectRepository.findAll();
        log.debug("[LIST_PROJECTS] Found {} projects", projects.size());
        return projects.stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        log.debug("[GET_PROJECT] Fetching project - id={}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[GET_PROJECT] Project not found - id={}", id);
                    return new ResourceNotFoundException("Project", "id", id);
                });
        return projectMapper.toResponse(project);
    }

    @Override
    public ProjectResponse createProject(CreateProjectRequest request) {
        log.debug("[CREATE_PROJECT] Creating project - title={}", request.getTitle());

        Project project = projectMapper.toEntity(request);

        // Associate tags if provided
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            log.debug("[CREATE_PROJECT] Associating tags - count={}", request.getTagIds().size());
            Set<Tag> tags = new HashSet<>();
            for (Long tagId : request.getTagIds()) {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> {
                            log.warn("[CREATE_PROJECT] Tag not found - tagId={}", tagId);
                            return new ResourceNotFoundException("Tag", "id", tagId);
                        });
                tags.add(tag);
            }
            project.setTags(tags);
        }

        // Use domain logic for featured status
        if (request.isFeatured()) {
            try {
                project.markAsFeatured();
                log.debug("[CREATE_PROJECT] Project marked as featured - title={}", request.getTitle());
            } catch (IllegalStateException e) {
                log.warn("[CREATE_PROJECT] Cannot mark as featured - reason={}", e.getMessage());
                // Project will be created but not featured
                project.setFeatured(false);
            }
        } else {
            project.setFeatured(false);
        }

        Project savedProject = projectRepository.save(project);
        log.info("[CREATE_PROJECT] Project created - id={}, title={}, featured={}, tagsCount={}",
                savedProject.getId(), savedProject.getTitle(), savedProject.isFeatured(), savedProject.getTags().size());
        return projectMapper.toResponse(savedProject);
    }

    @Override
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
        log.debug("[UPDATE_PROJECT] Updating project - id={}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[UPDATE_PROJECT] Project not found - id={}", id);
                    return new ResourceNotFoundException("Project", "id", id);
                });

        // Store current featured status to detect changes
        boolean wasFeatured = project.isFeatured();

        // Update only provided fields using MapStruct
        projectMapper.updateEntityFromRequest(request, project);

        // Update tags if provided
        if (request.getTagIds() != null) {
            log.debug("[UPDATE_PROJECT] Updating tags - id={}, tagsCount={}", id, request.getTagIds().size());
            project.getTags().clear();

            if (!request.getTagIds().isEmpty()) {
                Set<Tag> tags = new HashSet<>();
                for (Long tagId : request.getTagIds()) {
                    Tag tag = tagRepository.findById(tagId)
                            .orElseThrow(() -> {
                                log.warn("[UPDATE_PROJECT] Tag not found - tagId={}", tagId);
                                return new ResourceNotFoundException("Tag", "id", tagId);
                            });
                    tags.add(tag);
                }
                project.setTags(tags);
            }
        }

        // Use domain logic for featured status if changed
        if (request.getFeatured() != null) {
            if (request.getFeatured() && !wasFeatured) {
                try {
                    project.markAsFeatured();
                    log.debug("[UPDATE_PROJECT] Project marked as featured - id={}", id);
                } catch (IllegalStateException e) {
                    log.warn("[UPDATE_PROJECT] Cannot mark as featured - id={}, reason={}", id, e.getMessage());
                    project.setFeatured(false);
                }
            } else if (!request.getFeatured() && wasFeatured) {
                project.unfeature();
                log.debug("[UPDATE_PROJECT] Project unfeatured - id={}", id);
            }
        }

        Project updatedProject = projectRepository.save(project);
        log.info("[UPDATE_PROJECT] Project updated - id={}, title={}, featured={}",
                updatedProject.getId(), updatedProject.getTitle(), updatedProject.isFeatured());
        return projectMapper.toResponse(updatedProject);
    }

    @Override
    public void deleteProject(Long id) {
        log.debug("[DELETE_PROJECT] Deleting project - id={}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[DELETE_PROJECT] Project not found - id={}", id);
                    return new ResourceNotFoundException("Project", "id", id);
                });

        projectRepository.delete(project);
        log.info("[DELETE_PROJECT] Project deleted - id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getFeaturedProjects() {
        log.debug("[LIST_FEATURED_PROJECTS] Fetching featured projects");
        List<Project> projects = projectRepository.findByFeaturedTrue();
        log.debug("[LIST_FEATURED_PROJECTS] Found {} featured projects", projects.size());
        return projects.stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> searchByTitle(String title) {
        log.debug("[SEARCH_PROJECTS_TITLE] Searching by title - title={}", title);
        List<Project> projects = projectRepository.findByTitleContainingIgnoreCase(title);
        log.debug("[SEARCH_PROJECTS_TITLE] Found {} projects", projects.size());
        return projects.stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> searchByTechnology(String technology) {
        log.debug("[SEARCH_PROJECTS_TECH] Searching by technology - technology={}", technology);
        List<Project> projects = projectRepository.findByTechnology(technology);
        log.debug("[SEARCH_PROJECTS_TECH] Found {} projects", projects.size());
        return projects.stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }
}
