package com.emmanuelgabe.portfolio.batch;

import com.emmanuelgabe.portfolio.entity.ImageStatus;
import com.emmanuelgabe.portfolio.entity.Project;
import com.emmanuelgabe.portfolio.entity.ProjectImage;
import com.emmanuelgabe.portfolio.repository.ArticleImageRepository;
import com.emmanuelgabe.portfolio.repository.ProjectImageRepository;
import com.emmanuelgabe.portfolio.service.ImageProcessorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ImageReprocessingJobConfig.
 * Tests the batch job components: reader, processor, and writer.
 */
@ExtendWith(MockitoExtension.class)
class ImageReprocessingJobConfigTest {

    @Mock
    private ProjectImageRepository projectImageRepository;

    @Mock
    private ArticleImageRepository articleImageRepository;

    @Mock
    private ImageProcessorService imageProcessorService;

    // ========== Processor Tests ==========

    @Test
    void should_returnReprocessingItem_when_projectImageProcessorCalledWithOriginalFile() {
        // Arrange
        ProjectImage image = createTestProjectImage(1L, "/uploads/projects/test.webp", "/uploads/projects/test_thumb.webp");

        when(imageProcessorService.hasOriginalFile(image.getImageUrl())).thenReturn(true);
        when(imageProcessorService.resolveOriginalPathFromUrl(image.getImageUrl()))
                .thenReturn(Path.of("/uploads/projects/test.original"));
        when(imageProcessorService.resolveOptimizedPathFromUrl(image.getImageUrl()))
                .thenReturn(Path.of("/uploads/projects/test.webp"));
        when(imageProcessorService.resolveThumbnailPathFromUrl(image.getThumbnailUrl()))
                .thenReturn(Path.of("/uploads/projects/test_thumb.webp"));

        ImageReprocessingJobConfig config = new ImageReprocessingJobConfig(
                projectImageRepository, articleImageRepository, imageProcessorService);

        // Act
        var processor = config.projectImageProcessor();
        ImageReprocessingJobConfig.ReprocessingItem result;
        try {
            result = processor.process(image);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImageId()).isEqualTo(1L);
        assertThat(result.getImageType()).isEqualTo(ImageReprocessingJobConfig.ImageType.PROJECT);
        assertThat(result.getOriginalPath()).isEqualTo(Path.of("/uploads/projects/test.original"));
    }

    @Test
    void should_returnNull_when_projectImageProcessorCalledWithoutOriginalFile() {
        // Arrange
        ProjectImage image = createTestProjectImage(1L, "/uploads/projects/test.webp", "/uploads/projects/test_thumb.webp");

        when(imageProcessorService.hasOriginalFile(image.getImageUrl())).thenReturn(false);

        ImageReprocessingJobConfig config = new ImageReprocessingJobConfig(
                projectImageRepository, articleImageRepository, imageProcessorService);

        // Act
        var processor = config.projectImageProcessor();
        ImageReprocessingJobConfig.ReprocessingItem result;
        try {
            result = processor.process(image);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void should_identifyCarouselImage_when_displayOrderGreaterThanZero() {
        // Arrange
        ProjectImage image = createTestProjectImage(1L, "/uploads/projects/carousel.webp", "/uploads/projects/carousel_thumb.webp");
        image.setDisplayOrder(2);

        when(imageProcessorService.hasOriginalFile(image.getImageUrl())).thenReturn(true);
        when(imageProcessorService.resolveOriginalPathFromUrl(any())).thenReturn(Path.of("/test.original"));
        when(imageProcessorService.resolveOptimizedPathFromUrl(any())).thenReturn(Path.of("/test.webp"));
        when(imageProcessorService.resolveThumbnailPathFromUrl(any())).thenReturn(Path.of("/test_thumb.webp"));

        ImageReprocessingJobConfig config = new ImageReprocessingJobConfig(
                projectImageRepository, articleImageRepository, imageProcessorService);

        // Act
        var processor = config.projectImageProcessor();
        ImageReprocessingJobConfig.ReprocessingItem result;
        try {
            result = processor.process(image);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isCarousel()).isTrue();
    }

    // ========== Reader Tests ==========

    @Test
    void should_returnImages_when_projectImageReaderCalledWithReadyImages() throws Exception {
        // Arrange
        List<ProjectImage> images = List.of(
                createTestProjectImage(1L, "/url1.webp", "/thumb1.webp"),
                createTestProjectImage(2L, "/url2.webp", "/thumb2.webp")
        );
        when(projectImageRepository.findByStatus(ImageStatus.READY)).thenReturn(images);

        ImageReprocessingJobConfig config = new ImageReprocessingJobConfig(
                projectImageRepository, articleImageRepository, imageProcessorService);

        // Act
        var reader = config.projectImageReader();
        ProjectImage first = reader.read();
        ProjectImage second = reader.read();
        ProjectImage third = reader.read();

        // Assert
        assertThat(first).isNotNull();
        assertThat(first.getId()).isEqualTo(1L);
        assertThat(second).isNotNull();
        assertThat(second.getId()).isEqualTo(2L);
        assertThat(third).isNull();
    }

    @Test
    void should_returnNull_when_projectImageReaderCalledWithNoImages() throws Exception {
        // Arrange
        when(projectImageRepository.findByStatus(ImageStatus.READY)).thenReturn(Collections.emptyList());

        ImageReprocessingJobConfig config = new ImageReprocessingJobConfig(
                projectImageRepository, articleImageRepository, imageProcessorService);

        // Act
        var reader = config.projectImageReader();
        ProjectImage result = reader.read();

        // Assert
        assertThat(result).isNull();
    }

    // ========== Writer Tests ==========

    @Test
    void should_callReprocessFromOriginal_when_writerProcessesItems() throws Exception {
        // Arrange
        ImageReprocessingJobConfig config = new ImageReprocessingJobConfig(
                projectImageRepository, articleImageRepository, imageProcessorService);

        var item = ImageReprocessingJobConfig.ReprocessingItem.builder()
                .imageId(1L)
                .imageType(ImageReprocessingJobConfig.ImageType.PROJECT)
                .originalPath(Path.of("/test.original"))
                .optimizedPath(Path.of("/test.webp"))
                .thumbnailPath(Path.of("/test_thumb.webp"))
                .isCarousel(false)
                .build();

        // Act
        var writer = config.imageReprocessingWriter();
        writer.write(new org.springframework.batch.item.Chunk<>(List.of(item)));

        // Assert
        verify(imageProcessorService).reprocessFromOriginal(
                Path.of("/test.original"),
                Path.of("/test.webp"),
                Path.of("/test_thumb.webp"),
                false
        );
    }

    @Test
    void should_passCarouselFlag_when_writerProcessesCarouselImage() throws Exception {
        // Arrange
        ImageReprocessingJobConfig config = new ImageReprocessingJobConfig(
                projectImageRepository, articleImageRepository, imageProcessorService);

        var item = ImageReprocessingJobConfig.ReprocessingItem.builder()
                .imageId(1L)
                .imageType(ImageReprocessingJobConfig.ImageType.PROJECT)
                .originalPath(Path.of("/carousel.original"))
                .optimizedPath(Path.of("/carousel.webp"))
                .thumbnailPath(Path.of("/carousel_thumb.webp"))
                .isCarousel(true)
                .build();

        // Act
        var writer = config.imageReprocessingWriter();
        writer.write(new org.springframework.batch.item.Chunk<>(List.of(item)));

        // Assert
        verify(imageProcessorService).reprocessFromOriginal(
                Path.of("/carousel.original"),
                Path.of("/carousel.webp"),
                Path.of("/carousel_thumb.webp"),
                true
        );
    }

    // ========== Helper Methods ==========

    private ProjectImage createTestProjectImage(Long id, String imageUrl, String thumbnailUrl) {
        Project project = new Project();
        project.setId(1L);

        ProjectImage image = new ProjectImage();
        image.setId(id);
        image.setProject(project);
        image.setImageUrl(imageUrl);
        image.setThumbnailUrl(thumbnailUrl);
        image.setStatus(ImageStatus.READY);
        image.setDisplayOrder(0);
        return image;
    }
}
