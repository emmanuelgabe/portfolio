package com.emmanuelgabe.portfolio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for image storage (project images)
 * Supports optimized images with WebP conversion and thumbnail generation
 */
@Configuration
@ConfigurationProperties(prefix = "image.storage")
@Getter
@Setter
public class ImageStorageProperties {

    /**
     * Directory where uploaded images are stored
     */
    private String uploadDir = "uploads/projects";

    /**
     * Base path for serving images via HTTP
     */
    private String basePath = "/uploads/projects";

    /**
     * Maximum file size in bytes (default: 10MB)
     */
    private long maxFileSize = 10485760;

    /**
     * Maximum width for optimized images in pixels
     */
    private int imageMaxWidth = 1200;

    /**
     * Thumbnail size for square crop in pixels
     */
    private int thumbnailSize = 300;

    /**
     * JPEG compression quality (0.0 - 1.0)
     */
    private float jpegQuality = 0.85f;

    /**
     * Thumbnail compression quality (0.0 - 1.0)
     */
    private float thumbnailQuality = 0.80f;

    /**
     * Allowed file extensions
     */
    private String[] allowedExtensions = {"jpg", "jpeg", "png", "webp"};

    /**
     * Allowed MIME types
     */
    private String[] allowedMimeTypes = {"image/jpeg", "image/png", "image/webp"};
}
