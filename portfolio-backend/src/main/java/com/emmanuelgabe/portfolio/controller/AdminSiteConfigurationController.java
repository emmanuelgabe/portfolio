package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.SiteConfigurationResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSiteConfigurationRequest;
import com.emmanuelgabe.portfolio.service.SiteConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for admin site configuration management.
 * Provides read, update, and profile image upload operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/configuration")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSiteConfigurationController {

    private final SiteConfigurationService siteConfigurationService;

    /**
     * Get the site configuration.
     *
     * @return Site configuration details
     */
    @GetMapping
    public ResponseEntity<SiteConfigurationResponse> getSiteConfiguration() {
        log.debug("[ADMIN_SITE_CONFIG] Fetching site configuration");
        SiteConfigurationResponse config = siteConfigurationService.getSiteConfiguration();
        log.debug("[ADMIN_SITE_CONFIG] Found site configuration - fullName={}", config.getFullName());
        return ResponseEntity.ok(config);
    }

    /**
     * Update the site configuration.
     *
     * @param request Update site configuration request
     * @return Updated site configuration
     */
    @PutMapping
    public ResponseEntity<SiteConfigurationResponse> updateSiteConfiguration(
            @Valid @RequestBody UpdateSiteConfigurationRequest request) {
        log.info("[ADMIN_SITE_CONFIG] Updating site configuration - fullName={}", request.getFullName());
        SiteConfigurationResponse updatedConfig = siteConfigurationService.updateSiteConfiguration(request);
        log.info("[ADMIN_SITE_CONFIG] Updated site configuration - fullName={}", updatedConfig.getFullName());
        return ResponseEntity.ok(updatedConfig);
    }

    /**
     * Upload profile image.
     *
     * @param file Image file to upload
     * @return Updated site configuration with profile image URL
     */
    @PostMapping("/profile-image")
    public ResponseEntity<SiteConfigurationResponse> uploadProfileImage(
            @RequestParam("file") MultipartFile file) {
        log.info("[ADMIN_SITE_CONFIG] Uploading profile image - fileName={}, size={}",
                file.getOriginalFilename(), file.getSize());
        SiteConfigurationResponse config = siteConfigurationService.uploadProfileImage(file);
        log.info("[ADMIN_SITE_CONFIG] Profile image uploaded - url={}", config.getProfileImageUrl());
        return ResponseEntity.ok(config);
    }

    /**
     * Delete profile image.
     *
     * @return Site configuration without profile image
     */
    @DeleteMapping("/profile-image")
    public ResponseEntity<SiteConfigurationResponse> deleteProfileImage() {
        log.info("[ADMIN_SITE_CONFIG] Deleting profile image");
        SiteConfigurationResponse config = siteConfigurationService.deleteProfileImage();
        log.info("[ADMIN_SITE_CONFIG] Profile image deleted");
        return ResponseEntity.ok(config);
    }
}
