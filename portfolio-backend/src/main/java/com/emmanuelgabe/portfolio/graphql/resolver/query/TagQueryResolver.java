package com.emmanuelgabe.portfolio.graphql.resolver.query;

import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL Query Resolver for Tag operations.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TagQueryResolver {

    private final TagService tagService;

    @QueryMapping
    public List<TagResponse> tags() {
        log.debug("[GRAPHQL_QUERY] tags");
        return tagService.getAllTags();
    }

    @QueryMapping
    public TagResponse tag(@Argument Long id) {
        log.debug("[GRAPHQL_QUERY] tag - id={}", id);
        return tagService.getTagById(id);
    }
}
