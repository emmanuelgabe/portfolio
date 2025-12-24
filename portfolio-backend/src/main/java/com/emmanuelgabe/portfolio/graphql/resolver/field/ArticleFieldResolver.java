package com.emmanuelgabe.portfolio.graphql.resolver.field;

import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.dto.article.ArticleImageResponse;
import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Field resolver for Article type.
 * Uses DataLoaders to batch load tags and images, preventing N+1 queries.
 *
 * Note: These resolvers are only called if the parent object's field is null.
 * When the service already populates these fields, Spring GraphQL uses the
 * existing values without calling these resolvers.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ArticleFieldResolver {

    /**
     * Resolve tags for an article using DataLoader.
     * Batches multiple tag requests into a single database query.
     */
    @SchemaMapping(typeName = "Article", field = "tags")
    public CompletableFuture<List<TagResponse>> tags(
            ArticleResponse article,
            DataLoader<Long, List<TagResponse>> tagByArticleLoader) {

        if (article.getTags() != null && !article.getTags().isEmpty()) {
            // Already loaded by service, return as-is
            return CompletableFuture.completedFuture(article.getTags());
        }

        log.debug("[DATALOADER] Loading tags for article id={}", article.getId());
        return tagByArticleLoader.load(article.getId());
    }

    /**
     * Resolve images for an article using DataLoader.
     * Batches multiple image requests into a single database query.
     */
    @SchemaMapping(typeName = "Article", field = "images")
    public CompletableFuture<List<ArticleImageResponse>> images(
            ArticleResponse article,
            DataLoader<Long, List<ArticleImageResponse>> articleImageLoader) {

        if (article.getImages() != null && !article.getImages().isEmpty()) {
            // Already loaded by service, return as-is
            return CompletableFuture.completedFuture(article.getImages());
        }

        log.debug("[DATALOADER] Loading images for article id={}", article.getId());
        return articleImageLoader.load(article.getId());
    }
}
