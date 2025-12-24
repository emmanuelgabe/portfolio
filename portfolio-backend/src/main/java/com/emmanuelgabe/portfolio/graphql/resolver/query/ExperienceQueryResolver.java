package com.emmanuelgabe.portfolio.graphql.resolver.query;

import com.emmanuelgabe.portfolio.dto.ExperienceResponse;
import com.emmanuelgabe.portfolio.entity.ExperienceType;
import com.emmanuelgabe.portfolio.graphql.input.filter.ExperienceFilterInput;
import com.emmanuelgabe.portfolio.graphql.type.response.ExperienceTypeGroup;
import com.emmanuelgabe.portfolio.service.ExperienceService;
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
 * GraphQL Query Resolver for Experience operations.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ExperienceQueryResolver {

    private final ExperienceService experienceService;

    @QueryMapping
    public List<ExperienceResponse> experiences(@Argument ExperienceFilterInput filter) {
        log.debug("[GRAPHQL_QUERY] experiences - filter={}", filter);

        if (filter != null) {
            // Filter by type
            if (filter.getType() != null) {
                return experienceService.getExperiencesByType(filter.getType());
            }

            // Filter by ongoing status
            if (filter.getOngoing() != null && filter.getOngoing()) {
                return experienceService.getOngoingExperiences();
            }
        }

        return experienceService.getAllExperiences();
    }

    @QueryMapping
    public ExperienceResponse experience(@Argument Long id) {
        log.debug("[GRAPHQL_QUERY] experience - id={}", id);
        return experienceService.getExperienceById(id);
    }

    @QueryMapping
    public List<ExperienceTypeGroup> experiencesByType() {
        log.debug("[GRAPHQL_QUERY] experiencesByType");

        List<ExperienceResponse> allExperiences = experienceService.getAllExperiences();

        // Group experiences by type
        Map<ExperienceType, List<ExperienceResponse>> grouped = allExperiences.stream()
                .collect(Collectors.groupingBy(ExperienceResponse::getType));

        // Return groups for all types with experiences
        return Arrays.stream(ExperienceType.values())
                .map(type -> new ExperienceTypeGroup(type, grouped.getOrDefault(type, List.of())))
                .filter(group -> !group.getExperiences().isEmpty())
                .toList();
    }

    @QueryMapping
    public List<ExperienceResponse> recentExperiences(@Argument Integer limit) {
        log.debug("[GRAPHQL_QUERY] recentExperiences - limit={}", limit);
        int effectiveLimit = limit != null ? Math.min(limit, 50) : 3;
        return experienceService.getRecentExperiences(effectiveLimit);
    }
}
