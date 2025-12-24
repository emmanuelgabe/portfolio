package com.emmanuelgabe.portfolio.graphql.dataloader;

import com.emmanuelgabe.portfolio.dto.ProjectImageResponse;
import com.emmanuelgabe.portfolio.entity.ProjectImage;
import com.emmanuelgabe.portfolio.mapper.ProjectImageMapper;
import com.emmanuelgabe.portfolio.repository.ProjectImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Batch loader for Project Images.
 * Batches multiple image requests into single database queries to prevent N+1 problem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectImageBatchLoader {

    private final ProjectImageRepository projectImageRepository;
    private final ProjectImageMapper projectImageMapper;

    /**
     * Batch load images for multiple projects.
     * @param projectIds set of project IDs to load images for
     * @return map of project ID to list of images
     */
    public CompletionStage<Map<Long, List<ProjectImageResponse>>> loadImagesForProjects(Set<Long> projectIds) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("[DATALOADER] Loading images for {} projects", projectIds.size());

            List<ProjectImage> allImages = projectImageRepository.findByProjectIdInOrderByDisplayOrderAsc(projectIds);

            Map<Long, List<ProjectImageResponse>> result = allImages.stream()
                    .collect(Collectors.groupingBy(
                            img -> img.getProject().getId(),
                            Collectors.mapping(projectImageMapper::toResponse, Collectors.toList())
                    ));

            // Ensure all requested IDs have an entry (empty list if no images)
            for (Long projectId : projectIds) {
                result.putIfAbsent(projectId, new ArrayList<>());
            }

            log.debug("[DATALOADER] Loaded images for {} projects", result.size());
            return result;
        });
    }
}
