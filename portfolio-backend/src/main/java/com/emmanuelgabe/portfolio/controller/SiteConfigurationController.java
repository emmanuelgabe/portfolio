package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.SiteConfigurationResponse;
import com.emmanuelgabe.portfolio.service.SiteConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for public site configuration endpoint.
 * Admin endpoint is in AdminSiteConfigurationController under /api/admin/configuration.
 */
@Slf4j
@RestController
@RequestMapping("/api/configuration")
@RequiredArgsConstructor
public class SiteConfigurationController {

    private final SiteConfigurationService siteConfigurationService;

    /**
     * Get the site configuration.
     *
     * @return Site configuration details
     */
    @GetMapping
    public ResponseEntity<SiteConfigurationResponse> getSiteConfiguration() {
        log.debug("[SITE_CONFIG] Fetching site configuration");
        SiteConfigurationResponse config = siteConfigurationService.getSiteConfiguration();
        log.debug("[SITE_CONFIG] Found site configuration - fullName={}", config.getFullName());
        return ResponseEntity.ok(config);
    }
}
