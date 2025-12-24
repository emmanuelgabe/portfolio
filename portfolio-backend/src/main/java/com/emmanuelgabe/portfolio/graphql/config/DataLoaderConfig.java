package com.emmanuelgabe.portfolio.graphql.config;

import com.emmanuelgabe.portfolio.dto.ProjectImageResponse;
import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.dto.article.ArticleImageResponse;
import com.emmanuelgabe.portfolio.graphql.dataloader.ArticleImageBatchLoader;
import com.emmanuelgabe.portfolio.graphql.dataloader.ProjectImageBatchLoader;
import com.emmanuelgabe.portfolio.graphql.dataloader.TagBatchLoader;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataLoaderRegistrar;

import java.util.List;

/**
 * Configuration for GraphQL DataLoaders.
 * DataLoaders batch multiple requests into single database queries,
 * solving the N+1 query problem common in GraphQL.
 */
@Configuration
@RequiredArgsConstructor
public class DataLoaderConfig {

    public static final String TAG_BY_PROJECT_LOADER = "tagByProjectLoader";
    public static final String TAG_BY_ARTICLE_LOADER = "tagByArticleLoader";
    public static final String PROJECT_IMAGE_LOADER = "projectImageLoader";
    public static final String ARTICLE_IMAGE_LOADER = "articleImageLoader";

    private final TagBatchLoader tagBatchLoader;
    private final ProjectImageBatchLoader projectImageBatchLoader;
    private final ArticleImageBatchLoader articleImageBatchLoader;

    @Bean
    public DataLoaderRegistrar dataLoaderRegistrar() {
        return (registry, context) -> {
            // Tag loader for projects
            DataLoader<Long, List<TagResponse>> tagByProjectLoader = DataLoaderFactory
                    .newMappedDataLoader(tagBatchLoader::loadTagsForProjects);
            registry.register(TAG_BY_PROJECT_LOADER, tagByProjectLoader);

            // Tag loader for articles
            DataLoader<Long, List<TagResponse>> tagByArticleLoader = DataLoaderFactory
                    .newMappedDataLoader(tagBatchLoader::loadTagsForArticles);
            registry.register(TAG_BY_ARTICLE_LOADER, tagByArticleLoader);

            // Project image loader
            DataLoader<Long, List<ProjectImageResponse>> projectImageLoader = DataLoaderFactory
                    .newMappedDataLoader(projectImageBatchLoader::loadImagesForProjects);
            registry.register(PROJECT_IMAGE_LOADER, projectImageLoader);

            // Article image loader
            DataLoader<Long, List<ArticleImageResponse>> articleImageLoader = DataLoaderFactory
                    .newMappedDataLoader(articleImageBatchLoader::loadImagesForArticles);
            registry.register(ARTICLE_IMAGE_LOADER, articleImageLoader);
        };
    }
}
