package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.ImageUploadResponse;
import com.emmanuelgabe.portfolio.dto.article.ArticleImageResponse;
import com.emmanuelgabe.portfolio.service.ArticleService;
import com.emmanuelgabe.portfolio.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for managing article images.
 * Provides admin endpoints for uploading and deleting images associated with articles.
 */
@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Article Images", description = "Article image management (admin only)")
public class ArticleImageController {

    private final ImageService imageService;
    private final ArticleService articleService;

    @PostMapping("/{id}/images")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload article image", description = "Uploads an image for an article (admin only)")
    @ApiResponse(responseCode = "201", description = "Image uploaded")
    @ApiResponse(responseCode = "404", description = "Article not found")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<ArticleImageResponse> uploadImage(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file
    ) {
        log.info("[UPLOAD_ARTICLE_IMAGE] Request - articleId={}, fileName={}, size={}",
            id, file.getOriginalFilename(), file.getSize());

        ImageUploadResponse uploadResponse = imageService.uploadArticleImage(id, file);
        ArticleImageResponse response = articleService.addImageToArticle(
            id,
            uploadResponse.getImageUrl(),
            uploadResponse.getThumbnailUrl()
        );

        log.info("[UPLOAD_ARTICLE_IMAGE] Success - articleId={}, imageId={}",
            id, response.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{articleId}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete article image", description = "Deletes an image from an article (admin only)")
    @ApiResponse(responseCode = "204", description = "Image deleted")
    @ApiResponse(responseCode = "404", description = "Article or image not found")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<Void> deleteImage(
        @PathVariable Long articleId,
        @PathVariable Long imageId
    ) {
        log.info("[DELETE_ARTICLE_IMAGE] Request - articleId={}, imageId={}", articleId, imageId);

        articleService.removeImageFromArticle(articleId, imageId);

        log.info("[DELETE_ARTICLE_IMAGE] Success - articleId={}, imageId={}", articleId, imageId);

        return ResponseEntity.noContent().build();
    }
}
