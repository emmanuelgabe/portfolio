package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.audit.Auditable;
import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.ImageUploadResponse;
import com.emmanuelgabe.portfolio.dto.PreparedImageInfo;
import com.emmanuelgabe.portfolio.dto.ProjectImageResponse;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.ReorderProjectImagesRequest;
import com.emmanuelgabe.portfolio.dto.ReorderRequest;
import com.emmanuelgabe.portfolio.dto.UpdateProjectImageRequest;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;
import com.emmanuelgabe.portfolio.entity.ImageStatus;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.entity.ProjectImage;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.ProjectImageMapper;

import static com.emmanuelgabe.portfolio.util.EntityHelper.findOrThrow;
import static com.emmanuelgabe.portfolio.util.EntityHelper.findImageByUrl;

import com.emmanuelgabe.portfolio.mapper.ProjectMapper;
import com.emmanuelgabe.portfolio.messaging.event.ImageProcessingEvent;
import com.emmanuelgabe.portfolio.messaging.publisher.EventPublisher;
import com.emmanuelgabe.portfolio.repository.ProjectImageRepository;
import com.emmanuelgabe.portfolio.search.event.ProjectIndexEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import com.emmanuelgabe.portfolio.service.ImageService;
import com.emmanuelgabe.portfolio.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Comparator;
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

    private static final int MAX_IMAGES_PER_PROJECT = 10;

    private final ProjectRepository projectRepository;
    private final ProjectImageRepository projectImageRepository;
    private final TagRepository tagRepository;
    private final ProjectMapper projectMapper;
    private final ProjectImageMapper projectImageMapper;
    private final ImageService imageService;
    private final EventPublisher eventPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Cacheable(value = "projects", key = "'all'")
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        log.debug("[LIST_PROJECTS] Fetching all projects");
        List<Project> projects = projectRepository.findAllWithTags();
        log.debug("[LIST_PROJECTS] Found {} projects", projects.size());
        return projects.stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "projects", key = "#id")
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        log.debug("[GET_PROJECT] Fetching project - id={}", id);
        Project project = findOrThrow(projectRepository.findById(id), "Project", "id", id);
        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByIds(Collection<Long> ids) {
        log.debug("[LIST_PROJECTS] Fetching projects by IDs - count={}", ids.size());
        List<Project> projects = projectRepository.findAllById(ids);
        log.debug("[LIST_PROJECTS] Found {} projects by IDs", projects.size());
        return projects.stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    @Auditable(action = AuditAction.CREATE, entityType = "Project",
            entityIdExpression = "#result.id", entityNameExpression = "#result.title")
    public ProjectResponse createProject(CreateProjectRequest request) {
        log.debug("[CREATE_PROJECT] Creating project - title={}", request.getTitle());

        // Validate techStack is required when hasDetails is true
        if (request.isHasDetails() && (request.getTechStack() == null || request.getTechStack().isBlank())) {
            log.warn("[CREATE_PROJECT] Tech stack required when hasDetails is true - title={}", request.getTitle());
            throw new IllegalArgumentException("Tech stack is required when project has details page");
        }

        Project project = projectMapper.toEntity(request);

        // Associate tags if provided (batch loading with validation)
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            log.debug("[CREATE_PROJECT] Associating tags - count={}", request.getTagIds().size());
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            validateAllTagsFound(request.getTagIds(), tags);
            project.setTags(new HashSet<>(tags));
        }

        // Set featured status directly during creation
        // Note: Images are typically uploaded after project creation, so we don't validate
        // image presence here. The markAsFeatured() validation is used for updates only.
        project.setFeatured(request.isFeatured());
        if (request.isFeatured()) {
            log.debug("[CREATE_PROJECT] Project marked as featured - title={}", request.getTitle());
        }

        Project savedProject = projectRepository.save(project);
        applicationEventPublisher.publishEvent(ProjectIndexEvent.forIndex(savedProject));
        log.info("[CREATE_PROJECT] Project created - id={}, title={}, featured={}, tagsCount={}",
                savedProject.getId(), savedProject.getTitle(), savedProject.isFeatured(), savedProject.getTags().size());
        return projectMapper.toResponse(savedProject);
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    @Auditable(action = AuditAction.UPDATE, entityType = "Project",
            entityIdExpression = "#id", entityNameExpression = "#result.title")
    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
        log.debug("[UPDATE_PROJECT] Updating project - id={}", id);

        Project project = findOrThrow(projectRepository.findById(id), "Project", "id", id);

        // Determine effective hasDetails value (from request or existing)
        boolean effectiveHasDetails = request.getHasDetails() != null
                ? request.getHasDetails()
                : project.isHasDetails();

        // Determine effective techStack value (from request or existing)
        String effectiveTechStack = request.getTechStack() != null
                ? request.getTechStack()
                : project.getTechStack();

        // Validate techStack is required when hasDetails is true
        if (effectiveHasDetails && (effectiveTechStack == null || effectiveTechStack.isBlank())) {
            log.warn("[UPDATE_PROJECT] Tech stack required when hasDetails is true - id={}", id);
            throw new IllegalArgumentException("Tech stack is required when project has details page");
        }

        // Store current featured status to detect changes
        boolean wasFeatured = project.isFeatured();

        // Update only provided fields using MapStruct
        projectMapper.updateEntityFromRequest(request, project);

        // Update tags if provided (batch loading with validation)
        if (request.getTagIds() != null) {
            log.debug("[UPDATE_PROJECT] Updating tags - id={}, tagsCount={}", id, request.getTagIds().size());
            project.getTags().clear();
            if (!request.getTagIds().isEmpty()) {
                List<Tag> tags = tagRepository.findAllById(request.getTagIds());
                validateAllTagsFound(request.getTagIds(), tags);
                project.setTags(new HashSet<>(tags));
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
        applicationEventPublisher.publishEvent(ProjectIndexEvent.forIndex(updatedProject));
        log.info("[UPDATE_PROJECT] Project updated - id={}, title={}, featured={}",
                updatedProject.getId(), updatedProject.getTitle(), updatedProject.isFeatured());
        return projectMapper.toResponse(updatedProject);
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    @Auditable(action = AuditAction.DELETE, entityType = "Project", entityIdExpression = "#id")
    public void deleteProject(Long id) {
        log.debug("[DELETE_PROJECT] Deleting project - id={}", id);

        Project project = findOrThrow(projectRepository.findById(id), "Project", "id", id);

        projectRepository.delete(project);
        applicationEventPublisher.publishEvent(ProjectIndexEvent.forRemove(project));
        log.info("[DELETE_PROJECT] Project deleted - id={}", id);
    }

    @Override
    @Cacheable(value = "projects", key = "'featured'")
    @Transactional(readOnly = true)
    public List<ProjectResponse> getFeaturedProjects() {
        log.debug("[LIST_FEATURED_PROJECTS] Fetching featured projects");
        List<Project> projects = projectRepository.findFeaturedWithTags();
        log.debug("[LIST_FEATURED_PROJECTS] Found {} featured projects", projects.size());
        return projects.stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> searchByTitle(String title) {
        log.debug("[SEARCH_PROJECTS_TITLE] Searching by title - title={}", title);
        List<Project> projects = projectRepository.findByTitleWithTags(title);
        log.debug("[SEARCH_PROJECTS_TITLE] Found {} projects", projects.size());
        return projects.stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> searchByTechnology(String technology) {
        log.debug("[SEARCH_PROJECTS_TECH] Searching by technology - technology={}", technology);
        List<Project> projects = projectRepository.findByTechnologyWithTags(technology);
        log.debug("[SEARCH_PROJECTS_TECH] Found {} projects", projects.size());
        return projects.stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public void updateImageUrls(Long id, String imageUrl, String thumbnailUrl) {
        log.debug("[UPDATE_IMAGE_URLS] Updating image URLs - projectId={}, imageUrl={}, thumbnailUrl={}",
                id, imageUrl, thumbnailUrl);

        Project project = findOrThrow(projectRepository.findById(id), "Project", "id", id);

        project.setImageUrl(imageUrl);
        project.setThumbnailUrl(thumbnailUrl);
        projectRepository.save(project);

        log.info("[UPDATE_IMAGE_URLS] Image URLs updated - projectId={}", id);
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public ImageUploadResponse uploadAndAssignProjectImage(Long id, MultipartFile file) {
        log.info("[UPLOAD_ASSIGN_IMAGE] Starting image upload and assignment - projectId={}, fileName={}",
                id, file.getOriginalFilename());

        // Upload and optimize image
        ImageUploadResponse response = imageService.uploadProjectImage(id, file);

        // Update project with image URLs
        updateImageUrls(id, response.getImageUrl(), response.getThumbnailUrl());

        log.info("[UPLOAD_ASSIGN_IMAGE] Image uploaded and assigned - projectId={}, imageUrl={}",
                id, response.getImageUrl());

        return response;
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public void deleteProjectImage(Long id) {
        log.info("[DELETE_PROJECT_IMAGE] Starting image deletion - projectId={}", id);

        // Delete physical files
        imageService.deleteProjectImage(id);

        // Clear image URLs in database
        updateImageUrls(id, null, null);

        log.info("[DELETE_PROJECT_IMAGE] Image deleted and URLs cleared - projectId={}", id);
    }

    // ========== Multi-image Management ==========

    @Override
    @Transactional
    public ProjectImageResponse addImageToProject(Long projectId, MultipartFile file) {
        return addImageToProject(projectId, file, null, null);
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public ProjectImageResponse addImageToProject(Long projectId, MultipartFile file, String altText, String caption) {
        log.info("[ADD_PROJECT_IMAGE] Adding image - projectId={}, fileName={}",
                projectId, file.getOriginalFilename());

        Project project = findOrThrow(projectRepository.findById(projectId), "Project", "id", projectId);

        int currentCount = projectImageRepository.countByProjectId(projectId);
        if (currentCount >= MAX_IMAGES_PER_PROJECT) {
            log.warn("[ADD_PROJECT_IMAGE] Image limit reached - projectId={}, count={}", projectId, currentCount);
            throw new IllegalStateException("Maximum " + MAX_IMAGES_PER_PROJECT + " images allowed per project");
        }

        // Step 1: Prepare image (save temp file, generate URLs)
        PreparedImageInfo preparedImage = imageService.prepareCarouselImage(projectId, currentCount, file);

        // Step 2: Create and save entity with PROCESSING status
        ProjectImage projectImage = new ProjectImage(
                project,
                preparedImage.getImageUrl(),
                preparedImage.getThumbnailUrl(),
                altText,
                caption
        );
        projectImage.setDisplayOrder(currentCount);
        projectImage.setStatus(ImageStatus.PROCESSING);

        project.addImage(projectImage);
        projectRepository.save(project);

        // Find the saved image to get the generated ID
        ProjectImage savedImage = findImageByUrl(project.getImages(), preparedImage.getImageUrl(),
                ProjectImage::getImageUrl, "ProjectImage");

        // Step 3: Publish event with the saved entity ID
        ImageProcessingEvent event = ImageProcessingEvent.forCarousel(
                projectId,
                savedImage.getId(),
                currentCount,
                preparedImage.getTempFilePath(),
                preparedImage.getOptimizedFilePath(),
                preparedImage.getThumbnailFilePath()
        );
        eventPublisher.publishImageEvent(event);

        // Set as primary if no primary exists (handles race conditions by checking after save)
        ensureSinglePrimaryImage(projectId, savedImage);

        log.info("[ADD_PROJECT_IMAGE] Success - projectId={}, imageId={}, status=PROCESSING",
                projectId, savedImage.getId());
        return projectImageMapper.toResponse(savedImage);
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public void removeImageFromProject(Long projectId, Long imageId) {
        log.info("[REMOVE_PROJECT_IMAGE] Removing image - projectId={}, imageId={}", projectId, imageId);

        Project project = findOrThrow(projectRepository.findById(projectId), "Project", "id", projectId);

        ProjectImage imageToRemove = project.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ProjectImage", "id", imageId));

        boolean wasPrimary = imageToRemove.isPrimary();
        int removedOrder = imageToRemove.getDisplayOrder();

        // Delete physical files
        imageService.deleteProjectImageByUrl(imageToRemove.getImageUrl());

        // Remove from project
        project.removeImage(imageToRemove);
        projectRepository.save(project);

        // Decrement order for remaining images
        projectImageRepository.decrementOrderAfter(projectId, removedOrder);

        // If removed image was primary, set first remaining image as primary
        if (wasPrimary && !project.getImages().isEmpty()) {
            ProjectImage newPrimary = project.getImages().stream()
                    .min(Comparator.comparing(ProjectImage::getDisplayOrder))
                    .orElse(null);
            if (newPrimary != null) {
                newPrimary.setPrimary(true);
                projectImageRepository.save(newPrimary);
            }
        }

        log.info("[REMOVE_PROJECT_IMAGE] Success - projectId={}, imageId={}", projectId, imageId);
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public ProjectImageResponse updateProjectImage(Long projectId, Long imageId, UpdateProjectImageRequest request) {
        log.info("[UPDATE_PROJECT_IMAGE] Updating image - projectId={}, imageId={}", projectId, imageId);

        ProjectImage image = findOrThrow(
                projectImageRepository.findByIdAndProjectId(imageId, projectId), "ProjectImage", "id", imageId);

        projectImageMapper.updateFromRequest(request, image);
        ProjectImage saved = projectImageRepository.save(image);

        log.info("[UPDATE_PROJECT_IMAGE] Success - projectId={}, imageId={}", projectId, imageId);
        return projectImageMapper.toResponse(saved);
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public void setPrimaryImage(Long projectId, Long imageId) {
        log.info("[SET_PRIMARY_IMAGE] Setting primary - projectId={}, imageId={}", projectId, imageId);

        ProjectImage newPrimary = findOrThrow(
                projectImageRepository.findByIdAndProjectId(imageId, projectId), "ProjectImage", "id", imageId);

        // Clear current primary
        projectImageRepository.clearPrimaryForProject(projectId);

        // Set new primary
        newPrimary.setPrimary(true);
        projectImageRepository.save(newPrimary);

        log.info("[SET_PRIMARY_IMAGE] Success - projectId={}, imageId={}", projectId, imageId);
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    @Transactional
    public void reorderImages(Long projectId, ReorderProjectImagesRequest request) {
        log.info("[REORDER_IMAGES] Reordering - projectId={}, count={}", projectId, request.getImageIds().size());

        Project project = findOrThrow(projectRepository.findById(projectId), "Project", "id", projectId);

        List<Long> imageIds = request.getImageIds();
        int order = 0;
        for (Long id : imageIds) {
            ProjectImage image = project.getImages().stream()
                    .filter(img -> img.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("ProjectImage", "id", id));
            image.setDisplayOrder(order++);
        }

        projectRepository.save(project);
        log.info("[REORDER_IMAGES] Success - projectId={}", projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectImageResponse> getProjectImages(Long projectId) {
        log.debug("[GET_PROJECT_IMAGES] Fetching images - projectId={}", projectId);

        if (!projectRepository.existsById(projectId)) {
            log.warn("[GET_PROJECT_IMAGES] Project not found - projectId={}", projectId);
            throw new ResourceNotFoundException("Project", "id", projectId);
        }

        List<ProjectImage> images = projectImageRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);
        log.debug("[GET_PROJECT_IMAGES] Found {} images - projectId={}", images.size(), projectId);
        return projectImageMapper.toResponseList(images);
    }

    @Override
    @Transactional(readOnly = true)
    public ImageStatus getImageStatus(Long projectId, Long imageId) {
        log.debug("[GET_IMAGE_STATUS] Checking status - projectId={}, imageId={}", projectId, imageId);

        ProjectImage image = findOrThrow(
                projectImageRepository.findByIdAndProjectId(imageId, projectId), "ProjectImage", "id", imageId);

        log.debug("[GET_IMAGE_STATUS] Status retrieved - projectId={}, imageId={}, status={}",
                projectId, imageId, image.getStatus());
        return image.getStatus();
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    public void reorderProjects(ReorderRequest request) {
        log.debug("[REORDER_PROJECTS] Reordering projects - count={}", request.getOrderedIds().size());

        List<Long> orderedIds = request.getOrderedIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            Long projectId = orderedIds.get(i);
            Project project = findOrThrow(projectRepository.findById(projectId), "Project", "id", projectId);
            project.setDisplayOrder(i);
            projectRepository.save(project);
        }

        log.info("[REORDER_PROJECTS] Projects reordered - count={}", orderedIds.size());
    }

    // ========== Private Helper Methods ==========

    /**
     * Ensures exactly one primary image exists for a project.
     * If no primary image exists, sets the given image as primary.
     * If multiple primary images exist (due to race condition), keeps only the one with lowest displayOrder.
     */
    private void ensureSinglePrimaryImage(Long projectId, ProjectImage newImage) {
        List<ProjectImage> allImages = projectImageRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);

        List<ProjectImage> primaryImages = allImages.stream()
                .filter(ProjectImage::isPrimary)
                .toList();

        if (primaryImages.isEmpty()) {
            // No primary image - set the new image as primary
            newImage.setPrimary(true);
            projectImageRepository.save(newImage);
            log.debug("[ENSURE_PRIMARY] Set new image as primary - projectId={}, imageId={}",
                    projectId, newImage.getId());
        } else if (primaryImages.size() > 1) {
            // Multiple primary images (race condition) - keep only the first one (lowest displayOrder)
            ProjectImage keepPrimary = primaryImages.stream()
                    .min(Comparator.comparing(ProjectImage::getDisplayOrder))
                    .orElse(primaryImages.get(0));

            for (ProjectImage img : primaryImages) {
                if (!img.getId().equals(keepPrimary.getId())) {
                    img.setPrimary(false);
                    projectImageRepository.save(img);
                    log.debug("[ENSURE_PRIMARY] Removed duplicate primary flag - projectId={}, imageId={}",
                            projectId, img.getId());
                }
            }
        }
        // If exactly one primary image exists, do nothing
    }

    /**
     * Validates that all requested tag IDs were found in the database.
     * @param requestedIds The IDs requested by the user
     * @param foundTags The tags actually found in the database
     * @throws ResourceNotFoundException if any tag ID was not found
     */
    private void validateAllTagsFound(Set<Long> requestedIds, List<Tag> foundTags) {
        if (foundTags.size() != requestedIds.size()) {
            Set<Long> foundIds = foundTags.stream()
                    .map(Tag::getId)
                    .collect(Collectors.toSet());
            Long missingId = requestedIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElse(null);
            if (missingId != null) {
                throw new ResourceNotFoundException("Tag", "id", missingId);
            }
        }
    }
}
