package com.emmanuelgabe.portfolio.graphql.resolver.query;

import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.dto.search.ArticleSearchResult;
import com.emmanuelgabe.portfolio.dto.search.ProjectSearchResult;
import com.emmanuelgabe.portfolio.graphql.type.response.SearchResult;
import com.emmanuelgabe.portfolio.service.ArticleService;
import com.emmanuelgabe.portfolio.service.ProjectService;
import com.emmanuelgabe.portfolio.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GraphQL Query Resolver for unified search operations.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class SearchQueryResolver {

    private final SearchService searchService;
    private final ProjectService projectService;
    private final ArticleService articleService;

    @QueryMapping
    public SearchResult search(@Argument String query) {
        log.debug("[GRAPHQL_QUERY] search - query={}", query);

        if (query == null || query.isBlank()) {
            return SearchResult.builder()
                    .projects(List.of())
                    .articles(List.of())
                    .totalCount(0)
                    .build();
        }

        // Search using the SearchService (Elasticsearch or JPA fallback)
        List<ProjectSearchResult> projectResults = searchService.searchProjects(query);
        List<ArticleSearchResult> articleResults = searchService.searchArticles(query);

        // Convert search results to full responses using batch loading (N+1 fix)
        Set<Long> projectIds = projectResults.stream()
                .map(ProjectSearchResult::getId)
                .collect(Collectors.toSet());
        List<ProjectResponse> projects = projectService.getProjectsByIds(projectIds);

        Set<Long> articleIds = articleResults.stream()
                .map(ArticleSearchResult::getId)
                .collect(Collectors.toSet());
        List<ArticleResponse> articles = articleService.getArticlesByIds(articleIds);

        int totalCount = projects.size() + articles.size();

        return SearchResult.builder()
                .projects(projects)
                .articles(articles)
                .totalCount(totalCount)
                .build();
    }
}
