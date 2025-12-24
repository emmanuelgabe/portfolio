package com.emmanuelgabe.portfolio.service;

import java.time.LocalDateTime;

/**
 * Service for generating and managing the sitemap.xml file.
 */
public interface SitemapService {

    /**
     * Generate the sitemap XML content.
     *
     * @return the sitemap XML as a string
     */
    String generateSitemap();

    /**
     * Write the sitemap to the configured output file.
     *
     * @return the path where the sitemap was written
     */
    String writeSitemapToFile();

    /**
     * Get the last generation timestamp.
     *
     * @return the timestamp of the last sitemap generation, or null if never generated
     */
    LocalDateTime getLastGeneratedAt();
}
