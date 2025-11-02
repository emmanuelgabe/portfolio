package com.emmanuelgabe.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    /**
     * Configure CSRF protection with cookie-based tokens
     */
    private void configureCsrf(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http.csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(requestHandler)
        );
    }

    /**
     * Configure security headers for production environments
     */
    private void configureSecurityHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
        );
    }

    /**
     * Security configuration for LOCAL/DEV environment
     * - CSRF disabled for easier testing
     * - All endpoints accessible without authentication
     */
    @Bean
    @Profile("dev")
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/api/**", "/actuator/**").permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }

    /**
     * Security configuration for STAGING environment
     * - CSRF enabled with cookie-based tokens
     * - Security headers enabled
     * - Public endpoints: /health, /api/version
     * - Protected endpoints: /api/items/** (require auth in future)
     */
    @Bean
    @Profile("staging")
    public SecurityFilterChain stagingSecurityFilterChain(HttpSecurity http) throws Exception {
        configureCsrf(http);
        configureSecurityHeaders(http);

        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/health", "/api/version", "/actuator/health").permitAll()
            .requestMatchers("/api/**").permitAll()
            .anyRequest().authenticated()
        );

        return http.build();
    }

    /**
     * Security configuration for PRODUCTION environment
     * - CSRF enabled with cookie-based tokens
     * - Strict security headers
     * - Public endpoints: /health, /api/version
     * - Protected endpoints: /api/items/** (require auth in future)
     */
    @Bean
    @Profile("prod")
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
        configureCsrf(http);
        configureSecurityHeaders(http);

        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/health", "/api/version", "/actuator/health").permitAll()
            .requestMatchers("/api/**").permitAll()
            .anyRequest().authenticated()
        );

        return http.build();
    }
}
