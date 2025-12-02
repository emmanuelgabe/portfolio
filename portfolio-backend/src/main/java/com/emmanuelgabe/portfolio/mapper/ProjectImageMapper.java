package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.ProjectImageResponse;
import com.emmanuelgabe.portfolio.dto.UpdateProjectImageRequest;
import com.emmanuelgabe.portfolio.entity.ProjectImage;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Collection;
import java.util.List;

/**
 * MapStruct mapper for ProjectImage entity and DTOs.
 */
@Mapper(componentModel = "spring")
public interface ProjectImageMapper {

    /**
     * Convert ProjectImage entity to ProjectImageResponse DTO.
     */
    ProjectImageResponse toResponse(ProjectImage projectImage);

    /**
     * Convert collection of ProjectImage entities to list of responses.
     */
    List<ProjectImageResponse> toResponseList(Collection<ProjectImage> images);

    /**
     * Update ProjectImage entity from UpdateProjectImageRequest.
     * Only updates altText and caption fields, ignoring null values.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "thumbnailUrl", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "primary", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    void updateFromRequest(UpdateProjectImageRequest request, @MappingTarget ProjectImage image);
}
