package com.emmanuelgabe.portfolio.graphql.dataloader;

import com.emmanuelgabe.portfolio.dto.article.ArticleImageResponse;
import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.ArticleImage;
import com.emmanuelgabe.portfolio.entity.ImageStatus;
import com.emmanuelgabe.portfolio.mapper.ArticleImageMapper;
import com.emmanuelgabe.portfolio.repository.ArticleImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ArticleImageBatchLoader.
 */
@ExtendWith(MockitoExtension.class)
class ArticleImageBatchLoaderTest {

    @Mock
    private ArticleImageRepository articleImageRepository;

    @Mock
    private ArticleImageMapper articleImageMapper;

    @InjectMocks
    private ArticleImageBatchLoader articleImageBatchLoader;

    @BeforeEach
    void setUp() {
        lenient().when(articleImageMapper.toResponse(any(ArticleImage.class))).thenAnswer(invocation -> {
            ArticleImage img = invocation.getArgument(0);
            return new ArticleImageResponse(img.getId(), img.getImageUrl(),
                    img.getThumbnailUrl(), img.getStatus(), img.getUploadedAt());
        });
    }

    // ========== loadImagesForArticles Tests ==========

    @Test
    void should_returnImagesByArticleId_when_loadImagesForArticlesCalled() throws Exception {
        // Arrange
        Article article1 = new Article();
        article1.setId(1L);

        Article article2 = new Article();
        article2.setId(2L);

        ArticleImage image1 = createArticleImage(1L, article1);
        ArticleImage image2 = createArticleImage(2L, article1);
        ArticleImage image3 = createArticleImage(3L, article2);

        when(articleImageRepository.findByArticleIdIn(Set.of(1L, 2L)))
                .thenReturn(List.of(image1, image2, image3));

        // Act
        CompletionStage<Map<Long, List<ArticleImageResponse>>> result =
                articleImageBatchLoader.loadImagesForArticles(Set.of(1L, 2L));
        Map<Long, List<ArticleImageResponse>> imagesMap = result.toCompletableFuture().get();

        // Assert
        assertThat(imagesMap).hasSize(2);
        assertThat(imagesMap.get(1L)).hasSize(2);
        assertThat(imagesMap.get(2L)).hasSize(1);
    }

    @Test
    void should_returnEmptyListForArticlesWithoutImages_when_loadImagesForArticlesCalled() throws Exception {
        // Arrange
        when(articleImageRepository.findByArticleIdIn(Set.of(1L, 2L)))
                .thenReturn(List.of());

        // Act
        CompletionStage<Map<Long, List<ArticleImageResponse>>> result =
                articleImageBatchLoader.loadImagesForArticles(Set.of(1L, 2L));
        Map<Long, List<ArticleImageResponse>> imagesMap = result.toCompletableFuture().get();

        // Assert
        assertThat(imagesMap).hasSize(2);
        assertThat(imagesMap.get(1L)).isEmpty();
        assertThat(imagesMap.get(2L)).isEmpty();
    }

    @Test
    void should_returnCorrectImageDetails_when_loadImagesForArticlesCalled() throws Exception {
        // Arrange
        Article article = new Article();
        article.setId(1L);

        ArticleImage image = createArticleImage(1L, article);

        when(articleImageRepository.findByArticleIdIn(Set.of(1L)))
                .thenReturn(List.of(image));

        // Act
        CompletionStage<Map<Long, List<ArticleImageResponse>>> result =
                articleImageBatchLoader.loadImagesForArticles(Set.of(1L));
        Map<Long, List<ArticleImageResponse>> imagesMap = result.toCompletableFuture().get();

        // Assert
        assertThat(imagesMap.get(1L)).hasSize(1);
        ArticleImageResponse response = imagesMap.get(1L).get(0);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getImageUrl()).isEqualTo("http://example.com/article-image1.webp");
        assertThat(response.getStatus()).isEqualTo(ImageStatus.READY);
    }

    private ArticleImage createArticleImage(Long id, Article article) {
        ArticleImage image = new ArticleImage();
        image.setId(id);
        image.setArticle(article);
        image.setImageUrl("http://example.com/article-image" + id + ".webp");
        image.setThumbnailUrl("http://example.com/article-thumb" + id + ".webp");
        image.setStatus(ImageStatus.READY);
        image.setUploadedAt(LocalDateTime.now());
        return image;
    }
}
