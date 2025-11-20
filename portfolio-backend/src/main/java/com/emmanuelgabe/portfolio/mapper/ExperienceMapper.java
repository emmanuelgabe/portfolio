package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.CreateExperienceRequest;
import com.emmanuelgabe.portfolio.dto.ExperienceResponse;
import com.emmanuelgabe.portfolio.dto.UpdateExperienceRequest;
import com.emmanuelgabe.portfolio.entity.Experience;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Experience entity and DTOs.
 * Handles conversion between Experience entity and various request/response DTOs.
 */
@Mapper(componentModel = "spring")
public interface ExperienceMapper {

    /**
     * Convert Experience entity to ExperienceResponse DTO
     * @param experience Experience entity
     * @return ExperienceResponse DTO
     */
    ExperienceResponse toResponse(Experience experience);

    /**
     * Convert CreateExperienceRequest DTO to Experience entity
     * @param request CreateExperienceRequest DTO
     * @return Experience entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Experience toEntity(CreateExperienceRequest request);

    /**
     * Update existing Experience entity with values from UpdateExperienceRequest
     * Only updates non-null fields from the request
     * @param request UpdateExperienceRequest DTO
     * @param experience Existing Experience entity to update
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateExperienceRequest request, @MappingTarget Experience experience);

    /**
     * After mapping, set the ongoing flag based on endDate
     * @param experience Source entity
     * @param response Target DTO
     */
    @AfterMapping
    default void setOngoingFlag(Experience experience, @MappingTarget ExperienceResponse response) {
        response.setOngoing(experience.isOngoing());
    }
}
