package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.article.ArticleImageResponse;
import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.dto.article.CreateArticleRequest;
import com.emmanuelgabe.portfolio.dto.article.UpdateArticleRequest;
import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.ArticleImage;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.ArticleMapper;
import com.emmanuelgabe.portfolio.repository.ArticleRepository;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import com.emmanuelgabe.portfolio.repository.UserRepository;
import com.emmanuelgabe.portfolio.service.impl.ArticleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ArticleServiceImpl articleService;

    private Article article;
    private ArticleResponse articleResponse;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("admin");

        article = new Article();
        article.setId(1L);
        article.setTitle("Test Article");
        article.setSlug("test-article");
        article.setContent("Test content");
        article.setDraft(false);
        article.setPublishedAt(LocalDateTime.now().minusDays(1));
        article.setAuthor(user);

        articleResponse = new ArticleResponse();
        articleResponse.setId(1L);
        articleResponse.setTitle("Test Article");
        articleResponse.setSlug("test-article");
    }

    @Test
    void should_returnAllPublishedArticles_when_getAllPublished() {
        List<Article> articles = Arrays.asList(article);
        when(articleRepository.findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any(LocalDateTime.class)))
            .thenReturn(articles);
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        List<ArticleResponse> result = articleService.getAllPublished();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Article");
        verify(articleRepository).findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(any(LocalDateTime.class));
    }

    @Test
    void should_returnArticle_when_getBySlugWithPublishedArticle() {
        when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        ArticleResponse result = articleService.getBySlug("test-article");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Article");
        verify(articleRepository, never()).save(any(Article.class));
    }

    @Test
    void should_throwResourceNotFoundException_when_getBySlugWithNonexistentSlug() {
        when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.getBySlug("nonexistent"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Article not found with slug: nonexistent");
    }

    @Test
    void should_throwResourceNotFoundException_when_getBySlugWithUnpublishedArticle() {
        article.setDraft(true);
        when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));

        assertThatThrownBy(() -> articleService.getBySlug("test-article"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Article not found with slug: test-article");
    }

    @Test
    void should_returnArticle_when_getByIdWithExistingId() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        ArticleResponse result = articleService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(articleRepository).findById(1L);
    }

    @Test
    void should_throwResourceNotFoundException_when_getByIdWithNonexistentId() {
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.getById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Article not found with ID: 999");
    }

    @Test
    void should_returnAllArticles_when_getAllArticles() {
        List<Article> articles = Arrays.asList(article);
        when(articleRepository.findAllByOrderByPublishedAtDesc()).thenReturn(articles);
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        List<ArticleResponse> result = articleService.getAllArticles();

        assertThat(result).hasSize(1);
        verify(articleRepository).findAllByOrderByPublishedAtDesc();
    }

    @Test
    void should_createArticle_when_validRequestWithoutTags() {
        CreateArticleRequest request = new CreateArticleRequest();
        request.setTitle("New Article");
        request.setContent("New content");

        Article newArticle = new Article();
        newArticle.setTitle("New Article");
        newArticle.setContent("New content");
        newArticle.setSlug("new-article");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(articleMapper.toEntity(request)).thenReturn(newArticle);
        when(articleRepository.existsBySlug(anyString())).thenReturn(false);
        when(articleRepository.save(any(Article.class))).thenReturn(newArticle);
        when(articleMapper.toResponse(newArticle)).thenReturn(articleResponse);

        ArticleResponse result = articleService.createArticle(request, "admin");

        assertThat(result).isNotNull();
        verify(userRepository).findByUsername("admin");
        verify(articleRepository).save(any(Article.class));
    }

    @Test
    void should_createArticleWithUniqueslug_when_slugAlreadyExists() {
        CreateArticleRequest request = new CreateArticleRequest();
        request.setTitle("Test Article");

        Article newArticle = new Article();
        newArticle.setSlug("test-article");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(articleMapper.toEntity(request)).thenReturn(newArticle);
        when(articleRepository.existsBySlug("test-article")).thenReturn(true);
        when(articleRepository.save(any(Article.class))).thenReturn(newArticle);
        when(articleMapper.toResponse(newArticle)).thenReturn(articleResponse);

        ArticleResponse result = articleService.createArticle(request, "admin");

        assertThat(result).isNotNull();
        verify(articleRepository).save(any(Article.class));
    }

    @Test
    void should_createArticleWithTags_when_validRequestWithTagIds() {
        CreateArticleRequest request = new CreateArticleRequest();
        request.setTitle("New Article");
        request.setTagIds(Set.of(1L, 2L));

        Tag tag1 = new Tag();
        tag1.setId(1L);
        Tag tag2 = new Tag();
        tag2.setId(2L);

        Article newArticle = new Article();
        newArticle.setSlug("new-article");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(articleMapper.toEntity(request)).thenReturn(newArticle);
        when(articleRepository.existsBySlug(anyString())).thenReturn(false);
        when(tagRepository.findAllById(request.getTagIds())).thenReturn(Arrays.asList(tag1, tag2));
        when(articleRepository.save(any(Article.class))).thenReturn(newArticle);
        when(articleMapper.toResponse(newArticle)).thenReturn(articleResponse);

        ArticleResponse result = articleService.createArticle(request, "admin");

        assertThat(result).isNotNull();
        verify(tagRepository).findAllById(request.getTagIds());
    }

    @Test
    void should_throwResourceNotFoundException_when_createArticleWithNonexistentUser() {
        CreateArticleRequest request = new CreateArticleRequest();
        request.setTitle("New Article");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.createArticle(request, "nonexistent"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found: nonexistent");

        verify(articleRepository, never()).save(any(Article.class));
    }

    @Test
    void should_updateArticle_when_validRequest() {
        UpdateArticleRequest request = new UpdateArticleRequest();
        request.setTitle("Updated Title");

        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(articleRepository.existsBySlug(anyString())).thenReturn(false);
        when(articleRepository.save(any(Article.class))).thenReturn(article);
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        ArticleResponse result = articleService.updateArticle(1L, request);

        assertThat(result).isNotNull();
        verify(articleMapper).updateEntityFromRequest(request, article);
        verify(articleRepository).save(article);
    }

    @Test
    void should_throwResourceNotFoundException_when_updateNonexistentArticle() {
        UpdateArticleRequest request = new UpdateArticleRequest();

        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.updateArticle(999L, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Article not found with ID: 999");

        verify(articleRepository, never()).save(any(Article.class));
    }

    @Test
    void should_deleteArticle_when_articleExists() {
        when(articleRepository.existsById(1L)).thenReturn(true);

        articleService.deleteArticle(1L);

        verify(articleRepository).deleteById(1L);
    }

    @Test
    void should_throwResourceNotFoundException_when_deleteNonexistentArticle() {
        when(articleRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> articleService.deleteArticle(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Article not found with ID: 999");

        verify(articleRepository, never()).deleteById(anyLong());
    }

    @Test
    void should_publishArticle_when_validArticle() {
        article.setDraft(true);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenReturn(article);
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        ArticleResponse result = articleService.publishArticle(1L);

        assertThat(result).isNotNull();
        verify(articleRepository).save(article);
    }

    @Test
    void should_unpublishArticle_when_validArticle() {
        article.setDraft(false);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenReturn(article);
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        ArticleResponse result = articleService.unpublishArticle(1L);

        assertThat(result).isNotNull();
        verify(articleRepository).save(article);
    }

    @Test
    void should_throwResourceNotFoundException_when_publishNonexistentArticle() {
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.publishArticle(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Article not found with ID: 999");
    }

    @Test
    void should_throwResourceNotFoundException_when_unpublishNonexistentArticle() {
        // Arrange
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> articleService.unpublishArticle(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Article not found with ID: 999");
    }

    // ========== Pagination Tests ==========

    @Test
    void should_returnPageOfPublishedArticles_when_getAllPublishedPaginated() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Article> articlePage = new PageImpl<>(Arrays.asList(article), pageable, 1);
        when(articleRepository.findPublished(any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(articlePage);
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        // Act
        Page<ArticleResponse> result = articleService.getAllPublishedPaginated(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        verify(articleRepository).findPublished(any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void should_returnPageOfAllArticles_when_getAllArticlesPaginated() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Article> articlePage = new PageImpl<>(Arrays.asList(article), pageable, 1);
        when(articleRepository.findAllByOrderByPublishedAtDesc(any(Pageable.class)))
            .thenReturn(articlePage);
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        // Act
        Page<ArticleResponse> result = articleService.getAllArticlesPaginated(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        verify(articleRepository).findAllByOrderByPublishedAtDesc(any(Pageable.class));
    }

    // ========== Update Article Edge Cases ==========

    @Test
    void should_updateArticleWithContent_when_contentIsProvided() {
        // Arrange
        UpdateArticleRequest request = new UpdateArticleRequest();
        request.setContent("Updated content");

        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenReturn(article);
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        // Act
        ArticleResponse result = articleService.updateArticle(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(articleMapper).updateEntityFromRequest(request, article);
        verify(articleRepository).save(article);
    }

    @Test
    void should_updateArticleWithTags_when_tagIdsProvided() {
        // Arrange
        UpdateArticleRequest request = new UpdateArticleRequest();
        request.setTagIds(Set.of(1L, 2L));

        Tag tag1 = new Tag();
        tag1.setId(1L);
        Tag tag2 = new Tag();
        tag2.setId(2L);

        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(tagRepository.findAllById(request.getTagIds())).thenReturn(Arrays.asList(tag1, tag2));
        when(articleRepository.save(any(Article.class))).thenReturn(article);
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        // Act
        ArticleResponse result = articleService.updateArticle(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(tagRepository).findAllById(request.getTagIds());
    }

    @Test
    void should_clearTags_when_emptyTagIdsProvided() {
        // Arrange
        UpdateArticleRequest request = new UpdateArticleRequest();
        request.setTagIds(Set.of());

        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenReturn(article);
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        // Act
        ArticleResponse result = articleService.updateArticle(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(articleRepository).save(article);
    }

    @Test
    void should_setPublishedAt_when_draftChangedToFalseAndPublishedAtIsNull() {
        // Arrange
        UpdateArticleRequest request = new UpdateArticleRequest();
        request.setDraft(false);

        article.setPublishedAt(null);

        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenReturn(article);
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        // Act
        ArticleResponse result = articleService.updateArticle(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(articleRepository).save(article);
    }

    @Test
    void should_generateUniqueSlug_when_updateArticleWithExistingSlug() {
        // Arrange
        UpdateArticleRequest request = new UpdateArticleRequest();
        request.setTitle("Existing Title");

        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(articleRepository.existsBySlug(anyString())).thenReturn(true);
        when(articleRepository.save(any(Article.class))).thenReturn(article);
        when(articleMapper.toResponse(article)).thenReturn(articleResponse);

        // Act
        ArticleResponse result = articleService.updateArticle(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(articleRepository).save(article);
    }

    // ========== Article Image Tests ==========

    @Test
    void should_addImageToArticle_when_validArticleAndImageUrl() {
        // Arrange
        String imageUrl = "/uploads/articles/article_1.webp";
        String thumbnailUrl = "/uploads/articles/article_1_thumb.webp";

        Article articleWithImages = new Article();
        articleWithImages.setId(1L);
        articleWithImages.setTitle("Test Article");
        articleWithImages.setImages(new HashSet<>());

        ArticleImage articleImage = new ArticleImage(articleWithImages, imageUrl, thumbnailUrl);
        articleImage.setId(1L);
        articleWithImages.getImages().add(articleImage);

        when(articleRepository.findById(1L)).thenReturn(Optional.of(articleWithImages));
        when(articleRepository.save(any(Article.class))).thenReturn(articleWithImages);

        // Act
        ArticleImageResponse result = articleService.addImageToArticle(1L, imageUrl, thumbnailUrl);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).isEqualTo(imageUrl);
        assertThat(result.getThumbnailUrl()).isEqualTo(thumbnailUrl);
        verify(articleRepository).save(articleWithImages);
    }

    @Test
    void should_throwResourceNotFoundException_when_addImageToNonexistentArticle() {
        // Arrange
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> articleService.addImageToArticle(999L, "url", "thumb"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Article not found with ID: 999");
    }

    @Test
    void should_removeImageFromArticle_when_validArticleAndImage() {
        // Arrange
        Article articleWithImages = new Article();
        articleWithImages.setId(1L);
        articleWithImages.setImages(new HashSet<>());

        ArticleImage articleImage = new ArticleImage(articleWithImages, "/uploads/article_1.webp", "/uploads/article_1_thumb.webp");
        articleImage.setId(10L);
        articleWithImages.getImages().add(articleImage);

        when(articleRepository.findById(1L)).thenReturn(Optional.of(articleWithImages));
        when(articleRepository.save(any(Article.class))).thenReturn(articleWithImages);

        // Act
        articleService.removeImageFromArticle(1L, 10L);

        // Assert
        verify(imageService).deleteArticleImage("/uploads/article_1.webp");
        verify(articleRepository).save(articleWithImages);
    }

    @Test
    void should_throwResourceNotFoundException_when_removeImageFromNonexistentArticle() {
        // Arrange
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> articleService.removeImageFromArticle(999L, 1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Article not found with ID: 999");
    }

    @Test
    void should_throwResourceNotFoundException_when_removeNonexistentImage() {
        // Arrange
        Article articleWithImages = new Article();
        articleWithImages.setId(1L);
        articleWithImages.setImages(new HashSet<>());

        when(articleRepository.findById(1L)).thenReturn(Optional.of(articleWithImages));

        // Act / Assert
        assertThatThrownBy(() -> articleService.removeImageFromArticle(1L, 999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Image not found with ID: 999");
    }

    // ========== Create Article Edge Cases ==========

    @Test
    void should_setPublishedAt_when_createArticleNotAsDraft() {
        // Arrange
        CreateArticleRequest request = new CreateArticleRequest();
        request.setTitle("New Article");
        request.setContent("Content");

        Article newArticle = new Article();
        newArticle.setTitle("New Article");
        newArticle.setContent("Content");
        newArticle.setSlug("new-article");
        newArticle.setDraft(false);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(articleMapper.toEntity(request)).thenReturn(newArticle);
        when(articleRepository.existsBySlug(anyString())).thenReturn(false);
        when(articleRepository.save(any(Article.class))).thenReturn(newArticle);
        when(articleMapper.toResponse(newArticle)).thenReturn(articleResponse);

        // Act
        ArticleResponse result = articleService.createArticle(request, "admin");

        // Assert
        assertThat(result).isNotNull();
        verify(articleRepository).save(any(Article.class));
    }
}
