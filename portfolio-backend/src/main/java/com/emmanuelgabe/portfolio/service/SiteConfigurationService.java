package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.SiteConfigurationResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSiteConfigurationRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for SiteConfiguration operations.
 * SiteConfiguration is a singleton entity (only one row with id=1).
 */
public interface SiteConfigurationService {

    /**
     * Get the site configuration.
     *
     * @return Site configuration response
     */
    SiteConfigurationResponse getSiteConfiguration();

    /**
     * Update the site configuration.
     *
     * @param request Update site configuration request
     * @return Updated site configuration response
     */
    SiteConfigurationResponse updateSiteConfiguration(UpdateSiteConfigurationRequest request);

    /**
     * Upload profile image.
     *
     * @param file Image file to upload
     * @return Updated site configuration response with profile image URL
     */
    SiteConfigurationResponse uploadProfileImage(MultipartFile file);

    /**
     * Delete profile image.
     *
     * @return Updated site configuration response without profile image
     */
    SiteConfigurationResponse deleteProfileImage();
}
