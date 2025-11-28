package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.config.CvStorageProperties;
import com.emmanuelgabe.portfolio.dto.CvResponse;
import com.emmanuelgabe.portfolio.entity.Cv;
import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.entity.UserRole;
import com.emmanuelgabe.portfolio.exception.FileStorageException;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.CvMapper;
import com.emmanuelgabe.portfolio.repository.CvRepository;
import com.emmanuelgabe.portfolio.service.impl.CvServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvServiceTest {

    @Mock
    private CvRepository cvRepository;

    @Mock
    private CvMapper cvMapper;

    @Mock
    private CvStorageProperties cvStorageProperties;

    @InjectMocks
    private CvServiceImpl cvService;

    @TempDir
    Path tempDir;

    private User testUser;
    private Cv testCv;
    private CvResponse testCvResponse;
    private MultipartFile validPdfFile;
    private MultipartFile invalidFile;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setEmail("admin@test.com");
        testUser.setRole(UserRole.ROLE_ADMIN);

        // Setup test CV entity
        testCv = new Cv();
        testCv.setId(1L);
        testCv.setUser(testUser);
        testCv.setFileName("cv_20240101_120000_abc123.pdf");
        testCv.setOriginalFileName("my_cv.pdf");
        testCv.setFileUrl("/uploads/cvs/cv_20240101_120000_abc123.pdf");
        testCv.setFileSize(1024L);
        testCv.setUploadedAt(LocalDateTime.now());
        testCv.setCurrent(true);

        // Setup test CV response
        testCvResponse = new CvResponse();
        testCvResponse.setId(1L);
        testCvResponse.setFileName("cv_20240101_120000_abc123.pdf");
        testCvResponse.setOriginalFileName("my_cv.pdf");
        testCvResponse.setFileUrl("/uploads/cvs/cv_20240101_120000_abc123.pdf");
        testCvResponse.setFileSize(1024L);
        testCvResponse.setUploadedAt(testCv.getUploadedAt());
        testCvResponse.setCurrent(true);

        // Create valid PDF file (with PDF magic bytes)
        byte[] pdfContent = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34}; // %PDF-1.4
        validPdfFile = new MockMultipartFile(
                "file",
                "test_cv.pdf",
                "application/pdf",
                pdfContent
        );

        // Create invalid file (not a PDF)
        byte[] invalidContent = new byte[]{0x00, 0x01, 0x02, 0x03};
        invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                invalidContent
        );

        // Setup storage properties
        lenient().when(cvStorageProperties.getUploadDir()).thenReturn(tempDir.toString());
        lenient().when(cvStorageProperties.getBasePath()).thenReturn("/uploads/cvs");
        lenient().when(cvStorageProperties.getMaxFileSize()).thenReturn(10485760L); // 10MB
        lenient().when(cvStorageProperties.getAllowedExtensions()).thenReturn(new String[]{"pdf"});
        lenient().when(cvStorageProperties.getAllowedMimeTypes()).thenReturn(new String[]{"application/pdf"});

        // Initialize service (calls @PostConstruct)
        cvService.init();
    }

    @Test
    void should_uploadCv_when_validPdfFile() {
        // Arrange
        when(cvRepository.findByUserIdAndCurrent(testUser.getId(), true)).thenReturn(Arrays.asList());
        when(cvRepository.save(any(Cv.class))).thenReturn(testCv);
        when(cvMapper.toResponse(any(Cv.class))).thenReturn(testCvResponse);

        // Act
        CvResponse result = cvService.uploadCv(validPdfFile, testUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.isCurrent()).isTrue();
        verify(cvRepository).save(any(Cv.class));
        verify(cvMapper).toResponse(any(Cv.class));
    }

    @Test
    void should_throwException_when_uploadingInvalidFileType() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> cvService.uploadCv(invalidFile, testUser))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("File type not allowed");

        verify(cvRepository, never()).save(any(Cv.class));
    }

    @Test
    void should_throwException_when_fileIsEmpty() {
        // Arrange
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        // Act / Assert
        assertThatThrownBy(() -> cvService.uploadCv(emptyFile, testUser))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("empty");

        verify(cvRepository, never()).save(any(Cv.class));
    }

    @Test
    void should_throwException_when_fileSizeExceedsLimit() {
        // Arrange
        when(cvStorageProperties.getMaxFileSize()).thenReturn(100L); // Very small limit
        byte[] largePdfContent = new byte[200];
        largePdfContent[0] = 0x25; // %
        largePdfContent[1] = 0x50; // P
        largePdfContent[2] = 0x44; // D
        largePdfContent[3] = 0x46; // F
        MultipartFile largeFile = new MockMultipartFile("file", "large.pdf", "application/pdf", largePdfContent);

        // Act / Assert
        assertThatThrownBy(() -> cvService.uploadCv(largeFile, testUser))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("exceeds maximum");

        verify(cvRepository, never()).save(any(Cv.class));
    }

    @Test
    void should_setOldCvToNotCurrent_when_uploadingNewCv() {
        // Arrange
        Cv oldCv = new Cv();
        oldCv.setId(2L);
        oldCv.setUser(testUser);
        oldCv.setCurrent(true);

        when(cvRepository.findByUserIdAndCurrent(testUser.getId(), true)).thenReturn(Arrays.asList(oldCv));
        when(cvRepository.save(any(Cv.class))).thenReturn(testCv);
        when(cvRepository.saveAll(any())).thenReturn(Arrays.asList(oldCv));
        when(cvMapper.toResponse(any(Cv.class))).thenReturn(testCvResponse);

        // Act
        cvService.uploadCv(validPdfFile, testUser);

        // Assert
        assertThat(oldCv.isCurrent()).isFalse();
        verify(cvRepository).saveAll(any());
    }

    @Test
    void should_returnCurrentCv_when_cvExists() {
        // Arrange
        when(cvRepository.findByUserIdAndCurrentTrue(testUser.getId())).thenReturn(Optional.of(testCv));
        when(cvMapper.toResponse(testCv)).thenReturn(testCvResponse);

        // Act
        Optional<CvResponse> result = cvService.getCurrentCv(testUser.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().isCurrent()).isTrue();
        verify(cvRepository).findByUserIdAndCurrentTrue(testUser.getId());
    }

    @Test
    void should_returnEmpty_when_noCurrentCvExists() {
        // Arrange
        when(cvRepository.findByUserIdAndCurrentTrue(testUser.getId())).thenReturn(Optional.empty());

        // Act
        Optional<CvResponse> result = cvService.getCurrentCv(testUser.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnCurrentCv_when_publicEndpointCalled() {
        // Arrange
        when(cvRepository.findFirstByCurrentTrue()).thenReturn(Optional.of(testCv));
        when(cvMapper.toResponse(testCv)).thenReturn(testCvResponse);

        // Act
        Optional<CvResponse> result = cvService.getCurrentCv();

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        verify(cvRepository).findFirstByCurrentTrue();
    }

    @Test
    void should_returnEmpty_when_publicEndpointCalledWithNoCv() {
        // Arrange
        when(cvRepository.findFirstByCurrentTrue()).thenReturn(Optional.empty());

        // Act
        Optional<CvResponse> result = cvService.getCurrentCv();

        // Assert
        assertThat(result).isEmpty();
        verify(cvRepository).findFirstByCurrentTrue();
    }

    @Test
    void should_setCurrentCv_when_validCvId() {
        // Arrange
        Cv oldCv = new Cv();
        oldCv.setId(2L);
        oldCv.setUser(testUser);
        oldCv.setCurrent(true);

        when(cvRepository.findById(testCv.getId())).thenReturn(Optional.of(testCv));
        when(cvRepository.findByUserIdAndCurrent(testUser.getId(), true)).thenReturn(Arrays.asList(oldCv));
        when(cvRepository.save(testCv)).thenReturn(testCv);
        when(cvRepository.saveAll(any())).thenReturn(Arrays.asList(oldCv));
        when(cvMapper.toResponse(testCv)).thenReturn(testCvResponse);

        testCv.setCurrent(false); // Start as not current

        // Act
        CvResponse result = cvService.setCurrentCv(testCv.getId(), testUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(testCv.isCurrent()).isTrue();
        assertThat(oldCv.isCurrent()).isFalse();
        verify(cvRepository).save(testCv);
    }

    @Test
    void should_throwException_when_settingNonExistentCvAsCurrent() {
        // Arrange
        when(cvRepository.findById(999L)).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> cvService.setCurrentCv(999L, testUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void should_throwException_when_settingOtherUsersCvAsCurrent() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        testCv.setUser(otherUser);

        when(cvRepository.findById(testCv.getId())).thenReturn(Optional.of(testCv));

        // Act / Assert
        assertThatThrownBy(() -> cvService.setCurrentCv(testCv.getId(), testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to user");
    }

    @Test
    void should_getAllCvs_when_userHasMultipleCvs() {
        // Arrange
        Cv cv1 = new Cv();
        cv1.setId(1L);
        cv1.setCurrent(true);

        Cv cv2 = new Cv();
        cv2.setId(2L);
        cv2.setCurrent(false);

        List<Cv> cvs = Arrays.asList(cv1, cv2);

        when(cvRepository.findByUserIdOrderByUploadedAtDesc(testUser.getId())).thenReturn(cvs);
        when(cvMapper.toResponse(any(Cv.class))).thenReturn(testCvResponse);

        // Act
        List<CvResponse> result = cvService.getAllCvs(testUser.getId());

        // Assert
        assertThat(result).hasSize(2);
        verify(cvRepository).findByUserIdOrderByUploadedAtDesc(testUser.getId());
    }

    @Test
    void should_deleteCv_when_notCurrentCv() {
        // Arrange
        testCv.setCurrent(false);

        when(cvRepository.findById(testCv.getId())).thenReturn(Optional.of(testCv));
        doNothing().when(cvRepository).delete(testCv);

        // Act
        cvService.deleteCv(testCv.getId(), testUser);

        // Assert
        verify(cvRepository).delete(testCv);
    }

    @Test
    void should_throwException_when_deletingCurrentCvWithOthersExist() {
        // Arrange
        testCv.setCurrent(true);
        List<Cv> allCvs = Arrays.asList(testCv, new Cv());

        when(cvRepository.findById(testCv.getId())).thenReturn(Optional.of(testCv));
        when(cvRepository.findByUserIdOrderByUploadedAtDesc(testUser.getId())).thenReturn(allCvs);

        // Act / Assert
        assertThatThrownBy(() -> cvService.deleteCv(testCv.getId(), testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete current CV");
    }

    @Test
    void should_throwException_when_publicDownloadCalledWithNoCv() {
        // Arrange
        when(cvRepository.findFirstByCurrentTrue()).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> cvService.downloadCurrentCv())
                .isInstanceOf(ResourceNotFoundException.class);
        verify(cvRepository).findFirstByCurrentTrue();
    }

    @Test
    void should_returnResource_when_publicDownloadCalledWithExistingCv() throws Exception {
        // Arrange
        Path testFile = tempDir.resolve(testCv.getFileName());
        Files.write(testFile, new byte[]{0x25, 0x50, 0x44, 0x46}); // %PDF

        when(cvRepository.findFirstByCurrentTrue()).thenReturn(Optional.of(testCv));

        // Act
        org.springframework.core.io.Resource result = cvService.downloadCurrentCv();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
        assertThat(result.isReadable()).isTrue();
        verify(cvRepository).findFirstByCurrentTrue();
    }
}
