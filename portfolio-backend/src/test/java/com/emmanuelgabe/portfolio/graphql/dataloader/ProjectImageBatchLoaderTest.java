package com.emmanuelgabe.portfolio.graphql.dataloader;

import com.emmanuelgabe.portfolio.dto.ProjectImageResponse;
import com.emmanuelgabe.portfolio.entity.ImageStatus;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.entity.ProjectImage;
import com.emmanuelgabe.portfolio.mapper.ProjectImageMapper;
import com.emmanuelgabe.portfolio.repository.ProjectImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProjectImageBatchLoader.
 */
@ExtendWith(MockitoExtension.class)
class ProjectImageBatchLoaderTest {

    @Mock
    private ProjectImageRepository projectImageRepository;

    @Mock
    private ProjectImageMapper projectImageMapper;

    @InjectMocks
    private ProjectImageBatchLoader projectImageBatchLoader;

    // ========== loadImagesForProjects Tests ==========

    @Test
    void should_returnImagesByProjectId_when_loadImagesForProjectsCalled() throws Exception {
        // Arrange
        Project project1 = new Project();
        project1.setId(1L);

        Project project2 = new Project();
        project2.setId(2L);

        ProjectImage image1 = createProjectImage(1L, project1);
        ProjectImage image2 = createProjectImage(2L, project1);
        ProjectImage image3 = createProjectImage(3L, project2);

        ProjectImageResponse response1 = createImageResponse(1L);
        ProjectImageResponse response2 = createImageResponse(2L);
        ProjectImageResponse response3 = createImageResponse(3L);

        when(projectImageRepository.findByProjectIdInOrderByDisplayOrderAsc(Set.of(1L, 2L)))
                .thenReturn(List.of(image1, image2, image3));
        when(projectImageMapper.toResponse(image1)).thenReturn(response1);
        when(projectImageMapper.toResponse(image2)).thenReturn(response2);
        when(projectImageMapper.toResponse(image3)).thenReturn(response3);

        // Act
        CompletionStage<Map<Long, List<ProjectImageResponse>>> result =
                projectImageBatchLoader.loadImagesForProjects(Set.of(1L, 2L));
        Map<Long, List<ProjectImageResponse>> imagesMap = result.toCompletableFuture().get();

        // Assert
        assertThat(imagesMap).hasSize(2);
        assertThat(imagesMap.get(1L)).hasSize(2);
        assertThat(imagesMap.get(2L)).hasSize(1);
    }

    @Test
    void should_returnEmptyListForProjectsWithoutImages_when_loadImagesForProjectsCalled() throws Exception {
        // Arrange
        when(projectImageRepository.findByProjectIdInOrderByDisplayOrderAsc(Set.of(1L, 2L)))
                .thenReturn(List.of());

        // Act
        CompletionStage<Map<Long, List<ProjectImageResponse>>> result =
                projectImageBatchLoader.loadImagesForProjects(Set.of(1L, 2L));
        Map<Long, List<ProjectImageResponse>> imagesMap = result.toCompletableFuture().get();

        // Assert
        assertThat(imagesMap).hasSize(2);
        assertThat(imagesMap.get(1L)).isEmpty();
        assertThat(imagesMap.get(2L)).isEmpty();
    }

    private ProjectImage createProjectImage(Long id, Project project) {
        ProjectImage image = new ProjectImage();
        image.setId(id);
        image.setProject(project);
        image.setImageUrl("http://example.com/image" + id + ".webp");
        image.setThumbnailUrl("http://example.com/thumb" + id + ".webp");
        image.setDisplayOrder(id.intValue());
        image.setPrimary(id == 1);
        image.setStatus(ImageStatus.READY);
        image.setUploadedAt(LocalDateTime.now());
        return image;
    }

    private ProjectImageResponse createImageResponse(Long id) {
        ProjectImageResponse response = new ProjectImageResponse();
        response.setId(id);
        response.setImageUrl("http://example.com/image" + id + ".webp");
        response.setThumbnailUrl("http://example.com/thumb" + id + ".webp");
        response.setDisplayOrder(id.intValue());
        response.setPrimary(id == 1);
        response.setStatus(ImageStatus.READY);
        response.setUploadedAt(LocalDateTime.now());
        return response;
    }
}
