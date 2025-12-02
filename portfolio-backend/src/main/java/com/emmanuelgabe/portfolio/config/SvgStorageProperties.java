package com.emmanuelgabe.portfolio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for SVG icon storage (skill icons)
 * Supports custom SVG uploads for skills
 */
@Configuration
@ConfigurationProperties(prefix = "svg.storage")
@Getter
@Setter
public class SvgStorageProperties {

    /**
     * Directory where uploaded SVG icons are stored
     */
    private String uploadDir = "uploads/icons";

    /**
     * Base path for serving icons via HTTP
     */
    private String basePath = "/uploads/icons";

    /**
     * Maximum file size in bytes (default: 100KB)
     */
    private long maxFileSize = 102400;

    /**
     * Allowed file extensions
     */
    private String[] allowedExtensions = {"svg"};

    /**
     * Allowed MIME types
     */
    private String[] allowedMimeTypes = {"image/svg+xml"};
}
