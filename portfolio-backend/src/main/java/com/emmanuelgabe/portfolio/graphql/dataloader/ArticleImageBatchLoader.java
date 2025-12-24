package com.emmanuelgabe.portfolio.graphql.dataloader;

import com.emmanuelgabe.portfolio.dto.article.ArticleImageResponse;
import com.emmanuelgabe.portfolio.entity.ArticleImage;
import com.emmanuelgabe.portfolio.mapper.ArticleImageMapper;
import com.emmanuelgabe.portfolio.repository.ArticleImageRepository;
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
 * Batch loader for Article Images.
 * Batches multiple image requests into single database queries to prevent N+1 problem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleImageBatchLoader {

    private final ArticleImageRepository articleImageRepository;
    private final ArticleImageMapper articleImageMapper;

    /**
     * Batch load images for multiple articles.
     * @param articleIds set of article IDs to load images for
     * @return map of article ID to list of images
     */
    public CompletionStage<Map<Long, List<ArticleImageResponse>>> loadImagesForArticles(Set<Long> articleIds) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("[DATALOADER] Loading images for {} articles", articleIds.size());

            List<ArticleImage> allImages = articleImageRepository.findByArticleIdIn(articleIds);

            Map<Long, List<ArticleImageResponse>> result = allImages.stream()
                    .collect(Collectors.groupingBy(
                            img -> img.getArticle().getId(),
                            Collectors.mapping(articleImageMapper::toResponse, Collectors.toList())
                    ));

            // Ensure all requested IDs have an entry (empty list if no images)
            for (Long articleId : articleIds) {
                result.putIfAbsent(articleId, new ArrayList<>());
            }

            log.debug("[DATALOADER] Loaded images for {} articles", result.size());
            return result;
        });
    }
}
