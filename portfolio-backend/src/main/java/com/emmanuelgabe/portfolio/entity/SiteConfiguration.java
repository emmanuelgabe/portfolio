package com.emmanuelgabe.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Singleton entity for site-wide configuration.
 * Contains identity, hero section, SEO, and social links.
 * Only one row exists (id = 1), enforced by database constraint.
 */
@Entity
@Table(name = "site_configuration")
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SiteConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Identity
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false, length = 255)
    private String email;

    // Hero Section
    @NotBlank(message = "Hero title is required")
    @Size(min = 2, max = 200, message = "Hero title must be between 2 and 200 characters")
    @Column(name = "hero_title", nullable = false, length = 200)
    private String heroTitle;

    @NotBlank(message = "Hero description is required")
    @Column(name = "hero_description", nullable = false, columnDefinition = "TEXT")
    private String heroDescription;

    // SEO
    @NotBlank(message = "Site title is required")
    @Size(min = 2, max = 100, message = "Site title must be between 2 and 100 characters")
    @Column(name = "site_title", nullable = false, length = 100)
    private String siteTitle;

    @NotBlank(message = "SEO description is required")
    @Size(max = 300, message = "SEO description must not exceed 300 characters")
    @Column(name = "seo_description", nullable = false, length = 300)
    private String seoDescription;

    // Profile Image
    @Column(name = "profile_image_path", length = 500)
    private String profileImagePath;

    // Social Links
    @NotBlank(message = "GitHub URL is required")
    @Column(name = "github_url", nullable = false, length = 500)
    private String githubUrl;

    @NotBlank(message = "LinkedIn URL is required")
    @Column(name = "linkedin_url", nullable = false, length = 500)
    private String linkedinUrl;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
