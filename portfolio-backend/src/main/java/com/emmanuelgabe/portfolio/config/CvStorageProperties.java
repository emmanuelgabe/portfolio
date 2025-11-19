package com.emmanuelgabe.portfolio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for CV file storage
 */
@Configuration
@ConfigurationProperties(prefix = "cv.storage")
@Getter
@Setter
public class CvStorageProperties {

    private String uploadDir = "uploads/cvs";
    private String basePath = "/uploads/cvs";
    private long maxFileSize = 10485760; // 10MB in bytes
    private String[] allowedExtensions = {"pdf"};
    private String[] allowedMimeTypes = {"application/pdf"};
}
