package com.emmanuelgabe.portfolio.graphql.resolver.mutation;

import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.graphql.input.CreateProjectInput;
import com.emmanuelgabe.portfolio.graphql.input.UpdateProjectInput;
import com.emmanuelgabe.portfolio.graphql.mapper.GraphQLInputMapper;
import com.emmanuelgabe.portfolio.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL Mutation Resolver for Project operations.
 * All mutations require ADMIN role.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ProjectMutationResolver {

    private final ProjectService projectService;
    private final GraphQLInputMapper inputMapper;

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectResponse createProject(@Argument CreateProjectInput input) {
        log.info("[GRAPHQL_MUTATION] createProject - title={}", input.getTitle());
        return projectService.createProject(inputMapper.toCreateProjectRequest(input));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectResponse updateProject(@Argument Long id, @Argument UpdateProjectInput input) {
        log.info("[GRAPHQL_MUTATION] updateProject - id={}", id);
        return projectService.updateProject(id, inputMapper.toUpdateProjectRequest(input));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean deleteProject(@Argument Long id) {
        log.info("[GRAPHQL_MUTATION] deleteProject - id={}", id);
        projectService.deleteProject(id);
        return true;
    }
}
