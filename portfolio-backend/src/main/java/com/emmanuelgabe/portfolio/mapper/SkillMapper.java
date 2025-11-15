package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSkillRequest;
import com.emmanuelgabe.portfolio.entity.Skill;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Skill entity and DTOs
 * Handles conversion between Skill entity and various request/response DTOs
 */
@Mapper(componentModel = "spring")
public interface SkillMapper {

    /**
     * Convert Skill entity to SkillResponse DTO
     * @param skill Skill entity
     * @return SkillResponse DTO
     */
    @Mapping(target = "categoryDisplayName", source = "category.displayName")
    SkillResponse toResponse(Skill skill);

    /**
     * Convert CreateSkillRequest DTO to Skill entity
     * @param request CreateSkillRequest DTO
     * @return Skill entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Skill toEntity(CreateSkillRequest request);

    /**
     * Update existing Skill entity with values from UpdateSkillRequest
     * Only updates non-null fields from the request
     * @param request UpdateSkillRequest DTO
     * @param skill Existing Skill entity to update
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateSkillRequest request, @MappingTarget Skill skill);
}
