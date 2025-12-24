package com.emmanuelgabe.portfolio.graphql.resolver.mutation;

import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.graphql.input.CreateArticleInput;
import com.emmanuelgabe.portfolio.graphql.input.UpdateArticleInput;
import com.emmanuelgabe.portfolio.graphql.mapper.GraphQLInputMapper;
import com.emmanuelgabe.portfolio.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

/**
 * GraphQL Mutation Resolver for Article operations.
 * All mutations require ADMIN role.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ArticleMutationResolver {

    private final ArticleService articleService;
    private final GraphQLInputMapper inputMapper;

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ArticleResponse createArticle(@Argument CreateArticleInput input) {
        log.info("[GRAPHQL_MUTATION] createArticle - title={}", input.getTitle());
        String username = getCurrentUsername();
        return articleService.createArticle(inputMapper.toCreateArticleRequest(input), username);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ArticleResponse updateArticle(@Argument Long id, @Argument UpdateArticleInput input) {
        log.info("[GRAPHQL_MUTATION] updateArticle - id={}", id);
        return articleService.updateArticle(id, inputMapper.toUpdateArticleRequest(input));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean deleteArticle(@Argument Long id) {
        log.info("[GRAPHQL_MUTATION] deleteArticle - id={}", id);
        articleService.deleteArticle(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ArticleResponse publishArticle(@Argument Long id) {
        log.info("[GRAPHQL_MUTATION] publishArticle - id={}", id);
        return articleService.publishArticle(id);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ArticleResponse unpublishArticle(@Argument Long id) {
        log.info("[GRAPHQL_MUTATION] unpublishArticle - id={}", id);
        return articleService.unpublishArticle(id);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }
}
