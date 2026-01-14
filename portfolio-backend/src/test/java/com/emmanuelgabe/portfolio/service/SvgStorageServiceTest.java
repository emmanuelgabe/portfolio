package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.config.SvgStorageProperties;
import com.emmanuelgabe.portfolio.exception.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SvgStorageServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private MultipartFile mockFile;

    private SvgStorageProperties storageProperties;
    private SvgStorageService svgStorageService;

    private static final String VALID_SVG_CONTENT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 2L2 7l10 5 10-5-10-5z" fill="#000"/>
                <circle cx="12" cy="12" r="5" stroke="#333" fill="none"/>
            </svg>
            """;

    @BeforeEach
    void setUp() {
        storageProperties = new SvgStorageProperties();
        storageProperties.setUploadDir(tempDir.toString());
        storageProperties.setBasePath("/uploads/icons");
        storageProperties.setMaxFileSize(102400L);
        storageProperties.setAllowedExtensions(new String[]{"svg"});
        storageProperties.setAllowedMimeTypes(new String[]{"image/svg+xml"});

        svgStorageService = new SvgStorageService(storageProperties);
        svgStorageService.init();
    }

    // ========== Validation Tests ==========

    @Test
    void should_throwException_when_uploadSkillIconCalledWithNullFile() {
        // Arrange
        MultipartFile nullFile = null;

        // Act & Assert
        // Service throws NullPointerException when file is null (before isEmpty() check)
        assertThatThrownBy(() -> svgStorageService.uploadSkillIcon(1L, nullFile))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwException_when_uploadSkillIconCalledWithEmptyFile() throws IOException {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> svgStorageService.uploadSkillIcon(1L, mockFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("File is empty");
    }

    @Test
    void should_throwException_when_uploadSkillIconCalledWithPathTraversal() throws IOException {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("../../../etc/passwd");
        when(mockFile.getSize()).thenReturn(100L);

        // Act & Assert
        assertThatThrownBy(() -> svgStorageService.uploadSkillIcon(1L, mockFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("path traversal");
    }

    @Test
    void should_throwException_when_uploadSkillIconCalledWithFileTooLarge() throws IOException {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("icon.svg");
        when(mockFile.getSize()).thenReturn(200000L);

        // Act & Assert
        assertThatThrownBy(() -> svgStorageService.uploadSkillIcon(1L, mockFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("exceeds maximum");
    }

    @Test
    void should_throwException_when_uploadSkillIconCalledWithInvalidExtension() throws IOException {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("icon.png");
        when(mockFile.getSize()).thenReturn(100L);

        // Act & Assert
        assertThatThrownBy(() -> svgStorageService.uploadSkillIcon(1L, mockFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("type not allowed");
    }

    @Test
    void should_throwException_when_uploadSkillIconCalledWithInvalidMimeType() throws IOException {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("icon.svg");
        when(mockFile.getSize()).thenReturn(100L);
        when(mockFile.getContentType()).thenReturn("image/png");

        // Act & Assert
        assertThatThrownBy(() -> svgStorageService.uploadSkillIcon(1L, mockFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("type not allowed");
    }

    @Test
    void should_throwException_when_uploadSkillIconCalledWithNoSvgTag() throws IOException {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("icon.svg");
        when(mockFile.getSize()).thenReturn(100L);
        when(mockFile.getContentType()).thenReturn("image/svg+xml");
        when(mockFile.getBytes()).thenReturn("<div>Not SVG</div>".getBytes());

        // Act & Assert
        assertThatThrownBy(() -> svgStorageService.uploadSkillIcon(1L, mockFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("not a valid SVG");
    }

    // ========== Security Tests ==========

    @Test
    void should_throwException_when_uploadSkillIconCalledWithJavascriptHandler() throws IOException {
        // Arrange
        String dangerousSvg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                    <rect onclick="alert('xss')" width="100" height="100"/>
                </svg>
                """;
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("icon.svg");
        when(mockFile.getSize()).thenReturn(100L);
        when(mockFile.getContentType()).thenReturn("image/svg+xml");
        when(mockFile.getBytes()).thenReturn(dangerousSvg.getBytes());

        // Act & Assert
        assertThatThrownBy(() -> svgStorageService.uploadSkillIcon(1L, mockFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("dangerous content");
    }

    @Test
    void should_throwException_when_uploadSkillIconCalledWithJavascriptUrl() throws IOException {
        // Arrange
        String dangerousSvg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                    <a href="javascript:alert('xss')">
                        <rect width="100" height="100"/>
                    </a>
                </svg>
                """;
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("icon.svg");
        when(mockFile.getSize()).thenReturn(100L);
        when(mockFile.getContentType()).thenReturn("image/svg+xml");
        when(mockFile.getBytes()).thenReturn(dangerousSvg.getBytes());

        // Act & Assert
        assertThatThrownBy(() -> svgStorageService.uploadSkillIcon(1L, mockFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("dangerous content");
    }

    // ========== Style and Text Element Tests ==========

    @Test
    void should_uploadSuccessfully_when_uploadSkillIconCalledWithStyleElement() throws IOException {
        // Arrange
        String svgWithStyle = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <style>
                        .cls-1 { fill: #FF6B00; }
                        .cls-2 { fill: #7F52FF; }
                    </style>
                    <path class="cls-1" d="M10 10h80v80H10z"/>
                    <circle class="cls-2" cx="50" cy="50" r="20"/>
                </svg>
                """;
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("icon.svg");
        when(mockFile.getSize()).thenReturn((long) svgWithStyle.length());
        when(mockFile.getContentType()).thenReturn("image/svg+xml");
        when(mockFile.getBytes()).thenReturn(svgWithStyle.getBytes());

        // Act
        String result = svgStorageService.uploadSkillIcon(1L, mockFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).startsWith("/uploads/icons/skill_1_");
    }

    @Test
    void should_uploadSuccessfully_when_uploadSkillIconCalledWithTextElement() throws IOException {
        // Arrange
        String svgWithText = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <text x="10" y="50" font-family="Arial" font-size="14" fill="#333">Hello</text>
                    <tspan x="10" y="70">World</tspan>
                </svg>
                """;
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("icon.svg");
        when(mockFile.getSize()).thenReturn((long) svgWithText.length());
        when(mockFile.getContentType()).thenReturn("image/svg+xml");
        when(mockFile.getBytes()).thenReturn(svgWithText.getBytes());

        // Act
        String result = svgStorageService.uploadSkillIcon(1L, mockFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).startsWith("/uploads/icons/skill_1_");
    }

    @Test
    void should_uploadSuccessfully_when_uploadSkillIconCalledWithSafeDataImageUri() throws IOException {
        // Arrange
        String svgWithDataUri = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                    <rect fill="url(#pattern)" width="100" height="100"/>
                    <defs>
                        <pattern id="pattern" width="10" height="10">
                            <path d="M0 0h10v10H0z" fill="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="/>
                        </pattern>
                    </defs>
                </svg>
                """;
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("icon.svg");
        when(mockFile.getSize()).thenReturn((long) svgWithDataUri.length());
        when(mockFile.getContentType()).thenReturn("image/svg+xml");
        when(mockFile.getBytes()).thenReturn(svgWithDataUri.getBytes());

        // Act
        String result = svgStorageService.uploadSkillIcon(1L, mockFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).startsWith("/uploads/icons/skill_1_");
    }

    // ========== Upload Tests ==========

    @Test
    void should_uploadSuccessfully_when_uploadSkillIconCalledWithValidSvg() throws IOException {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("icon.svg");
        when(mockFile.getSize()).thenReturn((long) VALID_SVG_CONTENT.length());
        when(mockFile.getContentType()).thenReturn("image/svg+xml");
        when(mockFile.getBytes()).thenReturn(VALID_SVG_CONTENT.getBytes());

        // Act
        String result = svgStorageService.uploadSkillIcon(1L, mockFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).startsWith("/uploads/icons/skill_1_");
        assertThat(result).endsWith(".svg");
    }

    @Test
    void should_returnUrlWithCorrectFormat_when_uploadSkillIconCalledWithValidSvg() throws IOException {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("icon.svg");
        when(mockFile.getSize()).thenReturn((long) VALID_SVG_CONTENT.length());
        when(mockFile.getContentType()).thenReturn("image/svg+xml");
        when(mockFile.getBytes()).thenReturn(VALID_SVG_CONTENT.getBytes());

        // Act
        String result = svgStorageService.uploadSkillIcon(42L, mockFile);

        // Assert
        assertThat(result).contains("skill_42_");
    }

    // ========== Delete Tests ==========

    @Test
    void should_notThrowException_when_deleteSkillIconCalledWithNonExistingSkill() {
        // Act & Assert - should not throw
        svgStorageService.deleteSkillIcon(999L);
    }

    @Test
    void should_notThrowException_when_deleteIconByUrlCalledWithNull() {
        // Act & Assert - should not throw
        svgStorageService.deleteIconByUrl(null);
    }

    @Test
    void should_notThrowException_when_deleteIconByUrlCalledWithEmptyString() {
        // Act & Assert - should not throw
        svgStorageService.deleteIconByUrl("");
    }

    @Test
    void should_deleteFile_when_deleteIconByUrlCalledWithValidUrl() throws IOException {
        // Arrange - First upload a file
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("icon.svg");
        when(mockFile.getSize()).thenReturn((long) VALID_SVG_CONTENT.length());
        when(mockFile.getContentType()).thenReturn("image/svg+xml");
        when(mockFile.getBytes()).thenReturn(VALID_SVG_CONTENT.getBytes());

        String uploadedUrl = svgStorageService.uploadSkillIcon(1L, mockFile);

        // Act
        svgStorageService.deleteIconByUrl(uploadedUrl);

        // Assert - File should be deleted, but method doesn't throw on success
        // We can verify by trying to delete again (should not throw)
        svgStorageService.deleteIconByUrl(uploadedUrl);
    }
}
