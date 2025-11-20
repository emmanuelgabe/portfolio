package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for image upload operations
 * Contains URLs and metadata for uploaded optimized images
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {

    /**
     * URL to the full-size optimized image (max 1200px width, WebP format)
     */
    private String imageUrl;

    /**
     * URL to the thumbnail image (300x300px square crop, WebP format)
     */
    private String thumbnailUrl;

    /**
     * File size of the optimized image in bytes
     */
    private Long fileSize;

    /**
     * Timestamp when the image was uploaded
     */
    private LocalDateTime uploadedAt;
}
