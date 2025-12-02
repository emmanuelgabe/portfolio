package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.ProjectImageResponse;
import com.emmanuelgabe.portfolio.dto.UpdateProjectImageRequest;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.entity.ProjectImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ProjectImageMapper.
 */
class ProjectImageMapperTest {

    private ProjectImageMapper mapper;
    private Project project;
    private ProjectImage projectImage;
    private LocalDateTime uploadedAt;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(ProjectImageMapper.class);
        uploadedAt = LocalDateTime.now();

        project = new Project();
        project.setId(1L);
        project.setTitle("Test Project");

        projectImage = new ProjectImage();
        projectImage.setId(1L);
        projectImage.setProject(project);
        projectImage.setImageUrl("/uploads/projects/test.webp");
        projectImage.setThumbnailUrl("/uploads/projects/test_thumb.webp");
        projectImage.setAltText("Test alt text");
        projectImage.setCaption("Test caption");
        projectImage.setDisplayOrder(0);
        projectImage.setPrimary(true);
    }

    // ========== toResponse Tests ==========

    @Test
    void should_mapAllFields_when_toResponseCalledWithValidEntity() {
        // Arrange - use setUp data

        // Act
        ProjectImageResponse response = mapper.toResponse(projectImage);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getImageUrl()).isEqualTo("/uploads/projects/test.webp");
        assertThat(response.getThumbnailUrl()).isEqualTo("/uploads/projects/test_thumb.webp");
        assertThat(response.getAltText()).isEqualTo("Test alt text");
        assertThat(response.getCaption()).isEqualTo("Test caption");
        assertThat(response.getDisplayOrder()).isEqualTo(0);
        assertThat(response.isPrimary()).isTrue();
    }

    @Test
    void should_returnNull_when_toResponseCalledWithNull() {
        // Arrange & Act
        ProjectImageResponse response = mapper.toResponse(null);

        // Assert
        assertThat(response).isNull();
    }

    @Test
    void should_handleNullOptionalFields_when_toResponseCalledWithMinimalEntity() {
        // Arrange
        ProjectImage minimalImage = new ProjectImage();
        minimalImage.setId(2L);
        minimalImage.setImageUrl("/uploads/test.webp");
        minimalImage.setDisplayOrder(1);

        // Act
        ProjectImageResponse response = mapper.toResponse(minimalImage);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getImageUrl()).isEqualTo("/uploads/test.webp");
        assertThat(response.getThumbnailUrl()).isNull();
        assertThat(response.getAltText()).isNull();
        assertThat(response.getCaption()).isNull();
        assertThat(response.isPrimary()).isFalse();
    }

    // ========== toResponseList Tests ==========

    @Test
    void should_mapAllEntities_when_toResponseListCalledWithValidList() {
        // Arrange
        ProjectImage image2 = new ProjectImage();
        image2.setId(2L);
        image2.setImageUrl("/uploads/test2.webp");
        image2.setDisplayOrder(1);

        List<ProjectImage> images = Arrays.asList(projectImage, image2);

        // Act
        List<ProjectImageResponse> responses = mapper.toResponseList(images);

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void should_returnEmptyList_when_toResponseListCalledWithEmptyList() {
        // Arrange
        List<ProjectImage> emptyList = List.of();

        // Act
        List<ProjectImageResponse> responses = mapper.toResponseList(emptyList);

        // Assert
        assertThat(responses).isEmpty();
    }

    @Test
    void should_returnNull_when_toResponseListCalledWithNull() {
        // Arrange & Act
        List<ProjectImageResponse> responses = mapper.toResponseList(null);

        // Assert
        assertThat(responses).isNull();
    }

    // ========== updateFromRequest Tests ==========

    @Test
    void should_updateAltTextAndCaption_when_updateFromRequestCalledWithValidRequest() {
        // Arrange
        UpdateProjectImageRequest request = new UpdateProjectImageRequest("New alt", "New caption");

        // Act
        mapper.updateFromRequest(request, projectImage);

        // Assert
        assertThat(projectImage.getAltText()).isEqualTo("New alt");
        assertThat(projectImage.getCaption()).isEqualTo("New caption");
    }

    @Test
    void should_notUpdateOtherFields_when_updateFromRequestCalled() {
        // Arrange
        UpdateProjectImageRequest request = new UpdateProjectImageRequest("New alt", "New caption");
        String originalImageUrl = projectImage.getImageUrl();
        String originalThumbnailUrl = projectImage.getThumbnailUrl();
        int originalDisplayOrder = projectImage.getDisplayOrder();
        boolean originalPrimary = projectImage.isPrimary();

        // Act
        mapper.updateFromRequest(request, projectImage);

        // Assert - verify other fields remain unchanged
        assertThat(projectImage.getImageUrl()).isEqualTo(originalImageUrl);
        assertThat(projectImage.getThumbnailUrl()).isEqualTo(originalThumbnailUrl);
        assertThat(projectImage.getDisplayOrder()).isEqualTo(originalDisplayOrder);
        assertThat(projectImage.isPrimary()).isEqualTo(originalPrimary);
    }

    @Test
    void should_ignoreNullValues_when_updateFromRequestCalledWithPartialRequest() {
        // Arrange
        projectImage.setAltText("Original alt");
        projectImage.setCaption("Original caption");
        UpdateProjectImageRequest request = new UpdateProjectImageRequest(null, "Updated caption only");

        // Act
        mapper.updateFromRequest(request, projectImage);

        // Assert
        assertThat(projectImage.getAltText()).isEqualTo("Original alt");
        assertThat(projectImage.getCaption()).isEqualTo("Updated caption only");
    }

    @Test
    void should_handleEmptyStrings_when_updateFromRequestCalledWithEmptyValues() {
        // Arrange
        projectImage.setAltText("Original alt");
        UpdateProjectImageRequest request = new UpdateProjectImageRequest("", null);

        // Act
        mapper.updateFromRequest(request, projectImage);

        // Assert - empty string should be set (not ignored like null)
        assertThat(projectImage.getAltText()).isEmpty();
    }
}
