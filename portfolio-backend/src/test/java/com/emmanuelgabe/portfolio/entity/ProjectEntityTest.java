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
    void should_haveNoConstraintViolations_when_projectIsValid() {
        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveConstraintViolation_when_titleIsBlank() {
        // Arrange
        project.setTitle("");

        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Title is required"));
    }

    @Test
    void should_haveConstraintViolation_when_titleIsNull() {
        // Arrange
        project.setTitle(null);

        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Title is required");
    }

    @Test
    void should_haveConstraintViolation_when_titleIsTooShort() {
        // Arrange
        project.setTitle("AB"); // Less than 3 characters

        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Title must be between 3 and 100 characters");
    }

    @Test
    void should_haveConstraintViolation_when_titleIsTooLong() {
        // Arrange
        project.setTitle("A".repeat(101)); // More than 100 characters

        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Title must be between 3 and 100 characters");
    }

    @Test
    void should_haveConstraintViolation_when_descriptionIsBlank() {
        // Arrange
        project.setDescription("");

        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Description is required"));
    }

    @Test
    void should_haveConstraintViolation_when_descriptionIsTooShort() {
        // Arrange
        project.setDescription("Too short"); // Less than 10 characters

        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Description must be between 10 and 2000 characters");
    }

    @Test
    void should_haveConstraintViolation_when_descriptionIsTooLong() {
        // Arrange
        project.setDescription("A".repeat(2001)); // More than 2000 characters

        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Description must be between 10 and 2000 characters");
    }

    @Test
    void should_haveNoViolation_when_techStackIsBlank() {
        // Arrange - techStack is now optional (nullable)
        project.setTechStack("");

        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert - no violations since techStack is optional
        assertThat(violations).isEmpty();
    }

    @Test
    void should_haveConstraintViolation_when_techStackIsTooLong() {
        // Arrange
        project.setTechStack("A".repeat(501)); // More than 500 characters

        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Tech stack cannot exceed 500 characters");
    }

    @Test
    void should_haveConstraintViolation_when_githubUrlIsTooLong() {
        // Arrange
        project.setGithubUrl("https://github.com/" + "A".repeat(250)); // More than 255 characters

        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("GitHub URL cannot exceed 255 characters");
    }

    @Test
    void should_haveConstraintViolation_when_imageUrlIsTooLong() {
        // Arrange
        project.setImageUrl("https://example.com/" + "A".repeat(250)); // More than 255 characters

        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Image URL cannot exceed 255 characters");
    }

    @Test
    void should_haveConstraintViolation_when_demoUrlIsTooLong() {
        // Arrange
        project.setDemoUrl("https://demo.com/" + "A".repeat(250)); // More than 255 characters

        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("Demo URL cannot exceed 255 characters");
    }

    @Test
    void should_addTag_when_addTagCalled() {
        // Act
        project.addTag(tag);

        // Assert
        assertThat(project.getTags()).contains(tag);
        assertThat(tag.getProjects()).contains(project);
    }

    @Test
    void should_throwIllegalArgumentException_when_addingNullTag() {
        // Act & Assert
        assertThatThrownBy(() -> project.addTag(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag cannot be null");
    }

    @Test
    void should_removeTag_when_removeTagCalled() {
        // Arrange
        project.addTag(tag);

        // Act
        project.removeTag(tag);

        // Assert
        assertThat(project.getTags()).doesNotContain(tag);
        assertThat(tag.getProjects()).doesNotContain(project);
    }

    @Test
    void should_throwIllegalArgumentException_when_removingNullTag() {
        // Act & Assert
        assertThatThrownBy(() -> project.removeTag(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag cannot be null");
    }

    @Test
    void should_markProjectAsFeatured_when_hasTagsAndImage() {
        // Arrange
        project.addTag(tag);
        project.setImageUrl("https://example.com/image.jpg");

        // Act
        project.markAsFeatured();

        // Assert
        assertThat(project.isFeatured()).isTrue();
    }

    @Test
    void should_throwIllegalStateException_when_markingAsFeaturedWithoutTags() {
        // Arrange
        project.setImageUrl("https://example.com/image.jpg");

        // Act & Assert
        assertThatThrownBy(() -> project.markAsFeatured())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot feature project without tags");
    }

    @Test
    void should_throwIllegalStateException_when_markingAsFeaturedWithoutImageUrl() {
        // Arrange
        project.addTag(tag);
        project.setImageUrl(null);

        // Act & Assert
        assertThatThrownBy(() -> project.markAsFeatured())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot feature project without an image");
    }

    @Test
    void should_throwIllegalStateException_when_markingAsFeaturedWithBlankImageUrl() {
        // Arrange
        project.addTag(tag);
        project.setImageUrl("   ");

        // Act & Assert
        assertThatThrownBy(() -> project.markAsFeatured())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot feature project without an image");
    }

    @Test
    void should_unfeatureProject_when_unfeatureCalled() {
        // Arrange
        project.addTag(tag);
        project.setImageUrl("https://example.com/image.jpg");
        project.markAsFeatured();

        // Act
        project.unfeature();

        // Assert
        assertThat(project.isFeatured()).isFalse();
    }

    @Test
    void should_returnTrue_when_canBeFeaturedWithTagsAndImage() {
        // Arrange
        project.addTag(tag);
        project.setImageUrl("https://example.com/image.jpg");

        // Act
        boolean canBeFeatured = project.canBeFeatured();

        // Assert
        assertThat(canBeFeatured).isTrue();
    }

    @Test
    void should_returnFalse_when_canBeFeaturedWithoutTags() {
        // Arrange
        project.setImageUrl("https://example.com/image.jpg");

        // Act
        boolean canBeFeatured = project.canBeFeatured();

        // Assert
        assertThat(canBeFeatured).isFalse();
    }

    @Test
    void should_returnFalse_when_canBeFeaturedWithoutImageUrl() {
        // Arrange
        project.addTag(tag);
        project.setImageUrl(null);

        // Act
        boolean canBeFeatured = project.canBeFeatured();

        // Assert
        assertThat(canBeFeatured).isFalse();
    }

    @Test
    void should_returnFalse_when_canBeFeaturedWithBlankImageUrl() {
        // Arrange
        project.addTag(tag);
        project.setImageUrl("   ");

        // Act
        boolean canBeFeatured = project.canBeFeatured();

        // Assert
        assertThat(canBeFeatured).isFalse();
    }

    @Test
    void should_haveFeaturedSetToFalse_when_projectIsNew() {
        // Arrange
        Project newProject = new Project();

        // Assert
        assertThat(newProject.isFeatured()).isFalse();
    }

    @Test
    void should_haveEmptyTagsSet_when_projectIsNew() {
        // Arrange
        Project newProject = new Project();

        // Assert
        assertThat(newProject.getTags()).isNotNull();
        assertThat(newProject.getTags()).isEmpty();
    }

    @Test
    void should_haveMultipleConstraintViolations_when_multipleFieldsInvalid() {
        // Arrange
        project.setTitle(""); // Blank - triggers both @NotBlank and @Size
        project.setDescription("Short"); // Too short
        project.setTechStack(null); // Null

        // Act
        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        // Assert
        assertThat(violations).hasSizeGreaterThanOrEqualTo(3);
    }
}
