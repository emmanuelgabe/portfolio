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
    @Mapping(target = "showMonths", defaultValue = "true")
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
    @Mapping(target = "company", expression = "java(normalizeEmptyString(request.getCompany(), experience.getCompany()))")
    @Mapping(target = "role", expression = "java(normalizeEmptyString(request.getRole(), experience.getRole()))")
    void updateEntityFromRequest(UpdateExperienceRequest request, @MappingTarget Experience experience);

    /**
     * Normalize empty strings to null for optional fields
     * If the new value is an empty/blank string, return null
     * If the new value is null, keep the existing value (partial update behavior)
     * @param newValue The new value from the request
     * @param existingValue The existing value in the entity
     * @return The normalized value
     */
    default String normalizeEmptyString(String newValue, String existingValue) {
        if (newValue == null) {
            return existingValue;
        }
        if (newValue.isBlank()) {
            return null;
        }
        return newValue;
    }

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
