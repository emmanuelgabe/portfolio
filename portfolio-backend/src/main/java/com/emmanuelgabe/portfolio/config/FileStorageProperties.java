package com.emmanuelgabe.portfolio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for file storage
 */
@Configuration
@ConfigurationProperties(prefix = "file.upload")
@Getter
@Setter
public class FileStorageProperties {

    private String uploadDir = "uploads/images";
    private String basePath = "/uploads/images";
    private long maxFileSize = 5242880; // 5MB in bytes
    private String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "webp"};
    private String[] allowedMimeTypes = {
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp"
    };
}
