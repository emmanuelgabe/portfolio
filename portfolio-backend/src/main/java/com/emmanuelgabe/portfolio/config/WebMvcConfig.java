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
    private final ImageStorageProperties imageStorageProperties;
    private final SvgStorageProperties svgStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded images
        registry.addResourceHandler(fileStorageProperties.getBasePath() + "/**")
                .addResourceLocations("file:" + fileStorageProperties.getUploadDir() + "/");

        // Serve uploaded CVs
        registry.addResourceHandler(cvStorageProperties.getBasePath() + "/**")
                .addResourceLocations("file:" + cvStorageProperties.getUploadDir() + "/");

        // Serve project images (optimized images and thumbnails)
        registry.addResourceHandler(imageStorageProperties.getBasePath() + "/**")
                .addResourceLocations("file:" + imageStorageProperties.getUploadDir() + "/");

        // Serve SVG icons (skill icons)
        registry.addResourceHandler(svgStorageProperties.getBasePath() + "/**")
                .addResourceLocations("file:" + svgStorageProperties.getUploadDir() + "/");
    }
}
