package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.SiteConfigurationResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSiteConfigurationRequest;
import com.emmanuelgabe.portfolio.entity.SiteConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SiteConfigurationMapper.
 */
class SiteConfigurationMapperTest {

    private SiteConfigurationMapper mapper;
    private SiteConfiguration testConfig;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(SiteConfigurationMapper.class);

        testConfig = new SiteConfiguration();
        testConfig.setId(1L);
        testConfig.setFullName("Emmanuel Gabe");
        testConfig.setEmail("contact@emmanuelgabe.com");
        testConfig.setHeroTitle("Developpeur Backend");
        testConfig.setHeroDescription("Je cree des applications web modernes.");
        testConfig.setSiteTitle("Portfolio - Emmanuel Gabe");
        testConfig.setSeoDescription("Portfolio de Emmanuel Gabe.");
        testConfig.setProfileImagePath("profile_20241201_120000.webp");
        testConfig.setGithubUrl("https://github.com/emmanuelgabe");
        testConfig.setLinkedinUrl("https://linkedin.com/in/egabe");
        testConfig.setCreatedAt(LocalDateTime.now());
        testConfig.setUpdatedAt(LocalDateTime.now());
    }

    // ========== toResponse Tests ==========

    @Test
    void should_mapAllFields_when_toResponseCalled() {
        // Act
        SiteConfigurationResponse response = mapper.toResponse(testConfig);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testConfig.getId());
        assertThat(response.getFullName()).isEqualTo(testConfig.getFullName());
        assertThat(response.getEmail()).isEqualTo(testConfig.getEmail());
        assertThat(response.getHeroTitle()).isEqualTo(testConfig.getHeroTitle());
        assertThat(response.getHeroDescription()).isEqualTo(testConfig.getHeroDescription());
        assertThat(response.getSiteTitle()).isEqualTo(testConfig.getSiteTitle());
        assertThat(response.getSeoDescription()).isEqualTo(testConfig.getSeoDescription());
        assertThat(response.getGithubUrl()).isEqualTo(testConfig.getGithubUrl());
        assertThat(response.getLinkedinUrl()).isEqualTo(testConfig.getLinkedinUrl());
        assertThat(response.getCreatedAt()).isEqualTo(testConfig.getCreatedAt());
        assertThat(response.getUpdatedAt()).isEqualTo(testConfig.getUpdatedAt());
    }

    @Test
    void should_ignoreProfileImageUrl_when_toResponseCalled() {
        // Act
        SiteConfigurationResponse response = mapper.toResponse(testConfig);

        // Assert
        // profileImageUrl should be null because it's ignored in mapper (set by service)
        assertThat(response.getProfileImageUrl()).isNull();
    }

    @Test
    void should_returnNull_when_toResponseCalledWithNull() {
        // Act
        SiteConfigurationResponse response = mapper.toResponse(null);

        // Assert
        assertThat(response).isNull();
    }

    // ========== updateEntityFromRequest Tests ==========

    @Test
    void should_updateAllFields_when_updateEntityFromRequestCalled() {
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

        // Act
        mapper.updateEntityFromRequest(request, testConfig);

        // Assert
        assertThat(testConfig.getFullName()).isEqualTo("New Name");
        assertThat(testConfig.getEmail()).isEqualTo("new@email.com");
        assertThat(testConfig.getHeroTitle()).isEqualTo("New Title");
        assertThat(testConfig.getHeroDescription()).isEqualTo("New Description");
        assertThat(testConfig.getSiteTitle()).isEqualTo("New Site Title");
        assertThat(testConfig.getSeoDescription()).isEqualTo("New SEO Description");
        assertThat(testConfig.getGithubUrl()).isEqualTo("https://github.com/newuser");
        assertThat(testConfig.getLinkedinUrl()).isEqualTo("https://linkedin.com/in/newuser");
    }

    @Test
    void should_notChangeId_when_updateEntityFromRequestCalled() {
        // Arrange
        Long originalId = testConfig.getId();
        UpdateSiteConfigurationRequest request = new UpdateSiteConfigurationRequest();
        request.setFullName("New Name");
        request.setEmail("new@email.com");
        request.setHeroTitle("New Title");
        request.setHeroDescription("New Description");
        request.setSiteTitle("New Site Title");
        request.setSeoDescription("New SEO Description");
        request.setGithubUrl("https://github.com/newuser");
        request.setLinkedinUrl("https://linkedin.com/in/newuser");

        // Act
        mapper.updateEntityFromRequest(request, testConfig);

        // Assert
        assertThat(testConfig.getId()).isEqualTo(originalId);
    }

    @Test
    void should_notChangeProfileImagePath_when_updateEntityFromRequestCalled() {
        // Arrange
        String originalPath = testConfig.getProfileImagePath();
        UpdateSiteConfigurationRequest request = new UpdateSiteConfigurationRequest();
        request.setFullName("New Name");
        request.setEmail("new@email.com");
        request.setHeroTitle("New Title");
        request.setHeroDescription("New Description");
        request.setSiteTitle("New Site Title");
        request.setSeoDescription("New SEO Description");
        request.setGithubUrl("https://github.com/newuser");
        request.setLinkedinUrl("https://linkedin.com/in/newuser");

        // Act
        mapper.updateEntityFromRequest(request, testConfig);

        // Assert
        assertThat(testConfig.getProfileImagePath()).isEqualTo(originalPath);
    }

    @Test
    void should_notChangeTimestamps_when_updateEntityFromRequestCalled() {
        // Arrange
        LocalDateTime originalCreatedAt = testConfig.getCreatedAt();
        LocalDateTime originalUpdatedAt = testConfig.getUpdatedAt();
        UpdateSiteConfigurationRequest request = new UpdateSiteConfigurationRequest();
        request.setFullName("New Name");
        request.setEmail("new@email.com");
        request.setHeroTitle("New Title");
        request.setHeroDescription("New Description");
        request.setSiteTitle("New Site Title");
        request.setSeoDescription("New SEO Description");
        request.setGithubUrl("https://github.com/newuser");
        request.setLinkedinUrl("https://linkedin.com/in/newuser");

        // Act
        mapper.updateEntityFromRequest(request, testConfig);

        // Assert
        assertThat(testConfig.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(testConfig.getUpdatedAt()).isEqualTo(originalUpdatedAt);
    }
}
