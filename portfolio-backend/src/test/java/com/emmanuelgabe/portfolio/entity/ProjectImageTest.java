package com.emmanuelgabe.portfolio.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ProjectImage entity.
 */
class ProjectImageTest {

    private Project project;
    private ProjectImage projectImage;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);
        project.setTitle("Test Project");
        project.setDescription("Test Description");
        project.setTechStack("Java, Spring");

        projectImage = new ProjectImage();
        projectImage.setProject(project);
        projectImage.setImageUrl("/uploads/projects/test.webp");
        projectImage.setThumbnailUrl("/uploads/projects/test_thumb.webp");
    }

    // ========== Constructor Tests ==========

    @Test
    void should_createProjectImage_when_usingDefaultConstructor() {
        // Arrange & Act
        ProjectImage image = new ProjectImage();

        // Assert
        assertThat(image.getId()).isNull();
        assertThat(image.getProject()).isNull();
        assertThat(image.getImageUrl()).isNull();
        assertThat(image.getDisplayOrder()).isEqualTo(0);
        assertThat(image.isPrimary()).isFalse();
    }

    @Test
    void should_createProjectImage_when_usingConstructorWithBasicParams() {
        // Arrange & Act
        ProjectImage image = new ProjectImage(project, "/uploads/image.webp", "/uploads/thumb.webp");

        // Assert
        assertThat(image.getProject()).isEqualTo(project);
        assertThat(image.getImageUrl()).isEqualTo("/uploads/image.webp");
        assertThat(image.getThumbnailUrl()).isEqualTo("/uploads/thumb.webp");
        assertThat(image.getAltText()).isNull();
        assertThat(image.getCaption()).isNull();
    }

    @Test
    void should_createProjectImage_when_usingConstructorWithAllParams() {
        // Arrange & Act
        ProjectImage image = new ProjectImage(
            project,
            "/uploads/image.webp",
            "/uploads/thumb.webp",
            "Alt text",
            "Caption text"
        );

        // Assert
        assertThat(image.getProject()).isEqualTo(project);
        assertThat(image.getImageUrl()).isEqualTo("/uploads/image.webp");
        assertThat(image.getThumbnailUrl()).isEqualTo("/uploads/thumb.webp");
        assertThat(image.getAltText()).isEqualTo("Alt text");
        assertThat(image.getCaption()).isEqualTo("Caption text");
    }

    // ========== Getter/Setter Tests ==========

    @Test
    void should_setAndGetAltText_when_altTextProvided() {
        // Arrange & Act
        projectImage.setAltText("Test alt text");

        // Assert
        assertThat(projectImage.getAltText()).isEqualTo("Test alt text");
    }

    @Test
    void should_setAndGetCaption_when_captionProvided() {
        // Arrange & Act
        projectImage.setCaption("Test caption");

        // Assert
        assertThat(projectImage.getCaption()).isEqualTo("Test caption");
    }

    @Test
    void should_setAndGetDisplayOrder_when_displayOrderProvided() {
        // Arrange & Act
        projectImage.setDisplayOrder(5);

        // Assert
        assertThat(projectImage.getDisplayOrder()).isEqualTo(5);
    }

    @Test
    void should_setAndGetPrimary_when_primaryFlagSet() {
        // Arrange & Act
        projectImage.setPrimary(true);

        // Assert
        assertThat(projectImage.isPrimary()).isTrue();
    }

    // ========== Equals/HashCode Tests ==========

    @Test
    void should_beEqual_when_sameId() {
        // Arrange
        ProjectImage image1 = new ProjectImage();
        image1.setId(1L);

        ProjectImage image2 = new ProjectImage();
        image2.setId(1L);

        // Act & Assert
        assertThat(image1).isEqualTo(image2);
        assertThat(image1.hashCode()).isEqualTo(image2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentId() {
        // Arrange
        ProjectImage image1 = new ProjectImage();
        image1.setId(1L);

        ProjectImage image2 = new ProjectImage();
        image2.setId(2L);

        // Act & Assert
        assertThat(image1).isNotEqualTo(image2);
    }

    @Test
    void should_notBeEqual_when_comparedWithNull() {
        // Arrange
        projectImage.setId(1L);

        // Act & Assert
        assertThat(projectImage).isNotEqualTo(null);
    }

    @Test
    void should_notBeEqual_when_comparedWithDifferentType() {
        // Arrange
        projectImage.setId(1L);

        // Act & Assert
        assertThat(projectImage).isNotEqualTo("string");
    }

    // ========== Default Values Tests ==========

    @Test
    void should_haveDefaultDisplayOrderZero_when_created() {
        // Arrange & Act
        ProjectImage image = new ProjectImage();

        // Assert
        assertThat(image.getDisplayOrder()).isEqualTo(0);
    }

    @Test
    void should_haveDefaultPrimaryFalse_when_created() {
        // Arrange & Act
        ProjectImage image = new ProjectImage();

        // Assert
        assertThat(image.isPrimary()).isFalse();
    }
}
