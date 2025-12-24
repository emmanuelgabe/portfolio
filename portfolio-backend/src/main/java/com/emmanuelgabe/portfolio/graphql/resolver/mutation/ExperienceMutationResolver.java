package com.emmanuelgabe.portfolio.graphql.resolver.mutation;

import com.emmanuelgabe.portfolio.dto.ExperienceResponse;
import com.emmanuelgabe.portfolio.graphql.input.CreateExperienceInput;
import com.emmanuelgabe.portfolio.graphql.input.UpdateExperienceInput;
import com.emmanuelgabe.portfolio.graphql.mapper.GraphQLInputMapper;
import com.emmanuelgabe.portfolio.service.ExperienceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL Mutation Resolver for Experience operations.
 * All mutations require ADMIN role.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ExperienceMutationResolver {

    private final ExperienceService experienceService;
    private final GraphQLInputMapper inputMapper;

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ExperienceResponse createExperience(@Argument CreateExperienceInput input) {
        log.info("[GRAPHQL_MUTATION] createExperience - company={}, role={}",
                input.getCompany(), input.getRole());
        return experienceService.createExperience(inputMapper.toCreateExperienceRequest(input));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ExperienceResponse updateExperience(@Argument Long id, @Argument UpdateExperienceInput input) {
        log.info("[GRAPHQL_MUTATION] updateExperience - id={}", id);
        return experienceService.updateExperience(id, inputMapper.toUpdateExperienceRequest(input));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean deleteExperience(@Argument Long id) {
        log.info("[GRAPHQL_MUTATION] deleteExperience - id={}", id);
        experienceService.deleteExperience(id);
        return true;
    }
}
