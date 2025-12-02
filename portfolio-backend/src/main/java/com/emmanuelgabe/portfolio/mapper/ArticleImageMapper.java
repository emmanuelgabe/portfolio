package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.article.ArticleImageResponse;
import com.emmanuelgabe.portfolio.entity.ArticleImage;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for ArticleImage entity conversions.
 */
@Mapper(componentModel = "spring")
public interface ArticleImageMapper {

    /**
     * Converts ArticleImage entity to ArticleImageResponse DTO.
     *
     * @param articleImage the ArticleImage entity
     * @return the ArticleImageResponse DTO
     */
    ArticleImageResponse toResponse(ArticleImage articleImage);
}
