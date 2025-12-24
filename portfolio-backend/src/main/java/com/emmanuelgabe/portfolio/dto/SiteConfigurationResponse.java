package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for site configuration.
 * Contains all site-wide configuration values.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteConfigurationResponse {

    private Long id;
    private String fullName;
    private String email;
    private String heroTitle;
    private String heroDescription;
    private String siteTitle;
    private String seoDescription;
    private String profileImageUrl;
    private String githubUrl;
    private String linkedinUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
