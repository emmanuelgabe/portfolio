package com.emmanuelgabe.portfolio.graphql.resolver.query;

import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.graphql.input.PageInput;
import com.emmanuelgabe.portfolio.graphql.input.ProjectSortField;
import com.emmanuelgabe.portfolio.graphql.input.ProjectSortInput;
import com.emmanuelgabe.portfolio.graphql.input.SortDirection;
import com.emmanuelgabe.portfolio.graphql.input.filter.ProjectFilterInput;
import com.emmanuelgabe.portfolio.graphql.type.connection.Connection;
import com.emmanuelgabe.portfolio.graphql.util.ConnectionUtil;
import com.emmanuelgabe.portfolio.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Comparator;
import java.util.List;

/**
 * GraphQL Query Resolver for Project operations.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ProjectQueryResolver {

    private final ProjectService projectService;

    @QueryMapping
    public Connection<ProjectResponse> projects(
            @Argument ProjectFilterInput filter,
            @Argument ProjectSortInput sort,
            @Argument PageInput page) {

        log.debug("[GRAPHQL_QUERY] projects - filter={}, sort={}, page={}", filter, sort, page);

        List<ProjectResponse> allProjects = projectService.getAllProjects();

        // Apply filters
        List<ProjectResponse> filtered = applyFilters(allProjects, filter);

        // Apply sorting
        List<ProjectResponse> sorted = applySorting(filtered, sort);

        return ConnectionUtil.fromList(sorted, page, ProjectResponse::getId);
    }

    @QueryMapping
    public ProjectResponse project(@Argument Long id) {
        log.debug("[GRAPHQL_QUERY] project - id={}", id);
        return projectService.getProjectById(id);
    }

    @QueryMapping
    public List<ProjectResponse> featuredProjects() {
        log.debug("[GRAPHQL_QUERY] featuredProjects");
        return projectService.getFeaturedProjects();
    }

    private List<ProjectResponse> applyFilters(List<ProjectResponse> projects, ProjectFilterInput filter) {
        if (filter == null) {
            return projects;
        }

        return projects.stream()
                .filter(p -> {
                    // Filter by featured
                    if (filter.getFeatured() != null && p.isFeatured() != filter.getFeatured()) {
                        return false;
                    }

                    // Filter by search query (title or description)
                    if (filter.getSearchQuery() != null && !filter.getSearchQuery().isBlank()) {
                        String query = filter.getSearchQuery().toLowerCase();
                        boolean matchesTitle = p.getTitle() != null
                                && p.getTitle().toLowerCase().contains(query);
                        boolean matchesDescription = p.getDescription() != null
                                && p.getDescription().toLowerCase().contains(query);
                        if (!matchesTitle && !matchesDescription) {
                            return false;
                        }
                    }

                    // Filter by technology
                    if (filter.getTechnology() != null && !filter.getTechnology().isBlank()) {
                        String tech = filter.getTechnology().toLowerCase();
                        boolean matchesTech = p.getTechStack() != null
                                && p.getTechStack().toLowerCase().contains(tech);
                        if (!matchesTech) {
                            return false;
                        }
                    }

                    // Filter by tag IDs
                    if (filter.getTagIds() != null && !filter.getTagIds().isEmpty()) {
                        if (p.getTags() == null || p.getTags().isEmpty()) {
                            return false;
                        }
                        boolean hasMatchingTag = p.getTags().stream()
                                .anyMatch(tag -> filter.getTagIds().contains(tag.getId()));
                        if (!hasMatchingTag) {
                            return false;
                        }
                    }

                    return true;
                })
                .toList();
    }

    private List<ProjectResponse> applySorting(List<ProjectResponse> projects, ProjectSortInput sort) {
        if (sort == null) {
            // Default: sort by createdAt DESC
            return projects.stream()
                    .sorted(Comparator.comparing(ProjectResponse::getCreatedAt).reversed())
                    .toList();
        }

        ProjectSortField field = sort.getField() != null ? sort.getField() : ProjectSortField.CREATED_AT;
        SortDirection direction = sort.getDirection() != null ? sort.getDirection() : SortDirection.DESC;

        Comparator<ProjectResponse> comparator = switch (field) {
            case TITLE -> Comparator.comparing(ProjectResponse::getTitle,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case UPDATED_AT -> Comparator.comparing(ProjectResponse::getUpdatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case CREATED_AT -> Comparator.comparing(ProjectResponse::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        };

        if (direction == SortDirection.DESC) {
            comparator = comparator.reversed();
        }

        return projects.stream().sorted(comparator).toList();
    }
}
