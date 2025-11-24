package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.dto.article.CreateArticleRequest;
import com.emmanuelgabe.portfolio.dto.article.UpdateArticleRequest;
import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.service.MarkdownService;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MapStruct mapper for Article entity conversions.
 * Handles conversion between entity and DTOs, including Markdown to HTML rendering.
 */
@Mapper(componentModel = "spring", uses = {TagMapper.class, ArticleImageMapper.class})
public abstract class ArticleMapper {

    protected MarkdownService markdownService;

    @Autowired
    public void setMarkdownService(MarkdownService markdownService) {
        this.markdownService = markdownService;
    }

    /**
     * Converts Article entity to ArticleResponse DTO.
     * Renders Markdown content to HTML for contentHtml field.
     *
     * @param article the Article entity
     * @return the ArticleResponse DTO
     */
    @Mapping(target = "authorName", source = "author.username")
    @Mapping(target = "contentHtml", expression = "java(markdownService.renderToHtml(article.getContent()))")
    public abstract ArticleResponse toResponse(Article article);

    /**
     * Converts CreateArticleRequest DTO to Article entity.
     * Note: author, tags, and timestamps are set by the service layer.
     *
     * @param request the CreateArticleRequest DTO
     * @return the Article entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "readingTimeMinutes", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "images", ignore = true)
    public abstract Article toEntity(CreateArticleRequest request);

    /**
     * Updates an existing Article entity with values from UpdateArticleRequest.
     * Only updates non-null fields from the request.
     *
     * @param request the UpdateArticleRequest DTO
     * @param article the target Article entity to update
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "readingTimeMinutes", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "images", ignore = true)
    public abstract void updateEntityFromRequest(UpdateArticleRequest request, @MappingTarget Article article);
}
