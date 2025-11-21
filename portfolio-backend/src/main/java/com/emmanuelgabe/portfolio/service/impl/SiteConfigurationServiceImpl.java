package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.config.ImageStorageProperties;
import com.emmanuelgabe.portfolio.dto.ImageUploadResponse;
import com.emmanuelgabe.portfolio.dto.SiteConfigurationResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSiteConfigurationRequest;
import com.emmanuelgabe.portfolio.entity.SiteConfiguration;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.SiteConfigurationMapper;
import com.emmanuelgabe.portfolio.repository.SiteConfigurationRepository;
import com.emmanuelgabe.portfolio.service.ImageService;
import com.emmanuelgabe.portfolio.service.SiteConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation of SiteConfigurationService interface.
 * SiteConfiguration is a singleton entity (only one row with id=1).
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SiteConfigurationServiceImpl implements SiteConfigurationService {

    private static final Long SITE_CONFIGURATION_ID = 1L;

    private final SiteConfigurationRepository siteConfigurationRepository;
    private final SiteConfigurationMapper siteConfigurationMapper;
    private final ImageService imageService;
    private final ImageStorageProperties imageStorageProperties;

    @Override
    @Transactional(readOnly = true)
    public SiteConfigurationResponse getSiteConfiguration() {
        log.debug("[GET_SITE_CONFIG] Fetching site configuration");

        SiteConfiguration config = findSiteConfiguration();
        SiteConfigurationResponse response = siteConfigurationMapper.toResponse(config);
        response.setProfileImageUrl(buildProfileImageUrl(config.getProfileImagePath()));

        return response;
    }

    @Override
    public SiteConfigurationResponse updateSiteConfiguration(UpdateSiteConfigurationRequest request) {
        log.debug("[UPDATE_SITE_CONFIG] Updating site configuration - fullName={}", request.getFullName());

        SiteConfiguration config = findSiteConfiguration();
        siteConfigurationMapper.updateEntityFromRequest(request, config);
        SiteConfiguration updatedConfig = siteConfigurationRepository.save(config);

        log.info("[UPDATE_SITE_CONFIG] Site configuration updated - fullName={}", updatedConfig.getFullName());

        SiteConfigurationResponse response = siteConfigurationMapper.toResponse(updatedConfig);
        response.setProfileImageUrl(buildProfileImageUrl(updatedConfig.getProfileImagePath()));

        return response;
    }

    @Override
    public SiteConfigurationResponse uploadProfileImage(MultipartFile file) {
        log.info("[PROFILE_IMAGE] Uploading profile image - fileName={}, size={}",
                file.getOriginalFilename(), file.getSize());

        SiteConfiguration config = findSiteConfiguration();

        // Delete existing profile image if present
        if (config.getProfileImagePath() != null) {
            log.debug("[PROFILE_IMAGE] Deleting existing profile image - path={}",
                    config.getProfileImagePath());
            imageService.deleteProfileImage(config.getProfileImagePath());
        }

        // Upload new profile image
        ImageUploadResponse uploadResponse = imageService.uploadProfileImage(file);

        // Extract filename from URL and store as path
        String imageUrl = uploadResponse.getImageUrl();
        String imagePath = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        config.setProfileImagePath(imagePath);

        SiteConfiguration updatedConfig = siteConfigurationRepository.save(config);

        log.info("[PROFILE_IMAGE] Profile image uploaded - path={}", imagePath);

        SiteConfigurationResponse response = siteConfigurationMapper.toResponse(updatedConfig);
        response.setProfileImageUrl(buildProfileImageUrl(updatedConfig.getProfileImagePath()));

        return response;
    }

    @Override
    public SiteConfigurationResponse deleteProfileImage() {
        log.info("[PROFILE_IMAGE] Deleting profile image");

        SiteConfiguration config = findSiteConfiguration();

        if (config.getProfileImagePath() != null) {
            imageService.deleteProfileImage(config.getProfileImagePath());
            config.setProfileImagePath(null);
            siteConfigurationRepository.save(config);
            log.info("[PROFILE_IMAGE] Profile image deleted");
        } else {
            log.debug("[PROFILE_IMAGE] No profile image to delete");
        }

        SiteConfigurationResponse response = siteConfigurationMapper.toResponse(config);
        response.setProfileImageUrl(null);

        return response;
    }

    /**
     * Find the singleton site configuration entity.
     *
     * @return SiteConfiguration entity
     * @throws ResourceNotFoundException if not found
     */
    private SiteConfiguration findSiteConfiguration() {
        return siteConfigurationRepository.findById(SITE_CONFIGURATION_ID)
                .orElseThrow(() -> {
                    log.warn("[SITE_CONFIG] Site configuration not found");
                    return new ResourceNotFoundException("SiteConfiguration", "id", SITE_CONFIGURATION_ID);
                });
    }

    /**
     * Build full profile image URL from path.
     *
     * @param imagePath Image filename/path
     * @return Full URL or null if no image
     */
    private String buildProfileImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        return imageStorageProperties.getBasePath() + "/" + imagePath;
    }
}
