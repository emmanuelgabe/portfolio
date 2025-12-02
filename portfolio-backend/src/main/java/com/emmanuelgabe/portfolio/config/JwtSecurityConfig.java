package com.emmanuelgabe.portfolio.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT Security Configuration.
 * Validates JWT secret at application startup to prevent insecure deployments.
 */
@Configuration
@Slf4j
public class JwtSecurityConfig {

    private static final int MIN_SECRET_LENGTH = 43;
    private static final String DEV_SECRET_MARKER = "dev";
    private static final String DEFAULT_SECRET_MARKER = "default";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Validate JWT secret configuration at application startup.
     * Ensures production and staging environments have a secure, unique secret.
     *
     * @throws IllegalStateException if secret is invalid for the active profile
     */
    @PostConstruct
    public void validateJwtSecret() {
        boolean isProductionLike = activeProfile.contains("prod") || activeProfile.contains("staging");
        boolean isDefaultSecret = jwtSecret.toLowerCase().contains(DEV_SECRET_MARKER)
                || jwtSecret.toLowerCase().contains(DEFAULT_SECRET_MARKER);

        if (isProductionLike) {
            // In production/staging: secret MUST be explicitly set and secure
            if (isDefaultSecret) {
                log.error("[SECURITY] JWT_SECRET contains default/dev value in {} environment", activeProfile);
                throw new IllegalStateException(
                        "SECURITY ERROR: JWT_SECRET contains default value in " + activeProfile + " environment. "
                                + "Set a secure JWT_SECRET environment variable using: openssl rand -base64 64"
                );
            }

            if (jwtSecret.length() < MIN_SECRET_LENGTH) {
                log.error("[SECURITY] JWT_SECRET is too short - length={}, required={}",
                        jwtSecret.length(), MIN_SECRET_LENGTH);
                throw new IllegalStateException(
                        "SECURITY ERROR: JWT_SECRET must be at least " + MIN_SECRET_LENGTH
                                + " characters (256 bits). Current length: " + jwtSecret.length()
                );
            }

            log.info("[SECURITY] JWT secret validation passed for {} environment", activeProfile);
        } else {
            // In development: warn if using default secret
            if (isDefaultSecret) {
                log.warn("[SECURITY] Using development JWT secret - NOT FOR PRODUCTION USE");
            } else {
                log.info("[SECURITY] JWT secret configured for {} environment", activeProfile);
            }
        }
    }
}
