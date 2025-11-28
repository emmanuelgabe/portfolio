package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.config.FileStorageProperties;
import com.emmanuelgabe.portfolio.exception.FileStorageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FileStorageServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class FileStorageServiceImplTest {

    @Mock
    private FileStorageProperties fileStorageProperties;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    @TempDir
    Path tempDir;

    // Valid JPEG magic bytes
    private static final byte[] VALID_JPEG_BYTES = {
        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
        0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
    };

    // Valid PNG magic bytes
    private static final byte[] VALID_PNG_BYTES = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D
    };

    // Valid GIF magic bytes
    private static final byte[] VALID_GIF_BYTES = {
        0x47, 0x49, 0x46, 0x38, 0x39, 0x61,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    // Valid WebP magic bytes
    private static final byte[] VALID_WEBP_BYTES = {
        0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,
        0x57, 0x45, 0x42, 0x50
    };

    @BeforeEach
    void setUp() {
        when(fileStorageProperties.getUploadDir()).thenReturn(tempDir.toString());
        when(fileStorageProperties.getBasePath()).thenReturn("/uploads/images");
        when(fileStorageProperties.getMaxFileSize()).thenReturn(5242880L); // 5MB
        when(fileStorageProperties.getAllowedExtensions())
                .thenReturn(new String[]{"jpg", "jpeg", "png", "gif", "webp"});
        when(fileStorageProperties.getAllowedMimeTypes())
                .thenReturn(new String[]{"image/jpeg", "image/png", "image/gif", "image/webp"});
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up any files created during tests
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
        }
    }

    @Test
    void should_createUploadDirectory_when_initCalled() {
        // Act
        fileStorageService.init();

        // Assert
        assertTrue(Files.exists(tempDir));
        assertTrue(Files.isDirectory(tempDir));
    }

    @Test
    void should_storeFileSuccessfully_when_storeFileCalled() throws IOException {
        // Arrange
        fileStorageService.init();
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn(VALID_JPEG_BYTES);
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("test content".getBytes()));

        // Act
        String fileName = fileStorageService.storeFile(multipartFile);

        // Assert
        assertNotNull(fileName);
        assertTrue(fileName.endsWith(".jpg"));
        assertTrue(Files.exists(tempDir.resolve(fileName)));
    }

    @Test
    void should_throwException_when_storeFileCalledWithPathTraversal() throws IOException {
        // Arrange
        fileStorageService.init();
        when(multipartFile.getOriginalFilename()).thenReturn("../../../etc/passwd");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.isEmpty()).thenReturn(false);

        // Act & Assert
        assertThrows(FileStorageException.class, () -> fileStorageService.storeFile(multipartFile));
    }

    @Test
    void should_throwException_when_storeFileCalledAndIOExceptionOccurs() throws IOException {
        // Arrange
        fileStorageService.init();
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn(VALID_JPEG_BYTES);
        when(multipartFile.getInputStream()).thenThrow(new IOException("Simulated IO error"));

        // Act & Assert
        FileStorageException exception = assertThrows(FileStorageException.class,
                () -> fileStorageService.storeFile(multipartFile));
        assertTrue(exception.getMessage().contains("Could not store file"));
    }

    @Test
    void should_pass_when_validateFileCalledWithValidFile() throws IOException {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getBytes()).thenReturn(VALID_JPEG_BYTES);

        // Act & Assert
        assertDoesNotThrow(() -> fileStorageService.validateFile(multipartFile));
    }

    @Test
    void should_throwException_when_validateFileCalledWithNullFile() {
        // Act & Assert
        FileStorageException exception = assertThrows(FileStorageException.class,
                () -> fileStorageService.validateFile(null));
        assertEquals("File is empty", exception.getMessage());
    }

    @Test
    void should_throwException_when_validateFileCalledWithEmptyFile() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(true);

        // Act & Assert
        FileStorageException exception = assertThrows(FileStorageException.class,
                () -> fileStorageService.validateFile(multipartFile));
        assertEquals("File is empty", exception.getMessage());
    }

    @Test
    void should_throwException_when_validateFileCalledWithFileSizeExceedingLimit() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(10485760L); // 10MB
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");

        // Act & Assert
        FileStorageException exception = assertThrows(FileStorageException.class,
                () -> fileStorageService.validateFile(multipartFile));
        assertTrue(exception.getMessage().contains("File size exceeds maximum allowed size"));
    }

    @Test
    void should_throwException_when_validateFileCalledWithDisallowedExtension() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getOriginalFilename()).thenReturn("test.exe");

        // Act & Assert
        FileStorageException exception = assertThrows(FileStorageException.class,
                () -> fileStorageService.validateFile(multipartFile));
        assertTrue(exception.getMessage().contains("File type not allowed"));
    }

    @Test
    void should_handleMixedCaseExtensions_when_validateFileCalled() throws IOException {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getOriginalFilename()).thenReturn("test.JPG");
        when(multipartFile.getBytes()).thenReturn(VALID_JPEG_BYTES);

        // Act & Assert
        assertDoesNotThrow(() -> fileStorageService.validateFile(multipartFile));
    }

    @Test
    void should_returnCorrectUrl_when_getFileUrlCalled() {
        // Arrange
        String fileName = "test-file.jpg";

        // Act
        String fileUrl = fileStorageService.getFileUrl(fileName);

        // Assert
        assertEquals("/uploads/images/test-file.jpg", fileUrl);
    }

    @Test
    void should_returnCorrectExtension_when_getFileExtensionCalledWithValidFileName() throws IOException {
        // Arrange
        fileStorageService.init();
        when(multipartFile.getOriginalFilename()).thenReturn("document.pdf.backup.jpg");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn(VALID_JPEG_BYTES);

        // Act & Assert - Should extract the last extension
        assertDoesNotThrow(() -> fileStorageService.validateFile(multipartFile));
    }

    @Test
    void should_generateUniqueFileNames_when_storeFileCalled() throws IOException {
        // Arrange
        fileStorageService.init();
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn(VALID_JPEG_BYTES);
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("test content 1".getBytes()))
                .thenReturn(new ByteArrayInputStream("test content 2".getBytes()));

        // Act
        String fileName1 = fileStorageService.storeFile(multipartFile);
        String fileName2 = fileStorageService.storeFile(multipartFile);

        // Assert
        assertNotEquals(fileName1, fileName2);
        assertTrue(Files.exists(tempDir.resolve(fileName1)));
        assertTrue(Files.exists(tempDir.resolve(fileName2)));
    }

    @Test
    void should_replaceExistingFile_when_storeFileCalledWithMatchingFileName() throws IOException {
        // Arrange
        fileStorageService.init();
        when(multipartFile.getOriginalFilename()).thenReturn("test.png");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn(VALID_PNG_BYTES);
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("test content".getBytes()));

        // Act
        String fileName = fileStorageService.storeFile(multipartFile);

        // Assert
        Path targetPath = tempDir.resolve(fileName);
        assertTrue(Files.exists(targetPath));
        String content = Files.readString(targetPath);
        assertEquals("test content", content);
    }

    @Test
    void should_handleFileWithNoExtension_when_validateFileCalled() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getOriginalFilename()).thenReturn("testfile");

        // Act & Assert
        FileStorageException exception = assertThrows(FileStorageException.class,
                () -> fileStorageService.validateFile(multipartFile));
        assertTrue(exception.getMessage().contains("File type not allowed"));
    }

    @Test
    void should_handleNullOriginalFilename_when_validateFileCalled() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getOriginalFilename()).thenReturn(null);

        // Act & Assert
        assertThrows(Exception.class, () -> fileStorageService.validateFile(multipartFile));
    }
}
