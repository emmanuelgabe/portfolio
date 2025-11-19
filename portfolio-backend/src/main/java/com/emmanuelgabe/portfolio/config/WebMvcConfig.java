package com.emmanuelgabe.portfolio.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 * Configures static resource handling
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;
    private final CvStorageProperties cvStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded images
        registry.addResourceHandler(fileStorageProperties.getBasePath() + "/**")
                .addResourceLocations("file:" + fileStorageProperties.getUploadDir() + "/");

        // Serve uploaded CVs
        registry.addResourceHandler(cvStorageProperties.getBasePath() + "/**")
                .addResourceLocations("file:" + cvStorageProperties.getUploadDir() + "/");
    }
}
