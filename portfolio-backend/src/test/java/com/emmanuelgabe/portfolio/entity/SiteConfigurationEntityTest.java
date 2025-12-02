package com.emmanuelgabe.portfolio.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SiteConfiguration entity validation constraints.
 */
class SiteConfigurationEntityTest {

    private Validator validator;
    private SiteConfiguration config;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        config = new SiteConfiguration();
        config.setFullName("Emmanuel Gabe");
        config.setEmail("contact@emmanuelgabe.com");
        config.setHeroTitle("Developpeur Backend");
        config.setHeroDescription("Je cree des applications web modernes et evolutives.");
        config.setSiteTitle("Portfolio - Emmanuel Gabe");
        config.setSeoDescription("Portfolio de Emmanuel Gabe, Developpeur Backend.");
        config.setGithubUrl("https://github.com/emmanuelgabe");
        config.setLinkedinUrl("https://linkedin.com/in/egabe");
    }

    // ========== Valid Entity Tests ==========

    @Test
    void should_haveNoConstraintViolations_when_configIsValid() {
        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveNoConstraintViolations_when_profileImagePathIsNull() {
        // Arrange
        config.setProfileImagePath(null);

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isEmpty();
    }

    // ========== Full Name Validation Tests ==========

    @Test
    void should_haveConstraintViolation_when_fullNameIsBlank() {
        // Arrange
        config.setFullName("");

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Full name is required"));
    }

    @Test
    void should_haveConstraintViolation_when_fullNameIsNull() {
        // Arrange
        config.setFullName(null);

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Full name is required"));
    }

    @Test
    void should_haveConstraintViolation_when_fullNameIsTooShort() {
        // Arrange
        config.setFullName("A");

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getMessage().contains("Full name must be between 2 and 100 characters"));
    }

    @Test
    void should_haveConstraintViolation_when_fullNameIsTooLong() {
        // Arrange
        config.setFullName("A".repeat(101));

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getMessage().contains("Full name must be between 2 and 100 characters"));
    }

    // ========== Email Validation Tests ==========

    @Test
    void should_haveConstraintViolation_when_emailIsBlank() {
        // Arrange
        config.setEmail("");

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Email is required"));
    }

    @Test
    void should_haveConstraintViolation_when_emailIsInvalid() {
        // Arrange
        config.setEmail("invalid-email");

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Invalid email format"));
    }

    // ========== Hero Title Validation Tests ==========

    @Test
    void should_haveConstraintViolation_when_heroTitleIsBlank() {
        // Arrange
        config.setHeroTitle("");

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Hero title is required"));
    }

    @Test
    void should_haveConstraintViolation_when_heroTitleIsTooShort() {
        // Arrange
        config.setHeroTitle("A");

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getMessage().contains("Hero title must be between 2 and 200 characters"));
    }

    @Test
    void should_haveConstraintViolation_when_heroTitleIsTooLong() {
        // Arrange
        config.setHeroTitle("A".repeat(201));

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getMessage().contains("Hero title must be between 2 and 200 characters"));
    }

    // ========== Hero Description Validation Tests ==========

    @Test
    void should_haveConstraintViolation_when_heroDescriptionIsBlank() {
        // Arrange
        config.setHeroDescription("");

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Hero description is required"));
    }

    // ========== Site Title Validation Tests ==========

    @Test
    void should_haveConstraintViolation_when_siteTitleIsBlank() {
        // Arrange
        config.setSiteTitle("");

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Site title is required"));
    }

    @Test
    void should_haveConstraintViolation_when_siteTitleIsTooLong() {
        // Arrange
        config.setSiteTitle("A".repeat(101));

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getMessage().contains("Site title must be between 2 and 100 characters"));
    }

    // ========== SEO Description Validation Tests ==========

    @Test
    void should_haveConstraintViolation_when_seoDescriptionIsBlank() {
        // Arrange
        config.setSeoDescription("");

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("SEO description is required"));
    }

    @Test
    void should_haveConstraintViolation_when_seoDescriptionIsTooLong() {
        // Arrange
        config.setSeoDescription("A".repeat(301));

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getMessage().contains("SEO description must not exceed 300 characters"));
    }

    // ========== GitHub URL Validation Tests ==========

    @Test
    void should_haveConstraintViolation_when_githubUrlIsBlank() {
        // Arrange
        config.setGithubUrl("");

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("GitHub URL is required"));
    }

    // ========== LinkedIn URL Validation Tests ==========

    @Test
    void should_haveConstraintViolation_when_linkedinUrlIsBlank() {
        // Arrange
        config.setLinkedinUrl("");

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("LinkedIn URL is required"));
    }

    // ========== Multiple Violations Tests ==========

    @Test
    void should_haveMultipleConstraintViolations_when_multipleFieldsInvalid() {
        // Arrange
        config.setFullName("");
        config.setEmail("invalid");
        config.setHeroTitle("");
        config.setGithubUrl("");

        // Act
        Set<ConstraintViolation<SiteConfiguration>> violations = validator.validate(config);

        // Assert
        assertThat(violations).hasSizeGreaterThanOrEqualTo(4);
    }
}
