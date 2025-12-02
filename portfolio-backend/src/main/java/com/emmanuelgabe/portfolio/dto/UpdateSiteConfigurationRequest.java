package com.emmanuelgabe.portfolio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating site configuration.
 * Profile image is handled separately via upload endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSiteConfigurationRequest {

    // Identity
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    // Hero Section
    @NotBlank(message = "Hero title is required")
    @Size(min = 2, max = 200, message = "Hero title must be between 2 and 200 characters")
    private String heroTitle;

    @NotBlank(message = "Hero description is required")
    private String heroDescription;

    // SEO
    @NotBlank(message = "Site title is required")
    @Size(min = 2, max = 100, message = "Site title must be between 2 and 100 characters")
    private String siteTitle;

    @NotBlank(message = "SEO description is required")
    @Size(max = 300, message = "SEO description must not exceed 300 characters")
    private String seoDescription;

    // Social Links
    @NotBlank(message = "GitHub URL is required")
    @Pattern(regexp = "^https?://.*", message = "GitHub URL must be a valid URL")
    private String githubUrl;

    @NotBlank(message = "LinkedIn URL is required")
    @Pattern(regexp = "^https?://.*", message = "LinkedIn URL must be a valid URL")
    private String linkedinUrl;
}
