package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;
import com.emmanuelgabe.portfolio.entity.Project;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Project entity and DTOs
 * Handles conversion between Project entity and various request/response DTOs
 */
@Mapper(componentModel = "spring", uses = {TagMapper.class})
public interface ProjectMapper {

    /**
     * Convert Project entity to ProjectResponse DTO
     * @param project Project entity
     * @return ProjectResponse DTO
     */
    ProjectResponse toResponse(Project project);

    /**
     * Convert CreateProjectRequest DTO to Project entity
     * @param request CreateProjectRequest DTO
     * @return Project entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tags", ignore = true)
    Project toEntity(CreateProjectRequest request);

    /**
     * Update existing Project entity with values from UpdateProjectRequest
     * Only updates non-null fields from the request
     * @param request UpdateProjectRequest DTO
     * @param project Existing Project entity to update
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tags", ignore = true)
    void updateEntityFromRequest(UpdateProjectRequest request, @MappingTarget Project project);
}
