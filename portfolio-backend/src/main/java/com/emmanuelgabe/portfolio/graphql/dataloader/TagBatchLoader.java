package com.emmanuelgabe.portfolio.graphql.dataloader;

import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.mapper.TagMapper;
import com.emmanuelgabe.portfolio.repository.ArticleRepository;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Batch loader for Tags.
 * Batches multiple tag requests into single database queries to prevent N+1 problem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagBatchLoader {

    private final ProjectRepository projectRepository;
    private final ArticleRepository articleRepository;
    private final TagMapper tagMapper;

    /**
     * Batch load tags for multiple projects.
     * @param projectIds set of project IDs to load tags for
     * @return map of project ID to list of tags
     */
    public CompletionStage<Map<Long, List<TagResponse>>> loadTagsForProjects(Set<Long> projectIds) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("[DATALOADER] Loading tags for {} projects", projectIds.size());

            List<Project> projects = projectRepository.findAllById(projectIds);
            Map<Long, List<TagResponse>> result = new HashMap<>();

            for (Project project : projects) {
                List<TagResponse> tags = project.getTags().stream()
                        .map(tagMapper::toResponse)
                        .toList();
                result.put(project.getId(), tags);
            }

            // Ensure all requested IDs have an entry (empty list if no tags)
            for (Long projectId : projectIds) {
                result.putIfAbsent(projectId, new ArrayList<>());
            }

            log.debug("[DATALOADER] Loaded tags for {} projects", result.size());
            return result;
        });
    }

    /**
     * Batch load tags for multiple articles.
     * @param articleIds set of article IDs to load tags for
     * @return map of article ID to list of tags
     */
    public CompletionStage<Map<Long, List<TagResponse>>> loadTagsForArticles(Set<Long> articleIds) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("[DATALOADER] Loading tags for {} articles", articleIds.size());

            List<Article> articles = articleRepository.findAllById(articleIds);
            Map<Long, List<TagResponse>> result = new HashMap<>();

            for (Article article : articles) {
                List<TagResponse> tags = article.getTags().stream()
                        .map(tagMapper::toResponse)
                        .toList();
                result.put(article.getId(), tags);
            }

            // Ensure all requested IDs have an entry
            for (Long articleId : articleIds) {
                result.putIfAbsent(articleId, new ArrayList<>());
            }

            log.debug("[DATALOADER] Loaded tags for {} articles", result.size());
            return result;
        });
    }

}
