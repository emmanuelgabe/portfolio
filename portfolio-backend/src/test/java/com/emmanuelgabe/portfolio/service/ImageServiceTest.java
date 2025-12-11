package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.config.ImageStorageProperties;
import com.emmanuelgabe.portfolio.dto.ImageUploadResponse;
import com.emmanuelgabe.portfolio.exception.FileStorageException;
import com.emmanuelgabe.portfolio.messaging.event.ImageProcessingEvent;
import com.emmanuelgabe.portfolio.messaging.publisher.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ImageStorageProperties storageProperties;

    @Mock
    private EventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<ImageProcessingEvent> eventCaptor;

    @InjectMocks
    private ImageService imageService;

    @TempDir
    Path tempDir;

    private MultipartFile validJpegFile;
    private MultipartFile validPngFile;
    private MultipartFile invalidFile;
    private MultipartFile emptyFile;

    @BeforeEach
    void setUp() throws IOException {
        validJpegFile = new MockMultipartFile(
                "file", "test_image.jpg", "image/jpeg", createValidJpegBytes());

        validPngFile = new MockMultipartFile(
                "file", "test_image.png", "image/png", createValidPngBytes());

        invalidFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", new byte[]{0x00, 0x01, 0x02, 0x03});

        emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        lenient().when(storageProperties.getUploadDir()).thenReturn(tempDir.toString());
        lenient().when(storageProperties.getBasePath()).thenReturn("/uploads/projects");
        lenient().when(storageProperties.getMaxFileSize()).thenReturn(10485760L);
        lenient().when(storageProperties.getImageMaxWidth()).thenReturn(1200);
        lenient().when(storageProperties.getThumbnailSize()).thenReturn(300);
        lenient().when(storageProperties.getJpegQuality()).thenReturn(0.85f);
        lenient().when(storageProperties.getThumbnailQuality()).thenReturn(0.80f);
        lenient().when(storageProperties.getAllowedExtensions()).thenReturn(new String[]{"jpg", "jpeg", "png", "webp"});
        lenient().when(storageProperties.getAllowedMimeTypes()).thenReturn(new String[]{"image/jpeg", "image/png", "image/webp"});

        imageService.init();
    }

    // ========== Upload Project Image Tests ==========

    @Test
    void should_uploadProjectImage_when_validJpegFile() throws IOException {
        // Arrange
        Long projectId = 1L;

        // Act
        ImageUploadResponse result = imageService.uploadProjectImage(projectId, validJpegFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).contains("/uploads/projects/project_1_");
        assertThat(result.getImageUrl()).endsWith(".webp");
        assertThat(result.getThumbnailUrl()).contains("_thumb.webp");
        assertThat(result.getFileSize()).isGreaterThan(0);

        verify(eventPublisher).publishImageEvent(eventCaptor.capture());
        ImageProcessingEvent event = eventCaptor.getValue();
        assertThat(event.getProcessingType()).isEqualTo(ImageProcessingEvent.ImageProcessingType.PROJECT);
        assertThat(event.getEntityId()).isEqualTo(projectId);
    }

    @Test
    void should_uploadProjectImage_when_validPngFile() throws IOException {
        // Arrange
        Long projectId = 1L;

        // Act
        ImageUploadResponse result = imageService.uploadProjectImage(projectId, validPngFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).endsWith(".webp");
        verify(eventPublisher).publishImageEvent(eventCaptor.capture());
    }

    @Test
    void should_createTempFile_when_uploadProjectImageCalled() throws IOException {
        // Arrange
        Long projectId = 1L;

        // Act
        imageService.uploadProjectImage(projectId, validJpegFile);

        // Assert - verify temp file was created
        long tempFileCount = Files.list(tempDir)
                .filter(p -> p.toString().endsWith(".tmp"))
                .count();
        assertThat(tempFileCount).isEqualTo(1);
    }

    @Test
    void should_throwException_when_fileIsEmpty() {
        // Act / Assert
        assertThatThrownBy(() -> imageService.uploadProjectImage(1L, emptyFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void should_throwException_when_invalidFileType() {
        // Act / Assert
        assertThatThrownBy(() -> imageService.uploadProjectImage(1L, invalidFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("File type not allowed");
    }

    @Test
    void should_throwException_when_pathTraversalDetected() throws IOException {
        // Arrange
        MultipartFile pathTraversalFile = new MockMultipartFile(
                "file", "../../../etc/passwd.jpg", "image/jpeg", createValidJpegBytes());

        // Act / Assert
        assertThatThrownBy(() -> imageService.uploadProjectImage(1L, pathTraversalFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("path traversal");
    }

    @Test
    void should_throwException_when_fileSizeExceedsLimit() {
        // Arrange
        lenient().when(storageProperties.getMaxFileSize()).thenReturn(100L);
        byte[] largeContent = new byte[200];
        largeContent[0] = (byte) 0xFF;
        largeContent[1] = (byte) 0xD8;
        largeContent[2] = (byte) 0xFF;

        MultipartFile largeFile = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", largeContent);

        // Act / Assert
        assertThatThrownBy(() -> imageService.uploadProjectImage(1L, largeFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("exceeds maximum");
    }

    // ========== Delete Project Image Tests ==========

    @Test
    void should_deleteProjectImage_when_projectHasImages() throws IOException {
        // Arrange
        Long projectId = 1L;
        Path testFile = tempDir.resolve("project_1_test.webp");
        Files.createFile(testFile);
        assertThat(Files.list(tempDir).count()).isEqualTo(1);

        // Act
        imageService.deleteProjectImage(projectId);

        // Assert
        assertThat(Files.list(tempDir).count()).isEqualTo(0);
    }

    @Test
    void should_notFail_when_deletingProjectWithNoImages() {
        // Act / Assert - Should not throw
        imageService.deleteProjectImage(999L);
    }

    @Test
    void should_deleteProjectImageByUrl_when_mainImageUrlProvided() throws IOException {
        // Arrange
        Path mainFile = tempDir.resolve("project_1_test.webp");
        Path thumbFile = tempDir.resolve("project_1_test_thumb.webp");
        Files.createFile(mainFile);
        Files.createFile(thumbFile);

        // Act
        imageService.deleteProjectImageByUrl("/uploads/projects/project_1_test.webp");

        // Assert
        assertThat(Files.exists(mainFile)).isFalse();
        assertThat(Files.exists(thumbFile)).isFalse();
    }

    // ========== Delete Article Image Tests ==========

    @Test
    void should_deleteArticleImage_when_mainImageUrlProvided() throws IOException {
        // Arrange
        Path mainFile = tempDir.resolve("article_1_test.webp");
        Path thumbFile = tempDir.resolve("article_1_test_thumb.webp");
        Files.createFile(mainFile);
        Files.createFile(thumbFile);

        // Act
        imageService.deleteArticleImage("/uploads/projects/article_1_test.webp");

        // Assert
        assertThat(Files.exists(mainFile)).isFalse();
        assertThat(Files.exists(thumbFile)).isFalse();
    }

    @Test
    void should_deleteArticleImage_when_thumbnailUrlProvided() throws IOException {
        // Arrange
        Path mainFile = tempDir.resolve("article_1_test.webp");
        Path thumbFile = tempDir.resolve("article_1_test_thumb.webp");
        Files.createFile(mainFile);
        Files.createFile(thumbFile);

        // Act
        imageService.deleteArticleImage("/uploads/projects/article_1_test_thumb.webp");

        // Assert
        assertThat(Files.exists(mainFile)).isFalse();
        assertThat(Files.exists(thumbFile)).isFalse();
    }

    // ========== Upload Profile Image Tests ==========

    @Test
    void should_uploadProfileImage_when_validJpegFile() throws IOException {
        // Act
        ImageUploadResponse result = imageService.uploadProfileImage(validJpegFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).contains("/uploads/projects/profile_");
        assertThat(result.getImageUrl()).endsWith(".webp");
        assertThat(result.getThumbnailUrl()).isNull(); // Profile has no thumbnail

        verify(eventPublisher).publishImageEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getProcessingType())
                .isEqualTo(ImageProcessingEvent.ImageProcessingType.PROFILE);
    }

    @Test
    void should_uploadProfileImage_when_validPngFile() throws IOException {
        // Act
        ImageUploadResponse result = imageService.uploadProfileImage(validPngFile);

        // Assert
        assertThat(result).isNotNull();
        verify(eventPublisher).publishImageEvent(eventCaptor.capture());
    }

    @Test
    void should_deleteOldProfileImage_when_uploadingNewProfileImage() throws IOException {
        // Arrange
        Path oldProfile = tempDir.resolve("profile_old.webp");
        Files.createFile(oldProfile);
        assertThat(Files.exists(oldProfile)).isTrue();

        // Act
        imageService.uploadProfileImage(validJpegFile);

        // Assert - old profile should be deleted
        assertThat(Files.exists(oldProfile)).isFalse();
    }

    // ========== Delete Profile Image Tests ==========

    @Test
    void should_deleteProfileImage_when_fileNameProvided() throws IOException {
        // Arrange
        Path profileFile = tempDir.resolve("profile_test.webp");
        Files.createFile(profileFile);

        // Act
        imageService.deleteProfileImage("profile_test.webp");

        // Assert
        assertThat(Files.exists(profileFile)).isFalse();
    }

    @Test
    void should_notFail_when_deletingNonExistentProfileImage() {
        // Act / Assert - Should not throw
        imageService.deleteProfileImage("nonexistent.webp");
    }

    // ========== Helper Methods ==========

    private byte[] createValidJpegBytes() throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    private byte[] createValidPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
