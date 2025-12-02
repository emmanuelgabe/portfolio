package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.config.ImageStorageProperties;
import com.emmanuelgabe.portfolio.dto.ImageUploadResponse;
import com.emmanuelgabe.portfolio.dto.SiteConfigurationResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSiteConfigurationRequest;
import com.emmanuelgabe.portfolio.entity.SiteConfiguration;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.SiteConfigurationMapper;
import com.emmanuelgabe.portfolio.repository.SiteConfigurationRepository;
import com.emmanuelgabe.portfolio.service.impl.SiteConfigurationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SiteConfigurationServiceImpl.
 * Tests get, update, and profile image operations for site configuration.
 */
@ExtendWith(MockitoExtension.class)
class SiteConfigurationServiceTest {

    private static final Long SITE_CONFIGURATION_ID = 1L;

    @Mock
    private SiteConfigurationRepository siteConfigurationRepository;

    @Mock
    private SiteConfigurationMapper siteConfigurationMapper;

    @Mock
    private ImageService imageService;

    @Mock
    private ImageStorageProperties imageStorageProperties;

    @InjectMocks
    private SiteConfigurationServiceImpl siteConfigurationService;

    private SiteConfiguration testConfig;
    private SiteConfigurationResponse testConfigResponse;

    @BeforeEach
    void setUp() {
        testConfig = new SiteConfiguration();
        testConfig.setId(SITE_CONFIGURATION_ID);
        testConfig.setFullName("Emmanuel Gabe");
        testConfig.setEmail("contact@emmanuelgabe.com");
        testConfig.setHeroTitle("Developpeur Backend");
        testConfig.setHeroDescription("Je cree des applications web modernes.");
        testConfig.setSiteTitle("Portfolio - Emmanuel Gabe");
        testConfig.setSeoDescription("Portfolio de Emmanuel Gabe.");
        testConfig.setProfileImagePath(null);
        testConfig.setGithubUrl("https://github.com/emmanuelgabe");
        testConfig.setLinkedinUrl("https://linkedin.com/in/egabe");
        testConfig.setCreatedAt(LocalDateTime.now());
        testConfig.setUpdatedAt(LocalDateTime.now());

        testConfigResponse = new SiteConfigurationResponse();
        testConfigResponse.setId(SITE_CONFIGURATION_ID);
        testConfigResponse.setFullName("Emmanuel Gabe");
        testConfigResponse.setEmail("contact@emmanuelgabe.com");
        testConfigResponse.setHeroTitle("Developpeur Backend");
        testConfigResponse.setHeroDescription("Je cree des applications web modernes.");
        testConfigResponse.setSiteTitle("Portfolio - Emmanuel Gabe");
        testConfigResponse.setSeoDescription("Portfolio de Emmanuel Gabe.");
        testConfigResponse.setProfileImageUrl(null);
        testConfigResponse.setGithubUrl("https://github.com/emmanuelgabe");
        testConfigResponse.setLinkedinUrl("https://linkedin.com/in/egabe");
        testConfigResponse.setCreatedAt(testConfig.getCreatedAt());
        testConfigResponse.setUpdatedAt(testConfig.getUpdatedAt());

        lenient().when(siteConfigurationMapper.toResponse(any(SiteConfiguration.class)))
                .thenReturn(testConfigResponse);
        lenient().when(imageStorageProperties.getBasePath()).thenReturn("/uploads/projects");
    }

    // ========== Get Tests ==========

    @Test
    void should_returnConfiguration_when_getSiteConfigurationCalled() {
        // Arrange
        when(siteConfigurationRepository.findById(SITE_CONFIGURATION_ID))
                .thenReturn(Optional.of(testConfig));

        // Act
        SiteConfigurationResponse result = siteConfigurationService.getSiteConfiguration();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFullName()).isEqualTo("Emmanuel Gabe");
        assertThat(result.getHeroTitle()).isEqualTo("Developpeur Backend");
        verify(siteConfigurationRepository, times(1)).findById(SITE_CONFIGURATION_ID);
    }

    @Test
    void should_throwException_when_getSiteConfigurationCalledAndNotFound() {
        // Arrange
        when(siteConfigurationRepository.findById(SITE_CONFIGURATION_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> siteConfigurationService.getSiteConfiguration())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SiteConfiguration not found");
        verify(siteConfigurationRepository, times(1)).findById(SITE_CONFIGURATION_ID);
    }

    @Test
    void should_returnConfigWithImageUrl_when_profileImagePathExists() {
        // Arrange
        testConfig.setProfileImagePath("profile_20241201_120000.webp");
        when(siteConfigurationRepository.findById(SITE_CONFIGURATION_ID))
                .thenReturn(Optional.of(testConfig));

        // Act
        SiteConfigurationResponse result = siteConfigurationService.getSiteConfiguration();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProfileImageUrl()).isEqualTo("/uploads/projects/profile_20241201_120000.webp");
    }

    // ========== Update Tests ==========

    @Test
    void should_updateConfiguration_when_updateSiteConfigurationCalledWithValidRequest() {
        // Arrange
        UpdateSiteConfigurationRequest request = new UpdateSiteConfigurationRequest();
        request.setFullName("New Name");
        request.setEmail("new@email.com");
        request.setHeroTitle("New Title");
        request.setHeroDescription("New Description");
        request.setSiteTitle("New Site Title");
        request.setSeoDescription("New SEO Description");
        request.setGithubUrl("https://github.com/newuser");
        request.setLinkedinUrl("https://linkedin.com/in/newuser");

        when(siteConfigurationRepository.findById(SITE_CONFIGURATION_ID))
                .thenReturn(Optional.of(testConfig));
        when(siteConfigurationRepository.save(any(SiteConfiguration.class)))
                .thenReturn(testConfig);

        // Act
        SiteConfigurationResponse result = siteConfigurationService.updateSiteConfiguration(request);

        // Assert
        assertThat(result).isNotNull();
        verify(siteConfigurationRepository, times(1)).findById(SITE_CONFIGURATION_ID);
        verify(siteConfigurationMapper, times(1)).updateEntityFromRequest(request, testConfig);
        verify(siteConfigurationRepository, times(1)).save(testConfig);
    }

    @Test
    void should_throwException_when_updateSiteConfigurationCalledAndNotFound() {
        // Arrange
        UpdateSiteConfigurationRequest request = new UpdateSiteConfigurationRequest();
        request.setFullName("New Name");
        request.setEmail("new@email.com");
        request.setHeroTitle("New Title");
        request.setHeroDescription("New Description");
        request.setSiteTitle("New Site Title");
        request.setSeoDescription("New SEO Description");
        request.setGithubUrl("https://github.com/newuser");
        request.setLinkedinUrl("https://linkedin.com/in/newuser");

        when(siteConfigurationRepository.findById(SITE_CONFIGURATION_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> siteConfigurationService.updateSiteConfiguration(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SiteConfiguration not found");
        verify(siteConfigurationRepository, times(1)).findById(SITE_CONFIGURATION_ID);
        verify(siteConfigurationRepository, never()).save(any(SiteConfiguration.class));
    }

    // ========== Profile Image Upload Tests ==========

    @Test
    void should_uploadImage_when_uploadProfileImageCalledWithValidFile() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test image content".getBytes());
        ImageUploadResponse uploadResponse = new ImageUploadResponse(
                "/uploads/projects/profile_20241201_120000.webp", null, 1024L, LocalDateTime.now());

        when(siteConfigurationRepository.findById(SITE_CONFIGURATION_ID))
                .thenReturn(Optional.of(testConfig));
        when(imageService.uploadProfileImage(file)).thenReturn(uploadResponse);
        when(siteConfigurationRepository.save(any(SiteConfiguration.class)))
                .thenReturn(testConfig);

        // Act
        SiteConfigurationResponse result = siteConfigurationService.uploadProfileImage(file);

        // Assert
        assertThat(result).isNotNull();
        verify(imageService, times(1)).uploadProfileImage(file);
        verify(siteConfigurationRepository, times(1)).save(testConfig);
    }

    @Test
    void should_deleteOldImage_when_uploadProfileImageCalledAndImageExists() {
        // Arrange
        testConfig.setProfileImagePath("old_profile.webp");
        MultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test image content".getBytes());
        ImageUploadResponse uploadResponse = new ImageUploadResponse(
                "/uploads/projects/profile_20241201_120000.webp", null, 1024L, LocalDateTime.now());

        when(siteConfigurationRepository.findById(SITE_CONFIGURATION_ID))
                .thenReturn(Optional.of(testConfig));
        when(imageService.uploadProfileImage(file)).thenReturn(uploadResponse);
        when(siteConfigurationRepository.save(any(SiteConfiguration.class)))
                .thenReturn(testConfig);

        // Act
        siteConfigurationService.uploadProfileImage(file);

        // Assert
        verify(imageService, times(1)).deleteProfileImage("old_profile.webp");
        verify(imageService, times(1)).uploadProfileImage(file);
    }

    // ========== Profile Image Delete Tests ==========

    @Test
    void should_deleteImage_when_deleteProfileImageCalled() {
        // Arrange
        testConfig.setProfileImagePath("profile_20241201_120000.webp");
        when(siteConfigurationRepository.findById(SITE_CONFIGURATION_ID))
                .thenReturn(Optional.of(testConfig));
        when(siteConfigurationRepository.save(any(SiteConfiguration.class)))
                .thenReturn(testConfig);

        // Act
        SiteConfigurationResponse result = siteConfigurationService.deleteProfileImage();

        // Assert
        assertThat(result).isNotNull();
        verify(imageService, times(1)).deleteProfileImage("profile_20241201_120000.webp");
        verify(siteConfigurationRepository, times(1)).save(testConfig);
    }

    @Test
    void should_doNothing_when_deleteProfileImageCalledAndNoImage() {
        // Arrange
        testConfig.setProfileImagePath(null);
        when(siteConfigurationRepository.findById(SITE_CONFIGURATION_ID))
                .thenReturn(Optional.of(testConfig));

        // Act
        SiteConfigurationResponse result = siteConfigurationService.deleteProfileImage();

        // Assert
        assertThat(result).isNotNull();
        verify(imageService, never()).deleteProfileImage(any());
        verify(siteConfigurationRepository, never()).save(any());
    }
}
