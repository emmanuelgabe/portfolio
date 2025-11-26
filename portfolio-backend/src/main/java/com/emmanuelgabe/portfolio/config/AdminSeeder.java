package com.emmanuelgabe.portfolio.config;

import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.entity.UserRole;
import com.emmanuelgabe.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Seeds the admin user on application startup.
 * Uses BCrypt password hash from environment variable for security.
 * Creates admin if not exists, updates password hash if changed.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_EMAIL = "admin@portfolio.local";

    @Value("${app.admin.password-hash:#{null}}")
    private String adminPasswordHash;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public void run(String... args) {
        if (adminPasswordHash == null || adminPasswordHash.isBlank()) {
            handleMissingPasswordHash();
            return;
        }

        Optional<User> existingAdmin = userRepository.findByUsername(ADMIN_USERNAME);

        if (existingAdmin.isPresent()) {
            updateAdminPasswordIfNeeded(existingAdmin.get());
            return;
        }

        createAdminUser();
    }

    /**
     * Update admin password hash if it differs from environment configuration.
     * @param admin Existing admin user
     */
    private void updateAdminPasswordIfNeeded(User admin) {
        if (!adminPasswordHash.equals(admin.getPassword())) {
            admin.setPassword(adminPasswordHash);
            userRepository.save(admin);
            log.info("[ADMIN_SEEDER] Admin password hash updated - username={}", ADMIN_USERNAME);
        } else {
            log.debug("[ADMIN_SEEDER] Admin user exists with correct hash - username={}", ADMIN_USERNAME);
        }
    }

    /**
     * Handle case when password hash is not configured.
     * In dev: log warning. In prod/staging: log error.
     */
    private void handleMissingPasswordHash() {
        if (isProductionLike()) {
            log.error("[ADMIN_SEEDER] ADMIN_PASSWORD_HASH not configured in {} environment. "
                    + "Admin user will NOT be created. Set ADMIN_PASSWORD_HASH environment variable.",
                    activeProfile);
        } else {
            log.warn("[ADMIN_SEEDER] ADMIN_PASSWORD_HASH not configured. "
                    + "Admin user will NOT be created. "
                    + "Set ADMIN_PASSWORD_HASH in .env or environment.");
        }
    }

    /**
     * Create the admin user with password hash from environment.
     */
    private void createAdminUser() {
        User admin = new User();
        admin.setUsername(ADMIN_USERNAME);
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(adminPasswordHash);
        admin.setRole(UserRole.ROLE_ADMIN);
        admin.setEnabled(true);
        admin.setAccountNonExpired(true);
        admin.setAccountNonLocked(true);
        admin.setCredentialsNonExpired(true);

        userRepository.save(admin);

        log.info("[ADMIN_SEEDER] Admin user created - username={}", ADMIN_USERNAME);
    }

    private boolean isProductionLike() {
        return activeProfile.contains("prod") || activeProfile.contains("staging");
    }
}
