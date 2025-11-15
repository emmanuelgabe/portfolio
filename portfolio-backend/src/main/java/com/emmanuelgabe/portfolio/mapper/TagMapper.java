package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.CreateTagRequest;
import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.dto.UpdateTagRequest;
import com.emmanuelgabe.portfolio.entity.Tag;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Tag entity and DTOs
 * Handles conversion between Tag entity and response DTOs
 */
@Mapper(componentModel = "spring")
public interface TagMapper {

    /**
     * Convert Tag entity to TagResponse DTO
     * @param tag Tag entity
     * @return TagResponse DTO
     */
    TagResponse toResponse(Tag tag);

    /**
     * Convert CreateTagRequest DTO to Tag entity
     * @param request CreateTagRequest DTO
     * @return Tag entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "projects", ignore = true)
    Tag toEntity(CreateTagRequest request);

    /**
     * Update existing Tag entity from UpdateTagRequest DTO
     * Only non-null fields from the request will be updated
     * @param request UpdateTagRequest DTO with fields to update
     * @param tag Existing Tag entity to update
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "projects", ignore = true)
    void updateEntityFromRequest(UpdateTagRequest request, @MappingTarget Tag tag);
}
