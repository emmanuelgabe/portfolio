package com.emmanuelgabe.portfolio.graphql.resolver.mutation;

import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.graphql.input.CreateTagInput;
import com.emmanuelgabe.portfolio.graphql.input.UpdateTagInput;
import com.emmanuelgabe.portfolio.graphql.mapper.GraphQLInputMapper;
import com.emmanuelgabe.portfolio.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL Mutation Resolver for Tag operations.
 * All mutations require ADMIN role.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TagMutationResolver {

    private final TagService tagService;
    private final GraphQLInputMapper inputMapper;

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TagResponse createTag(@Argument CreateTagInput input) {
        log.info("[GRAPHQL_MUTATION] createTag - name={}", input.getName());
        return tagService.createTag(inputMapper.toCreateTagRequest(input));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TagResponse updateTag(@Argument Long id, @Argument UpdateTagInput input) {
        log.info("[GRAPHQL_MUTATION] updateTag - id={}", id);
        return tagService.updateTag(id, inputMapper.toUpdateTagRequest(input));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean deleteTag(@Argument Long id) {
        log.info("[GRAPHQL_MUTATION] deleteTag - id={}", id);
        tagService.deleteTag(id);
        return true;
    }
}
