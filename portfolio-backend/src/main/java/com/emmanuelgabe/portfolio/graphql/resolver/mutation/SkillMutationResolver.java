package com.emmanuelgabe.portfolio.graphql.resolver.mutation;

import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.graphql.input.CreateSkillInput;
import com.emmanuelgabe.portfolio.graphql.input.UpdateSkillInput;
import com.emmanuelgabe.portfolio.graphql.mapper.GraphQLInputMapper;
import com.emmanuelgabe.portfolio.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL Mutation Resolver for Skill operations.
 * All mutations require ADMIN role.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class SkillMutationResolver {

    private final SkillService skillService;
    private final GraphQLInputMapper inputMapper;

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SkillResponse createSkill(@Argument CreateSkillInput input) {
        log.info("[GRAPHQL_MUTATION] createSkill - name={}", input.getName());
        return skillService.createSkill(inputMapper.toCreateSkillRequest(input));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SkillResponse updateSkill(@Argument Long id, @Argument UpdateSkillInput input) {
        log.info("[GRAPHQL_MUTATION] updateSkill - id={}", id);
        return skillService.updateSkill(id, inputMapper.toUpdateSkillRequest(input));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean deleteSkill(@Argument Long id) {
        log.info("[GRAPHQL_MUTATION] deleteSkill - id={}", id);
        skillService.deleteSkill(id);
        return true;
    }
}
