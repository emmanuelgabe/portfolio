package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.entity.ProjectImage;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Project entity and DTOs
 * Handles conversion between Project entity and various request/response DTOs
 */
@Mapper(componentModel = "spring", uses = {TagMapper.class, ProjectImageMapper.class})
public interface ProjectMapper {

    /**
     * Convert Project entity to ProjectResponse DTO.
     * imageUrl and thumbnailUrl are computed from primary image for backward compatibility.
     * @param project Project entity
     * @return ProjectResponse DTO
     */
    @Mapping(target = "imageUrl", source = "project", qualifiedByName = "getPrimaryImageUrl")
    @Mapping(target = "thumbnailUrl", source = "project", qualifiedByName = "getPrimaryThumbnailUrl")
    ProjectResponse toResponse(Project project);

    /**
     * Get the primary image URL from project.
     * Falls back to legacy imageUrl field if no images in collection.
     */
    @Named("getPrimaryImageUrl")
    default String getPrimaryImageUrl(Project project) {
        if (project.getImages() != null && !project.getImages().isEmpty()) {
            ProjectImage primary = project.getPrimaryImage();
            return primary != null ? primary.getImageUrl() : null;
        }
        return project.getImageUrl();
    }

    /**
     * Get the primary thumbnail URL from project.
     * Falls back to legacy thumbnailUrl field if no images in collection.
     */
    @Named("getPrimaryThumbnailUrl")
    default String getPrimaryThumbnailUrl(Project project) {
        if (project.getImages() != null && !project.getImages().isEmpty()) {
            ProjectImage primary = project.getPrimaryImage();
            return primary != null ? primary.getThumbnailUrl() : null;
        }
        return project.getThumbnailUrl();
    }

    /**
     * Convert CreateProjectRequest DTO to Project entity
     * @param request CreateProjectRequest DTO
     * @return Project entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "thumbnailUrl", ignore = true)
    @Mapping(target = "images", ignore = true)
    Project toEntity(CreateProjectRequest request);

    /**
     * Update existing Project entity with values from UpdateProjectRequest
     * Only updates non-null fields from the request, except for githubUrl and demoUrl which can be set to null
     * @param request UpdateProjectRequest DTO
     * @param project Existing Project entity to update
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "thumbnailUrl", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "githubUrl", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "demoUrl", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void updateEntityFromRequest(UpdateProjectRequest request, @MappingTarget Project project);
}
