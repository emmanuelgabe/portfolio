package com.emmanuelgabe.portfolio.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectEntityTest {

    private Validator validator;
    private Project project;
    private Tag tag;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        project = new Project();
        project.setTitle("Test Project");
        project.setDescription("This is a valid test description with sufficient length");
        project.setTechStack("Java, Spring Boot, PostgreSQL");

        tag = new Tag();
        tag.setId(1L);
        tag.setName("Java");
        tag.setColor("#007396");
    }

    @Test
    void whenValidProject_thenNoConstraintViolations() {
        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void whenTitleIsBlank_thenConstraintViolation() {
        // Given
        project.setTitle("");

        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Title is required"));
    }

    @Test
    void whenTitleIsNull_thenConstraintViolation() {
        // Given
        project.setTitle(null);

        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Title is required");
    }

    @Test
    void whenTitleIsTooShort_thenConstraintViolation() {
        // Given
        project.setTitle("AB"); // Less than 3 characters

        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Title must be between 3 and 100 characters");
    }

    @Test
    void whenTitleIsTooLong_thenConstraintViolation() {
        // Given
        project.setTitle("A".repeat(101)); // More than 100 characters

        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Title must be between 3 and 100 characters");
    }

    @Test
    void whenDescriptionIsBlank_thenConstraintViolation() {
        // Given
        project.setDescription("");

        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Description is required"));
    }

    @Test
    void whenDescriptionIsTooShort_thenConstraintViolation() {
        // Given
        project.setDescription("Too short"); // Less than 10 characters

        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Description must be between 10 and 2000 characters");
    }

    @Test
    void whenDescriptionIsTooLong_thenConstraintViolation() {
        // Given
        project.setDescription("A".repeat(2001)); // More than 2000 characters

        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Description must be between 10 and 2000 characters");
    }

    @Test
    void whenTechStackIsBlank_thenConstraintViolation() {
        // Given
        project.setTechStack("");

        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Tech stack is required");
    }

    @Test
    void whenTechStackIsTooLong_thenConstraintViolation() {
        // Given
        project.setTechStack("A".repeat(501)); // More than 500 characters

        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Tech stack cannot exceed 500 characters");
    }

    @Test
    void whenGithubUrlIsTooLong_thenConstraintViolation() {
        // Given
        project.setGithubUrl("https://github.com/" + "A".repeat(250)); // More than 255 characters

        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("GitHub URL cannot exceed 255 characters");
    }

    @Test
    void whenImageUrlIsTooLong_thenConstraintViolation() {
        // Given
        project.setImageUrl("https://example.com/" + "A".repeat(250)); // More than 255 characters

        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Image URL cannot exceed 255 characters");
    }

    @Test
    void whenDemoUrlIsTooLong_thenConstraintViolation() {
        // Given
        project.setDemoUrl("https://demo.com/" + "A".repeat(250)); // More than 255 characters

        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Demo URL cannot exceed 255 characters");
    }

    @Test
    void whenAddTag_thenTagIsAdded() {
        // When
        project.addTag(tag);

        // Then
        assertThat(project.getTags()).contains(tag);
        assertThat(tag.getProjects()).contains(project);
    }

    @Test
    void whenAddNullTag_thenThrowsIllegalArgumentException() {
        // When & Then
        assertThatThrownBy(() -> project.addTag(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag cannot be null");
    }

    @Test
    void whenRemoveTag_thenTagIsRemoved() {
        // Given
        project.addTag(tag);

        // When
        project.removeTag(tag);

        // Then
        assertThat(project.getTags()).doesNotContain(tag);
        assertThat(tag.getProjects()).doesNotContain(project);
    }

    @Test
    void whenRemoveNullTag_thenThrowsIllegalArgumentException() {
        // When & Then
        assertThatThrownBy(() -> project.removeTag(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag cannot be null");
    }

    @Test
    void whenMarkAsFeatured_withTagsAndImage_thenProjectIsFeatured() {
        // Given
        project.addTag(tag);
        project.setImageUrl("https://example.com/image.jpg");

        // When
        project.markAsFeatured();

        // Then
        assertThat(project.isFeatured()).isTrue();
    }

    @Test
    void whenMarkAsFeatured_withoutTags_thenThrowsIllegalStateException() {
        // Given
        project.setImageUrl("https://example.com/image.jpg");

        // When & Then
        assertThatThrownBy(() -> project.markAsFeatured())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot feature project without tags");
    }

    @Test
    void whenMarkAsFeatured_withoutImageUrl_thenThrowsIllegalStateException() {
        // Given
        project.addTag(tag);
        project.setImageUrl(null);

        // When & Then
        assertThatThrownBy(() -> project.markAsFeatured())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot feature project without an image URL");
    }

    @Test
    void whenMarkAsFeatured_withBlankImageUrl_thenThrowsIllegalStateException() {
        // Given
        project.addTag(tag);
        project.setImageUrl("   ");

        // When & Then
        assertThatThrownBy(() -> project.markAsFeatured())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot feature project without an image URL");
    }

    @Test
    void whenUnfeature_thenProjectIsNotFeatured() {
        // Given
        project.addTag(tag);
        project.setImageUrl("https://example.com/image.jpg");
        project.markAsFeatured();

        // When
        project.unfeature();

        // Then
        assertThat(project.isFeatured()).isFalse();
    }

    @Test
    void whenCanBeFeatured_withTagsAndImage_thenReturnsTrue() {
        // Given
        project.addTag(tag);
        project.setImageUrl("https://example.com/image.jpg");

        // When
        boolean canBeFeatured = project.canBeFeatured();

        // Then
        assertThat(canBeFeatured).isTrue();
    }

    @Test
    void whenCanBeFeatured_withoutTags_thenReturnsFalse() {
        // Given
        project.setImageUrl("https://example.com/image.jpg");

        // When
        boolean canBeFeatured = project.canBeFeatured();

        // Then
        assertThat(canBeFeatured).isFalse();
    }

    @Test
    void whenCanBeFeatured_withoutImageUrl_thenReturnsFalse() {
        // Given
        project.addTag(tag);
        project.setImageUrl(null);

        // When
        boolean canBeFeatured = project.canBeFeatured();

        // Then
        assertThat(canBeFeatured).isFalse();
    }

    @Test
    void whenCanBeFeatured_withBlankImageUrl_thenReturnsFalse() {
        // Given
        project.addTag(tag);
        project.setImageUrl("   ");

        // When
        boolean canBeFeatured = project.canBeFeatured();

        // Then
        assertThat(canBeFeatured).isFalse();
    }

    @Test
    void whenNewProject_thenFeaturedIsFalse() {
        // Given
        Project newProject = new Project();

        // Then
        assertThat(newProject.isFeatured()).isFalse();
    }

    @Test
    void whenNewProject_thenTagsIsEmptySet() {
        // Given
        Project newProject = new Project();

        // Then
        assertThat(newProject.getTags()).isNotNull();
        assertThat(newProject.getTags()).isEmpty();
    }

    @Test
    void testMultipleConstraintViolations() {
        // Given
        project.setTitle(""); // Blank - triggers both @NotBlank and @Size
        project.setDescription("Short"); // Too short
        project.setTechStack(null); // Null

        // When
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Then
        assertThat(violations).hasSizeGreaterThanOrEqualTo(3);
    }
}
