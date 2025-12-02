package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.config.ImageStorageProperties;
import com.emmanuelgabe.portfolio.dto.ImageUploadResponse;
import com.emmanuelgabe.portfolio.exception.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
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

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ImageStorageProperties storageProperties;

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
        // Create valid JPEG file (with JPEG magic bytes)
        byte[] jpegContent = createValidJpegBytes();
        validJpegFile = new MockMultipartFile(
                "file",
                "test_image.jpg",
                "image/jpeg",
                jpegContent
        );

        // Create valid PNG file (with PNG magic bytes)
        byte[] pngContent = createValidPngBytes();
        validPngFile = new MockMultipartFile(
                "file",
                "test_image.png",
                "image/png",
                pngContent
        );

        // Create invalid file (not an image)
        byte[] invalidContent = new byte[]{0x00, 0x01, 0x02, 0x03};
        invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                invalidContent
        );

        // Create empty file
        emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        // Setup storage properties
        lenient().when(storageProperties.getUploadDir()).thenReturn(tempDir.toString());
        lenient().when(storageProperties.getBasePath()).thenReturn("/uploads/projects");
        lenient().when(storageProperties.getMaxFileSize()).thenReturn(10485760L); // 10MB
        lenient().when(storageProperties.getImageMaxWidth()).thenReturn(1200);
        lenient().when(storageProperties.getThumbnailSize()).thenReturn(300);
        lenient().when(storageProperties.getJpegQuality()).thenReturn(0.85f);
        lenient().when(storageProperties.getThumbnailQuality()).thenReturn(0.80f);
        lenient().when(storageProperties.getAllowedExtensions()).thenReturn(new String[]{"jpg", "jpeg", "png", "webp"});
        lenient().when(storageProperties.getAllowedMimeTypes()).thenReturn(new String[]{"image/jpeg", "image/png", "image/webp"});

        // Initialize service (calls @PostConstruct)
        imageService.init();
    }

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
        assertThat(result.getThumbnailUrl()).contains("/uploads/projects/project_1_");
        assertThat(result.getThumbnailUrl()).contains("_thumb.webp");
        assertThat(result.getFileSize()).isGreaterThan(0);
        assertThat(result.getUploadedAt()).isNotNull();

        // Verify files were created
        assertThat(Files.list(tempDir).count()).isEqualTo(2); // Full image + thumbnail
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
        assertThat(result.getThumbnailUrl()).endsWith("_thumb.webp");

        // Verify files were created
        assertThat(Files.list(tempDir).count()).isEqualTo(2);
    }

    @Test
    void should_throwException_when_fileIsEmpty() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> imageService.uploadProjectImage(1L, emptyFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void should_throwException_when_invalidFileType() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> imageService.uploadProjectImage(1L, invalidFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("File type not allowed");
    }

    @Test
    void should_throwException_when_pathTraversalDetected() throws IOException {
        // Arrange
        byte[] jpegContent = createValidJpegBytes();
        MultipartFile pathTraversalFile = new MockMultipartFile(
                "file",
                "../../../etc/passwd.jpg",
                "image/jpeg",
                jpegContent
        );

        // Act / Assert
        assertThatThrownBy(() -> imageService.uploadProjectImage(1L, pathTraversalFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("path traversal");
    }

    @Test
    void should_throwException_when_fileSizeExceedsLimit() {
        // Arrange
        lenient().when(storageProperties.getMaxFileSize()).thenReturn(100L); // Very small limit
        byte[] largeContent = new byte[200];
        // Add JPEG magic bytes
        largeContent[0] = (byte) 0xFF;
        largeContent[1] = (byte) 0xD8;
        largeContent[2] = (byte) 0xFF;

        MultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        // Act / Assert
        assertThatThrownBy(() -> imageService.uploadProjectImage(1L, largeFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("exceeds maximum");
    }

    @Test
    void should_deleteOldImages_when_projectAlreadyHasImages() throws IOException {
        // Arrange
        Long projectId = 1L;

        // Upload first image
        imageService.uploadProjectImage(projectId, validJpegFile);
        long filesAfterFirstUpload = Files.list(tempDir).count();
        assertThat(filesAfterFirstUpload).isEqualTo(2);

        // Act - Upload second image (should delete first)
        imageService.uploadProjectImage(projectId, validPngFile);

        // Assert - Should still have only 2 files (old ones deleted, new ones created)
        long filesAfterSecondUpload = Files.list(tempDir).count();
        assertThat(filesAfterSecondUpload).isEqualTo(2);
    }

    @Test
    void should_deleteProjectImage_when_projectHasImages() throws IOException {
        // Arrange
        Long projectId = 1L;
        imageService.uploadProjectImage(projectId, validJpegFile);

        assertThat(Files.list(tempDir).count()).isEqualTo(2);

        // Act
        imageService.deleteProjectImage(projectId);

        // Assert
        assertThat(Files.list(tempDir).count()).isEqualTo(0);
    }

    @Test
    void should_notFail_when_deletingProjectWithNoImages() {
        // Arrange / Act / Assert - Should not throw exception
        imageService.deleteProjectImage(999L);
    }

    @Test
    void should_createThumbnailWithCorrectDimensions_when_uploadingImage() throws IOException {
        // Arrange
        Long projectId = 1L;

        // Act
        ImageUploadResponse result = imageService.uploadProjectImage(projectId, validJpegFile);

        // Assert
        // Find the thumbnail file
        String thumbnailFileName = result.getThumbnailUrl().substring(result.getThumbnailUrl().lastIndexOf("/") + 1);
        Path thumbnailPath = tempDir.resolve(thumbnailFileName);

        assertThat(Files.exists(thumbnailPath)).isTrue();

        // Verify thumbnail dimensions
        BufferedImage thumbnail = ImageIO.read(thumbnailPath.toFile());
        assertThat(thumbnail.getWidth()).isEqualTo(300);
        assertThat(thumbnail.getHeight()).isEqualTo(300);
    }

    /**
     * Helper method to create a valid JPEG byte array with magic bytes and actual image data
     */
    private byte[] createValidJpegBytes() throws IOException {
        // Create a small test image
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    /**
     * Helper method to create a valid PNG byte array with magic bytes and actual image data
     */
    private byte[] createValidPngBytes() throws IOException {
        // Create a small test image
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    // ========== Article Image Tests ==========

    @Test
    void should_uploadArticleImage_when_validJpegFile() throws IOException {
        // Arrange
        Long articleId = 1L;

        // Act
        ImageUploadResponse result = imageService.uploadArticleImage(articleId, validJpegFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).contains("/uploads/projects/article_1_");
        assertThat(result.getImageUrl()).endsWith(".webp");
        assertThat(result.getThumbnailUrl()).contains("/uploads/projects/article_1_");
        assertThat(result.getThumbnailUrl()).contains("_thumb.webp");
        assertThat(result.getFileSize()).isGreaterThan(0);
        assertThat(result.getUploadedAt()).isNotNull();

        // Verify files were created
        assertThat(Files.list(tempDir).count()).isEqualTo(2);
    }

    @Test
    void should_uploadArticleImage_when_validPngFile() throws IOException {
        // Arrange
        Long articleId = 2L;

        // Act
        ImageUploadResponse result = imageService.uploadArticleImage(articleId, validPngFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).endsWith(".webp");
        assertThat(result.getThumbnailUrl()).endsWith("_thumb.webp");
        assertThat(Files.list(tempDir).count()).isEqualTo(2);
    }

    @Test
    void should_throwException_when_uploadArticleImageWithEmptyFile() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> imageService.uploadArticleImage(1L, emptyFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void should_throwException_when_uploadArticleImageWithInvalidFile() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> imageService.uploadArticleImage(1L, invalidFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("File type not allowed");
    }

    // ========== Delete Article Image Tests ==========

    @Test
    void should_deleteArticleImage_when_mainImageUrlProvided() throws IOException {
        // Arrange
        Long articleId = 1L;
        ImageUploadResponse uploadResponse = imageService.uploadArticleImage(articleId, validJpegFile);
        assertThat(Files.list(tempDir).count()).isEqualTo(2);

        // Act
        imageService.deleteArticleImage(uploadResponse.getImageUrl());

        // Assert
        assertThat(Files.list(tempDir).count()).isEqualTo(0);
    }

    @Test
    void should_deleteArticleImage_when_thumbnailUrlProvided() throws IOException {
        // Arrange
        Long articleId = 1L;
        ImageUploadResponse uploadResponse = imageService.uploadArticleImage(articleId, validJpegFile);
        assertThat(Files.list(tempDir).count()).isEqualTo(2);

        // Act
        imageService.deleteArticleImage(uploadResponse.getThumbnailUrl());

        // Assert
        assertThat(Files.list(tempDir).count()).isEqualTo(0);
    }

    @Test
    void should_notFail_when_deleteArticleImageWithNullUrl() {
        // Arrange / Act / Assert - Should not throw exception
        imageService.deleteArticleImage(null);
    }

    @Test
    void should_notFail_when_deleteArticleImageWithBlankUrl() {
        // Arrange / Act / Assert - Should not throw exception
        imageService.deleteArticleImage("   ");
    }

    @Test
    void should_notFail_when_deleteArticleImageWithNonexistentFile() {
        // Arrange / Act / Assert - Should not throw exception
        imageService.deleteArticleImage("/uploads/projects/nonexistent_article_999.webp");
    }

    // ========== Additional Validation Tests ==========

    @Test
    void should_throwException_when_fileTooSmall() {
        // Arrange
        MultipartFile tinyFile = new MockMultipartFile(
                "file",
                "tiny.jpg",
                "image/jpeg",
                new byte[]{0x00, 0x01}
        );

        // Act / Assert
        assertThatThrownBy(() -> imageService.uploadProjectImage(1L, tinyFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("too small");
    }

    @Test
    void should_throwException_when_invalidMagicBytes() {
        // Arrange
        byte[] invalidMagicBytes = new byte[20];
        invalidMagicBytes[0] = 0x00;
        invalidMagicBytes[1] = 0x00;
        invalidMagicBytes[2] = 0x00;
        invalidMagicBytes[3] = 0x00;

        MultipartFile invalidMagicFile = new MockMultipartFile(
                "file",
                "fake.jpg",
                "image/jpeg",
                invalidMagicBytes
        );

        // Act / Assert
        assertThatThrownBy(() -> imageService.uploadProjectImage(1L, invalidMagicFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("not a valid image");
    }

    @Test
    void should_throwException_when_mimeTypeIsNull() {
        // Arrange
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0x00};
        MultipartFile fileWithNullMime = new MockMultipartFile(
                "file",
                "test.jpg",
                null,
                jpegBytes
        );

        // Act / Assert
        assertThatThrownBy(() -> imageService.uploadProjectImage(1L, fileWithNullMime))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("File type not allowed");
    }

    @Test
    void should_resizeImage_when_uploadingLargeImage() throws IOException {
        // Arrange - create a larger image
        BufferedImage largeImage = new BufferedImage(2000, 1500, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(largeImage, "jpg", baos);
        byte[] largeImageBytes = baos.toByteArray();

        MultipartFile largeFile = new MockMultipartFile(
                "file",
                "large_image.jpg",
                "image/jpeg",
                largeImageBytes
        );

        // Act
        ImageUploadResponse result = imageService.uploadProjectImage(1L, largeFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).endsWith(".webp");

        // Verify the image was resized
        String fileName = result.getImageUrl().substring(result.getImageUrl().lastIndexOf("/") + 1);
        Path imagePath = tempDir.resolve(fileName);
        BufferedImage savedImage = ImageIO.read(imagePath.toFile());
        assertThat(savedImage.getWidth()).isLessThanOrEqualTo(1200);
    }

    // ========== Project Carousel Image Tests ==========

    @Test
    void should_uploadProjectCarouselImage_when_validJpegFile() throws IOException {
        // Arrange
        Long projectId = 1L;
        int imageIndex = 0;

        // Act
        ImageUploadResponse result = imageService.uploadProjectCarouselImage(projectId, imageIndex, validJpegFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).contains("/uploads/projects/project_1_img0_");
        assertThat(result.getImageUrl()).endsWith(".webp");
        assertThat(result.getThumbnailUrl()).contains("_thumb.webp");
        assertThat(result.getFileSize()).isGreaterThan(0);
        assertThat(result.getUploadedAt()).isNotNull();

        // Verify files were created
        assertThat(Files.list(tempDir).count()).isEqualTo(2);
    }

    @Test
    void should_uploadProjectCarouselImage_when_validPngFile() throws IOException {
        // Arrange
        Long projectId = 2L;
        int imageIndex = 3;

        // Act
        ImageUploadResponse result = imageService.uploadProjectCarouselImage(projectId, imageIndex, validPngFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).contains("project_2_img3_");
        assertThat(result.getImageUrl()).endsWith(".webp");
        assertThat(result.getThumbnailUrl()).endsWith("_thumb.webp");
    }

    @Test
    void should_throwException_when_uploadCarouselImageWithEmptyFile() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> imageService.uploadProjectCarouselImage(1L, 0, emptyFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void should_throwException_when_uploadCarouselImageWithInvalidFile() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> imageService.uploadProjectCarouselImage(1L, 0, invalidFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("File type not allowed");
    }

    @Test
    void should_generate16x9Image_when_uploadingCarouselImage() throws IOException {
        // Arrange - create a wide image (wider than 16:9)
        BufferedImage wideImage = new BufferedImage(1920, 800, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(wideImage, "jpg", baos);
        byte[] wideImageBytes = baos.toByteArray();

        MultipartFile wideFile = new MockMultipartFile(
                "file",
                "wide_image.jpg",
                "image/jpeg",
                wideImageBytes
        );

        // Act
        ImageUploadResponse result = imageService.uploadProjectCarouselImage(1L, 0, wideFile);

        // Assert
        assertThat(result).isNotNull();
        String fileName = result.getImageUrl().substring(result.getImageUrl().lastIndexOf("/") + 1);
        Path imagePath = tempDir.resolve(fileName);
        BufferedImage savedImage = ImageIO.read(imagePath.toFile());

        // Verify 16:9 aspect ratio (with some tolerance for rounding)
        double ratio = (double) savedImage.getWidth() / savedImage.getHeight();
        assertThat(ratio).isBetween(1.7, 1.8); // 16/9 = 1.777...
    }

    @Test
    void should_generate16x9Image_when_uploadingTallImage() throws IOException {
        // Arrange - create a tall image (taller than 16:9)
        BufferedImage tallImage = new BufferedImage(800, 1200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(tallImage, "jpg", baos);
        byte[] tallImageBytes = baos.toByteArray();

        MultipartFile tallFile = new MockMultipartFile(
                "file",
                "tall_image.jpg",
                "image/jpeg",
                tallImageBytes
        );

        // Act
        ImageUploadResponse result = imageService.uploadProjectCarouselImage(1L, 0, tallFile);

        // Assert
        assertThat(result).isNotNull();
        String fileName = result.getImageUrl().substring(result.getImageUrl().lastIndexOf("/") + 1);
        Path imagePath = tempDir.resolve(fileName);
        BufferedImage savedImage = ImageIO.read(imagePath.toFile());

        // Verify 16:9 aspect ratio
        double ratio = (double) savedImage.getWidth() / savedImage.getHeight();
        assertThat(ratio).isBetween(1.7, 1.8);
    }

    // ========== Profile Image Tests ==========

    @Test
    void should_uploadProfileImage_when_validJpegFile() throws IOException {
        // Arrange / Act
        ImageUploadResponse result = imageService.uploadProfileImage(validJpegFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).contains("/uploads/projects/profile_");
        assertThat(result.getImageUrl()).endsWith(".webp");
        assertThat(result.getThumbnailUrl()).isNull(); // No thumbnail for profile images
        assertThat(result.getFileSize()).isGreaterThan(0);
        assertThat(result.getUploadedAt()).isNotNull();

        // Verify file was created
        assertThat(Files.list(tempDir).count()).isEqualTo(1);
    }

    @Test
    void should_uploadProfileImage_when_validPngFile() throws IOException {
        // Arrange / Act
        ImageUploadResponse result = imageService.uploadProfileImage(validPngFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).contains("profile_");
        assertThat(result.getImageUrl()).endsWith(".webp");
        assertThat(Files.list(tempDir).count()).isEqualTo(1);
    }

    @Test
    void should_throwException_when_uploadProfileImageWithEmptyFile() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> imageService.uploadProfileImage(emptyFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void should_throwException_when_uploadProfileImageWithInvalidFile() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> imageService.uploadProfileImage(invalidFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("File type not allowed");
    }

    @Test
    void should_deleteOldProfileImage_when_uploadingNewProfileImage() throws IOException {
        // Arrange - upload first profile image
        imageService.uploadProfileImage(validJpegFile);
        assertThat(Files.list(tempDir).count()).isEqualTo(1);

        // Act - upload second profile image
        imageService.uploadProfileImage(validPngFile);

        // Assert - old image should be deleted
        assertThat(Files.list(tempDir).count()).isEqualTo(1);
    }

    @Test
    void should_generateSquareProfileImage_when_uploadingRectangularImage() throws IOException {
        // Arrange - create a rectangular image
        BufferedImage rectangularImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(rectangularImage, "jpg", baos);

        MultipartFile rectangularFile = new MockMultipartFile(
                "file",
                "rectangular.jpg",
                "image/jpeg",
                baos.toByteArray()
        );

        // Act
        ImageUploadResponse result = imageService.uploadProfileImage(rectangularFile);

        // Assert
        String fileName = result.getImageUrl().substring(result.getImageUrl().lastIndexOf("/") + 1);
        Path imagePath = tempDir.resolve(fileName);
        BufferedImage savedImage = ImageIO.read(imagePath.toFile());

        // Verify square aspect ratio
        assertThat(savedImage.getWidth()).isEqualTo(savedImage.getHeight());
        // Verify max size
        assertThat(savedImage.getWidth()).isLessThanOrEqualTo(500);
    }

    @Test
    void should_deleteProfileImage_when_fileNameProvided() throws IOException {
        // Arrange
        ImageUploadResponse uploadResponse = imageService.uploadProfileImage(validJpegFile);
        assertThat(Files.list(tempDir).count()).isEqualTo(1);

        String fileName = uploadResponse.getImageUrl().substring(uploadResponse.getImageUrl().lastIndexOf("/") + 1);

        // Act
        imageService.deleteProfileImage(fileName);

        // Assert
        assertThat(Files.list(tempDir).count()).isEqualTo(0);
    }

    @Test
    void should_notFail_when_deleteProfileImageWithNullFileName() {
        // Arrange / Act / Assert - Should not throw exception
        imageService.deleteProfileImage(null);
    }

    @Test
    void should_notFail_when_deleteProfileImageWithBlankFileName() {
        // Arrange / Act / Assert - Should not throw exception
        imageService.deleteProfileImage("   ");
    }

    @Test
    void should_notFail_when_deleteProfileImageWithNonexistentFile() {
        // Arrange / Act / Assert - Should not throw exception
        imageService.deleteProfileImage("nonexistent_profile.webp");
    }

    // ========== Delete Project Image By URL Tests ==========

    @Test
    void should_deleteProjectImageByUrl_when_mainImageUrlProvided() throws IOException {
        // Arrange
        Long projectId = 1L;
        ImageUploadResponse uploadResponse = imageService.uploadProjectCarouselImage(projectId, 0, validJpegFile);
        assertThat(Files.list(tempDir).count()).isEqualTo(2);

        // Act
        imageService.deleteProjectImageByUrl(uploadResponse.getImageUrl());

        // Assert - Both main image and thumbnail should be deleted
        assertThat(Files.list(tempDir).count()).isEqualTo(0);
    }

    @Test
    void should_deleteProjectImageByUrl_when_thumbnailUrlProvided() throws IOException {
        // Arrange
        Long projectId = 1L;
        ImageUploadResponse uploadResponse = imageService.uploadProjectCarouselImage(projectId, 0, validJpegFile);
        assertThat(Files.list(tempDir).count()).isEqualTo(2);

        // Act
        imageService.deleteProjectImageByUrl(uploadResponse.getThumbnailUrl());

        // Assert - Both main image and thumbnail should be deleted
        assertThat(Files.list(tempDir).count()).isEqualTo(0);
    }

    @Test
    void should_notFail_when_deleteProjectImageByUrlWithNullUrl() {
        // Arrange / Act / Assert - Should not throw exception
        imageService.deleteProjectImageByUrl(null);
    }

    @Test
    void should_notFail_when_deleteProjectImageByUrlWithBlankUrl() {
        // Arrange / Act / Assert - Should not throw exception
        imageService.deleteProjectImageByUrl("   ");
    }

    @Test
    void should_notFail_when_deleteProjectImageByUrlWithNonexistentFile() {
        // Arrange / Act / Assert - Should not throw exception
        imageService.deleteProjectImageByUrl("/uploads/projects/nonexistent_project_999.webp");
    }
}
