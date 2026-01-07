package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.audit.Auditable;
import com.emmanuelgabe.portfolio.dto.PreparedImageInfo;
import com.emmanuelgabe.portfolio.dto.ReorderRequest;
import com.emmanuelgabe.portfolio.dto.article.ArticleImageResponse;
import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import com.emmanuelgabe.portfolio.dto.article.CreateArticleRequest;
import com.emmanuelgabe.portfolio.dto.article.UpdateArticleRequest;
import com.emmanuelgabe.portfolio.entity.Article;
import com.emmanuelgabe.portfolio.entity.ArticleImage;
import com.emmanuelgabe.portfolio.entity.ImageStatus;
import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.ArticleMapper;

import static com.emmanuelgabe.portfolio.util.EntityHelper.findOrThrow;
import static com.emmanuelgabe.portfolio.util.EntityHelper.ensureUniqueSlug;
import static com.emmanuelgabe.portfolio.util.EntityHelper.associateTags;
import static com.emmanuelgabe.portfolio.util.EntityHelper.updateTags;
import static com.emmanuelgabe.portfolio.util.EntityHelper.findImageByUrl;

import com.emmanuelgabe.portfolio.messaging.event.ImageProcessingEvent;
import com.emmanuelgabe.portfolio.messaging.publisher.EventPublisher;
import com.emmanuelgabe.portfolio.repository.ArticleImageRepository;
import com.emmanuelgabe.portfolio.search.event.ArticleIndexEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.emmanuelgabe.portfolio.repository.ArticleRepository;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import com.emmanuelgabe.portfolio.repository.UserRepository;
import com.emmanuelgabe.portfolio.service.ArticleService;
import com.emmanuelgabe.portfolio.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of ArticleService for managing blog articles.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleImageRepository articleImageRepository;
    private final ArticleMapper articleMapper;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final ImageService imageService;
    private final EventPublisher eventPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<ArticleResponse> getArticlesByIds(Collection<Long> ids) {
        log.debug("[LIST_ARTICLES] Fetching articles by IDs - count={}", ids.size());
        List<Article> articles = articleRepository.findAllById(ids);
        log.debug("[LIST_ARTICLES] Found {} articles by IDs", articles.size());
        return articles.stream()
                .map(articleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleResponse> getAllPublished() {
        log.debug("[LIST_ARTICLES] Retrieving all published articles");
        List<Article> articles = articleRepository
            .findByDraftFalseAndPublishedAtBeforeOrderByPublishedAtDesc(LocalDateTime.now());
        log.info("[LIST_ARTICLES] Success - count={}", articles.size());
        return articles.stream()
            .map(articleMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> getAllPublishedPaginated(Pageable pageable) {
        log.debug("[LIST_ARTICLES] Retrieving published articles - page={}, size={}",
            pageable.getPageNumber(), pageable.getPageSize());
        Page<Article> articles = articleRepository.findPublished(LocalDateTime.now(), pageable);
        log.info("[LIST_ARTICLES] Success - page={}, totalElements={}, totalPages={}",
            pageable.getPageNumber(), articles.getTotalElements(), articles.getTotalPages());
        return articles.map(articleMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleResponse getBySlug(String slug) {
        log.debug("[GET_ARTICLE] Retrieving article - slug={}", slug);

        Article article = findOrThrow(articleRepository.findBySlug(slug), "Article", "slug", slug);

        if (!article.isPublished()) {
            log.warn("[GET_ARTICLE] Article not published - slug={}", slug);
            throw new ResourceNotFoundException("Article", "slug", slug);
        }

        log.info("[GET_ARTICLE] Success - slug={}, title={}", slug, article.getTitle());
        return articleMapper.toResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleResponse getById(Long id) {
        log.debug("[GET_ARTICLE] Retrieving article by ID - id={}", id);

        Article article = findOrThrow(articleRepository.findById(id), "Article", "id", id);

        log.info("[GET_ARTICLE] Success - id={}, title={}", id, article.getTitle());
        return articleMapper.toResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleResponse> getAllArticles() {
        log.debug("[LIST_ARTICLES] Retrieving all articles (including drafts)");
        List<Article> articles = articleRepository.findAllByOrderByPublishedAtDesc();
        log.info("[LIST_ARTICLES] Success - count={}", articles.size());
        return articles.stream()
            .map(articleMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleResponse> getAllArticlesPaginated(Pageable pageable) {
        log.debug("[LIST_ARTICLES] Retrieving all articles (admin) - page={}, size={}",
            pageable.getPageNumber(), pageable.getPageSize());
        Page<Article> articles = articleRepository.findAllByOrderByPublishedAtDesc(pageable);
        log.info("[LIST_ARTICLES] Success - page={}, totalElements={}, totalPages={}",
            pageable.getPageNumber(), articles.getTotalElements(), articles.getTotalPages());
        return articles.map(articleMapper::toResponse);
    }

    @Override
    @Auditable(action = AuditAction.CREATE, entityType = "Article",
            entityIdExpression = "#result.id", entityNameExpression = "#result.title")
    public ArticleResponse createArticle(CreateArticleRequest request, String username) {
        log.info("[CREATE_ARTICLE] Creating article - title={}, username={}",
            request.getTitle(), username);

        User author = findOrThrow(userRepository.findByUsername(username), "User", "username", username);

        Article article = articleMapper.toEntity(request);
        article.setAuthor(author);
        article.generateSlug();
        article.calculateReadingTime();

        // If article is created as published (not draft), set publishedAt
        if (!article.isDraft()) {
            article.setPublishedAt(LocalDateTime.now());
        }

        article.setSlug(ensureUniqueSlug(article.getSlug(), articleRepository::existsBySlug, "CREATE_ARTICLE"));

        associateTags(request.getTagIds(), tagRepository, article::addTag);

        Article saved = articleRepository.save(article);
        applicationEventPublisher.publishEvent(ArticleIndexEvent.forIndex(saved));

        log.info("[CREATE_ARTICLE] Success - id={}, slug={}, title={}",
            saved.getId(), saved.getSlug(), saved.getTitle());
        return articleMapper.toResponse(saved);
    }

    @Override
    @Auditable(action = AuditAction.UPDATE, entityType = "Article",
            entityIdExpression = "#id", entityNameExpression = "#result.title")
    public ArticleResponse updateArticle(Long id, UpdateArticleRequest request) {
        log.info("[UPDATE_ARTICLE] Updating article - id={}", id);

        Article article = findOrThrow(articleRepository.findById(id), "Article", "id", id);

        articleMapper.updateEntityFromRequest(request, article);

        if (request.getTitle() != null) {
            article.generateSlug();
            article.setSlug(ensureUniqueSlug(article.getSlug(), articleRepository::existsBySlug, "UPDATE_ARTICLE"));
        }

        if (request.getContent() != null) {
            article.calculateReadingTime();
        }

        // If article is being published (draft changed to false), set publishedAt if not already set
        if (request.getDraft() != null && !request.getDraft() && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }

        updateTags(request.getTagIds(), tagRepository, () -> article.getTags().clear(), article::addTag);

        Article saved = articleRepository.save(article);
        applicationEventPublisher.publishEvent(ArticleIndexEvent.forIndex(saved));

        log.info("[UPDATE_ARTICLE] Success - id={}, slug={}", saved.getId(), saved.getSlug());
        return articleMapper.toResponse(saved);
    }

    @Override
    @Auditable(action = AuditAction.DELETE, entityType = "Article", entityIdExpression = "#id")
    public void deleteArticle(Long id) {
        log.info("[DELETE_ARTICLE] Deleting article - id={}", id);

        Article article = findOrThrow(articleRepository.findById(id), "Article", "id", id);

        articleRepository.deleteById(id);
        applicationEventPublisher.publishEvent(ArticleIndexEvent.forRemove(article));
        log.info("[DELETE_ARTICLE] Success - id={}", id);
    }

    @Override
    @Auditable(action = AuditAction.PUBLISH, entityType = "Article",
            entityIdExpression = "#id", entityNameExpression = "#result.title")
    public ArticleResponse publishArticle(Long id) {
        log.info("[PUBLISH_ARTICLE] Publishing article - id={}", id);

        Article article = findOrThrow(articleRepository.findById(id), "Article", "id", id);

        article.publish();
        Article saved = articleRepository.save(article);
        applicationEventPublisher.publishEvent(ArticleIndexEvent.forIndex(saved));

        log.info("[PUBLISH_ARTICLE] Success - id={}, slug={}, publishedAt={}",
            saved.getId(), saved.getSlug(), saved.getPublishedAt());
        return articleMapper.toResponse(saved);
    }

    @Override
    @Auditable(action = AuditAction.UNPUBLISH, entityType = "Article",
            entityIdExpression = "#id", entityNameExpression = "#result.title")
    public ArticleResponse unpublishArticle(Long id) {
        log.info("[UNPUBLISH_ARTICLE] Unpublishing article - id={}", id);

        Article article = findOrThrow(articleRepository.findById(id), "Article", "id", id);

        article.unpublish();
        Article saved = articleRepository.save(article);
        applicationEventPublisher.publishEvent(ArticleIndexEvent.forIndex(saved));

        log.info("[UNPUBLISH_ARTICLE] Success - id={}, slug={}", saved.getId(), saved.getSlug());
        return articleMapper.toResponse(saved);
    }

    @Override
    public ArticleImageResponse addImageToArticle(Long articleId, MultipartFile file) {
        log.info("[ADD_ARTICLE_IMAGE] Adding image to article - articleId={}, fileName={}",
                articleId, file.getOriginalFilename());

        Article article = findOrThrow(articleRepository.findById(articleId), "Article", "id", articleId);

        // Step 1: Prepare image (save temp file, generate URLs)
        PreparedImageInfo preparedImage = imageService.prepareArticleImage(articleId, file);

        // Step 2: Create and save ArticleImage entity with PROCESSING status
        ArticleImage articleImage = new ArticleImage(article,
                preparedImage.getImageUrl(), preparedImage.getThumbnailUrl());
        articleImage.setStatus(ImageStatus.PROCESSING);
        article.addImage(articleImage);
        Article saved = articleRepository.save(article);

        ArticleImage savedImage = findImageByUrl(saved.getImages(), preparedImage.getImageUrl(),
                ArticleImage::getImageUrl, "ArticleImage");

        // Step 3: Publish event with the saved entity ID for status update after processing
        ImageProcessingEvent event = ImageProcessingEvent.forArticle(
                articleId, savedImage.getId(),
                preparedImage.getTempFilePath(),
                preparedImage.getOptimizedFilePath(),
                preparedImage.getThumbnailFilePath());
        eventPublisher.publishImageEvent(event);

        log.info("[ADD_ARTICLE_IMAGE] Success - articleId={}, imageId={}, status=PROCESSING",
                articleId, savedImage.getId());

        return new ArticleImageResponse(savedImage.getId(), savedImage.getImageUrl(),
            savedImage.getThumbnailUrl(), savedImage.getStatus(), savedImage.getUploadedAt());
    }

    @Override
    public void removeImageFromArticle(Long articleId, Long imageId) {
        log.info("[REMOVE_ARTICLE_IMAGE] Removing image from article - articleId={}, imageId={}", articleId, imageId);

        Article article = findOrThrow(articleRepository.findById(articleId), "Article", "id", articleId);

        ArticleImage imageToRemove = article.getImages().stream()
            .filter(img -> img.getId().equals(imageId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("ArticleImage", "id", imageId));

        imageService.deleteArticleImage(imageToRemove.getImageUrl());
        article.removeImage(imageToRemove);
        articleRepository.save(article);

        log.info("[REMOVE_ARTICLE_IMAGE] Success - articleId={}, imageId={}", articleId, imageId);
    }

    @Override
    public void reorderArticles(ReorderRequest request) {
        log.debug("[REORDER_ARTICLES] Reordering articles - count={}", request.getOrderedIds().size());

        List<Long> orderedIds = request.getOrderedIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            Long articleId = orderedIds.get(i);
            Article article = findOrThrow(articleRepository.findById(articleId), "Article", "id", articleId);
            article.setDisplayOrder(i);
            articleRepository.save(article);
        }

        log.info("[REORDER_ARTICLES] Articles reordered - count={}", orderedIds.size());
    }
}
