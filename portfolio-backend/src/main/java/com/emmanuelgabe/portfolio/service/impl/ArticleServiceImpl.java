package com.emmanuelgabe.portfolio.service.impl;

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
import com.emmanuelgabe.portfolio.service.ArticleService;
import com.emmanuelgabe.portfolio.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of ArticleService for managing blog articles.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final ImageService imageService;

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

        Article article = articleRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));

        if (!article.isPublished()) {
            log.warn("[GET_ARTICLE] Article not published - slug={}", slug);
            throw new ResourceNotFoundException("Article not found with slug: " + slug);
        }

        log.info("[GET_ARTICLE] Success - slug={}, title={}", slug, article.getTitle());
        return articleMapper.toResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleResponse getById(Long id) {
        log.debug("[GET_ARTICLE] Retrieving article by ID - id={}", id);

        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Article not found with ID: " + id));

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
    public ArticleResponse createArticle(CreateArticleRequest request, String username) {
        log.info("[CREATE_ARTICLE] Creating article - title={}, username={}",
            request.getTitle(), username);

        User author = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Article article = articleMapper.toEntity(request);
        article.setAuthor(author);
        article.generateSlug();
        article.calculateReadingTime();

        // If article is created as published (not draft), set publishedAt
        if (!article.isDraft()) {
            article.setPublishedAt(LocalDateTime.now());
        }

        if (articleRepository.existsBySlug(article.getSlug())) {
            log.warn("[CREATE_ARTICLE] Slug already exists - slug={}", article.getSlug());
            String uniqueSlug = article.getSlug() + "-" + UUID.randomUUID().toString().substring(0, 8);
            article.setSlug(uniqueSlug);
            log.info("[CREATE_ARTICLE] Generated unique slug - slug={}", uniqueSlug);
        }

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(request.getTagIds()));
            tags.forEach(article::addTag);
        }

        Article saved = articleRepository.save(article);

        log.info("[CREATE_ARTICLE] Success - id={}, slug={}, title={}",
            saved.getId(), saved.getSlug(), saved.getTitle());
        return articleMapper.toResponse(saved);
    }

    @Override
    public ArticleResponse updateArticle(Long id, UpdateArticleRequest request) {
        log.info("[UPDATE_ARTICLE] Updating article - id={}", id);

        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Article not found with ID: " + id));

        articleMapper.updateEntityFromRequest(request, article);

        if (request.getTitle() != null) {
            article.generateSlug();
            if (articleRepository.existsBySlug(article.getSlug())) {
                String uniqueSlug = article.getSlug() + "-" + UUID.randomUUID().toString().substring(0, 8);
                article.setSlug(uniqueSlug);
                log.info("[UPDATE_ARTICLE] Generated unique slug - slug={}", uniqueSlug);
            }
        }

        if (request.getContent() != null) {
            article.calculateReadingTime();
        }

        // If article is being published (draft changed to false), set publishedAt if not already set
        if (request.getDraft() != null && !request.getDraft() && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }

        if (request.getTagIds() != null) {
            article.getTags().clear();
            if (!request.getTagIds().isEmpty()) {
                Set<Tag> tags = new HashSet<>(tagRepository.findAllById(request.getTagIds()));
                tags.forEach(article::addTag);
            }
        }

        Article saved = articleRepository.save(article);

        log.info("[UPDATE_ARTICLE] Success - id={}, slug={}", saved.getId(), saved.getSlug());
        return articleMapper.toResponse(saved);
    }

    @Override
    public void deleteArticle(Long id) {
        log.info("[DELETE_ARTICLE] Deleting article - id={}", id);

        if (!articleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Article not found with ID: " + id);
        }

        articleRepository.deleteById(id);
        log.info("[DELETE_ARTICLE] Success - id={}", id);
    }

    @Override
    public ArticleResponse publishArticle(Long id) {
        log.info("[PUBLISH_ARTICLE] Publishing article - id={}", id);

        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Article not found with ID: " + id));

        article.publish();
        Article saved = articleRepository.save(article);

        log.info("[PUBLISH_ARTICLE] Success - id={}, slug={}, publishedAt={}",
            saved.getId(), saved.getSlug(), saved.getPublishedAt());
        return articleMapper.toResponse(saved);
    }

    @Override
    public ArticleResponse unpublishArticle(Long id) {
        log.info("[UNPUBLISH_ARTICLE] Unpublishing article - id={}", id);

        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Article not found with ID: " + id));

        article.unpublish();
        Article saved = articleRepository.save(article);

        log.info("[UNPUBLISH_ARTICLE] Success - id={}, slug={}", saved.getId(), saved.getSlug());
        return articleMapper.toResponse(saved);
    }

    @Override
    public ArticleImageResponse addImageToArticle(Long articleId, String imageUrl, String thumbnailUrl) {
        log.info("[ADD_ARTICLE_IMAGE] Adding image to article - articleId={}, imageUrl={}", articleId, imageUrl);

        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new ResourceNotFoundException("Article not found with ID: " + articleId));

        ArticleImage articleImage = new ArticleImage(article, imageUrl, thumbnailUrl);
        article.addImage(articleImage);
        Article saved = articleRepository.save(article);

        ArticleImage savedImage = saved.getImages().stream()
            .filter(img -> img.getImageUrl().equals(imageUrl))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Image not saved properly"));

        log.info("[ADD_ARTICLE_IMAGE] Success - articleId={}, imageId={}", articleId, savedImage.getId());
        return new ArticleImageResponse(savedImage.getId(), savedImage.getImageUrl(),
            savedImage.getThumbnailUrl(), savedImage.getUploadedAt());
    }

    @Override
    public void removeImageFromArticle(Long articleId, Long imageId) {
        log.info("[REMOVE_ARTICLE_IMAGE] Removing image from article - articleId={}, imageId={}", articleId, imageId);

        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new ResourceNotFoundException("Article not found with ID: " + articleId));

        ArticleImage imageToRemove = article.getImages().stream()
            .filter(img -> img.getId().equals(imageId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Image not found with ID: " + imageId));

        imageService.deleteArticleImage(imageToRemove.getImageUrl());
        article.removeImage(imageToRemove);
        articleRepository.save(article);

        log.info("[REMOVE_ARTICLE_IMAGE] Success - articleId={}, imageId={}", articleId, imageId);
    }
}
