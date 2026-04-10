package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.chat.ToolCallTracker;
import com.emmanuelgabe.portfolio.dto.CvResponse;
import com.emmanuelgabe.portfolio.dto.SiteConfigurationResponse;
import com.emmanuelgabe.portfolio.service.CvService;
import com.emmanuelgabe.portfolio.service.SiteConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

/**
 * Agent tools for retrieving site configuration and CV information.
 */
@Slf4j
@RequiredArgsConstructor
public class SiteInfoTool {

    private final SiteConfigurationService siteConfigService;
    private final CvService cvService;
    private final ToolCallTracker tracker;

    /**
     * Get the site configuration (bio, social links, etc.).
     *
     * @return site configuration with name, bio, hero text, and social URLs
     */
    @Tool(description = "Get site configuration: Emmanuel's full name, bio, hero description, "
            + "profile image URL, and social links (GitHub, LinkedIn).")
    public SiteConfigurationResponse getSiteConfig() {
        tracker.record("getSiteConfig");
        log.debug("[CHAT_TOOL] getSiteConfig");
        return siteConfigService.getSiteConfiguration();
    }

    /**
     * Get the current CV download URL.
     *
     * @return current CV data with file URL, or null if none available
     */
    @Tool(description = "Get the link to Emmanuel's current CV/resume PDF file for download.")
    public CvResponse getCurrentCV() {
        tracker.record("getCurrentCV");
        log.debug("[CHAT_TOOL] getCurrentCV");
        return cvService.getCurrentCv().orElse(null);
    }
}
