package com.emmanuelgabe.portfolio.search.mapper;

import com.emmanuelgabe.portfolio.dto.search.ArticleSearchResult;
import com.emmanuelgabe.portfolio.dto.search.ExperienceSearchResult;
import com.emmanuelgabe.portfolio.dto.search.ProjectSearchResult;
import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.Experience;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.search.document.ArticleDocument;
import com.emmanuelgabe.portfolio.search.document.ExperienceDocument;
import com.emmanuelgabe.portfolio.search.document.ProjectDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * MapStruct mapper for converting between JPA entities, Elasticsearch documents,
 * and search result DTOs.
 */
@Mapper(componentModel = "spring")
public interface SearchDocumentMapper {

    // ========== Entity to Document mappings ==========

    @Mapping(target = "tags", expression = "java(mapTags(article.getTags()))")
    @Mapping(target = "authorName", source = "author.username")
    ArticleDocument toDocument(Article article);

    @Mapping(target = "tags", expression = "java(mapTags(project.getTags()))")
    ProjectDocument toDocument(Project project);

    @Mapping(target = "type", expression = "java(experience.getType().name())")
    ExperienceDocument toDocument(Experience experience);

    // ========== Document to SearchResult mappings ==========

    ArticleSearchResult toSearchResult(ArticleDocument document);

    ProjectSearchResult toSearchResult(ProjectDocument document);

    ExperienceSearchResult toSearchResult(ExperienceDocument document);

    // ========== Entity to SearchResult mappings (for JPA fallback) ==========

    @Mapping(target = "tags", expression = "java(mapTags(article.getTags()))")
    ArticleSearchResult toSearchResult(Article article);

    @Mapping(target = "tags", expression = "java(mapTags(project.getTags()))")
    ProjectSearchResult toSearchResult(Project project);

    @Mapping(target = "type", expression = "java(experience.getType().name())")
    ExperienceSearchResult toSearchResult(Experience experience);

    // ========== Helper methods ==========

    default List<String> mapTags(Set<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags.stream()
                .map(Tag::getName)
                .toList();
    }
}
