package com.emmanuelgabe.portfolio.graphql.resolver.query;

import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.graphql.input.ArticleSortField;
import com.emmanuelgabe.portfolio.graphql.input.ArticleSortInput;
import com.emmanuelgabe.portfolio.graphql.input.PageInput;
import com.emmanuelgabe.portfolio.graphql.input.SortDirection;
import com.emmanuelgabe.portfolio.graphql.input.filter.ArticleFilterInput;
import com.emmanuelgabe.portfolio.graphql.type.connection.Connection;
import com.emmanuelgabe.portfolio.graphql.util.ConnectionUtil;
import com.emmanuelgabe.portfolio.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Comparator;
import java.util.List;

/**
 * GraphQL Query Resolver for Article operations.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ArticleQueryResolver {

    private final ArticleService articleService;

    @QueryMapping
    public Connection<ArticleResponse> articles(
            @Argument ArticleFilterInput filter,
            @Argument ArticleSortInput sort,
            @Argument PageInput page) {

        log.debug("[GRAPHQL_QUERY] articles - filter={}, sort={}, page={}", filter, sort, page);

        // For public queries, only return published articles
        // Admin filtering by draft status would require authentication check
        List<ArticleResponse> allArticles = articleService.getAllPublished();

        // Apply filters
        List<ArticleResponse> filtered = applyFilters(allArticles, filter);

        // Apply sorting
        List<ArticleResponse> sorted = applySorting(filtered, sort);

        return ConnectionUtil.fromList(sorted, page, ArticleResponse::getId);
    }

    @QueryMapping
    public ArticleResponse article(@Argument Long id) {
        log.debug("[GRAPHQL_QUERY] article - id={}", id);
        return articleService.getById(id);
    }

    @QueryMapping
    public ArticleResponse articleBySlug(@Argument String slug) {
        log.debug("[GRAPHQL_QUERY] articleBySlug - slug={}", slug);
        return articleService.getBySlug(slug);
    }

    private List<ArticleResponse> applyFilters(List<ArticleResponse> articles, ArticleFilterInput filter) {
        if (filter == null) {
            return articles;
        }

        return articles.stream()
                .filter(a -> {
                    // Filter by search query (title, content, excerpt)
                    if (filter.getSearchQuery() != null && !filter.getSearchQuery().isBlank()) {
                        String query = filter.getSearchQuery().toLowerCase();
                        boolean matchesTitle = a.getTitle() != null
                                && a.getTitle().toLowerCase().contains(query);
                        boolean matchesContent = a.getContent() != null
                                && a.getContent().toLowerCase().contains(query);
                        boolean matchesExcerpt = a.getExcerpt() != null
                                && a.getExcerpt().toLowerCase().contains(query);
                        if (!matchesTitle && !matchesContent && !matchesExcerpt) {
                            return false;
                        }
                    }

                    // Filter by tag IDs
                    if (filter.getTagIds() != null && !filter.getTagIds().isEmpty()) {
                        if (a.getTags() == null || a.getTags().isEmpty()) {
                            return false;
                        }
                        boolean hasMatchingTag = a.getTags().stream()
                                .anyMatch(tag -> filter.getTagIds().contains(tag.getId()));
                        if (!hasMatchingTag) {
                            return false;
                        }
                    }

                    return true;
                })
                .toList();
    }

    private List<ArticleResponse> applySorting(List<ArticleResponse> articles, ArticleSortInput sort) {
        if (sort == null) {
            // Default: sort by publishedAt DESC
            return articles.stream()
                    .sorted(Comparator.comparing(ArticleResponse::getPublishedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .toList();
        }

        ArticleSortField field = sort.getField() != null ? sort.getField() : ArticleSortField.PUBLISHED_AT;
        SortDirection direction = sort.getDirection() != null ? sort.getDirection() : SortDirection.DESC;

        Comparator<ArticleResponse> comparator = switch (field) {
            case TITLE -> Comparator.comparing(ArticleResponse::getTitle,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case CREATED_AT -> Comparator.comparing(ArticleResponse::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case READING_TIME -> Comparator.comparing(ArticleResponse::getReadingTimeMinutes,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case PUBLISHED_AT -> Comparator.comparing(ArticleResponse::getPublishedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        };

        if (direction == SortDirection.DESC) {
            comparator = comparator.reversed();
        }

        return articles.stream().sorted(comparator).toList();
    }
}
