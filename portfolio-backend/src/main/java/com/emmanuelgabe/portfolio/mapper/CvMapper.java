package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.CvResponse;
import com.emmanuelgabe.portfolio.entity.Cv;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for CV entity and DTOs
 * Handles conversion between Cv entity and CvResponse DTO
 */
@Mapper(componentModel = "spring")
public interface CvMapper {

    /**
     * Convert Cv entity to CvResponse DTO
     * @param cv Cv entity
     * @return CvResponse DTO
     */
    CvResponse toResponse(Cv cv);
}
