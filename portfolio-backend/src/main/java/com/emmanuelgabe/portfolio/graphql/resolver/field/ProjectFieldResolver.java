package com.emmanuelgabe.portfolio.graphql.resolver.field;

import com.emmanuelgabe.portfolio.dto.ProjectImageResponse;
import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.TagResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Field resolver for Project type.
 * Uses DataLoaders to batch load tags and images, preventing N+1 queries.
 *
 * Note: These resolvers are only called if the parent object's field is null.
 * When the service already populates these fields, Spring GraphQL uses the
 * existing values without calling these resolvers.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ProjectFieldResolver {

    /**
     * Resolve tags for a project using DataLoader.
     * Batches multiple tag requests into a single database query.
     */
    @SchemaMapping(typeName = "Project", field = "tags")
    public CompletableFuture<List<TagResponse>> tags(
            ProjectResponse project,
            DataLoader<Long, List<TagResponse>> tagByProjectLoader) {

        if (project.getTags() != null && !project.getTags().isEmpty()) {
            // Already loaded by service, return as-is
            return CompletableFuture.completedFuture(List.copyOf(project.getTags()));
        }

        log.debug("[DATALOADER] Loading tags for project id={}", project.getId());
        return tagByProjectLoader.load(project.getId());
    }

    /**
     * Resolve images for a project using DataLoader.
     * Batches multiple image requests into a single database query.
     */
    @SchemaMapping(typeName = "Project", field = "images")
    public CompletableFuture<List<ProjectImageResponse>> images(
            ProjectResponse project,
            DataLoader<Long, List<ProjectImageResponse>> projectImageLoader) {

        if (project.getImages() != null && !project.getImages().isEmpty()) {
            // Already loaded by service, return as-is
            return CompletableFuture.completedFuture(project.getImages());
        }

        log.debug("[DATALOADER] Loading images for project id={}", project.getId());
        return projectImageLoader.load(project.getId());
    }
}
