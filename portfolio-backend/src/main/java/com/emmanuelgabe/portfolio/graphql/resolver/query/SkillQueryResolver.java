package com.emmanuelgabe.portfolio.graphql.resolver.query;

import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.graphql.input.filter.SkillFilterInput;
import com.emmanuelgabe.portfolio.graphql.type.response.SkillCategoryGroup;
import com.emmanuelgabe.portfolio.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GraphQL Query Resolver for Skill operations.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class SkillQueryResolver {

    private final SkillService skillService;

    @QueryMapping
    public List<SkillResponse> skills(@Argument SkillFilterInput filter) {
        log.debug("[GRAPHQL_QUERY] skills - filter={}", filter);

        if (filter != null && filter.getCategory() != null) {
            return skillService.getSkillsByCategory(filter.getCategory());
        }

        return skillService.getAllSkills();
    }

    @QueryMapping
    public SkillResponse skill(@Argument Long id) {
        log.debug("[GRAPHQL_QUERY] skill - id={}", id);
        return skillService.getSkillById(id);
    }

    @QueryMapping
    public List<SkillCategoryGroup> skillsByCategory() {
        log.debug("[GRAPHQL_QUERY] skillsByCategory");

        List<SkillResponse> allSkills = skillService.getAllSkills();

        // Group skills by category
        Map<SkillCategory, List<SkillResponse>> grouped = allSkills.stream()
                .collect(Collectors.groupingBy(SkillResponse::getCategory));

        // Return groups for all categories (including empty ones for consistency)
        return Arrays.stream(SkillCategory.values())
                .map(category -> new SkillCategoryGroup(
                        category,
                        category.getDisplayName(),
                        grouped.getOrDefault(category, List.of())
                ))
                .filter(group -> !group.getSkills().isEmpty())
                .toList();
    }
}
