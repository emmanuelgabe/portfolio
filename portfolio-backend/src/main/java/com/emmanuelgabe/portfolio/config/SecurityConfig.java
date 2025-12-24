package com.emmanuelgabe.portfolio.config;

import com.emmanuelgabe.portfolio.security.AuthRateLimitFilter;
import com.emmanuelgabe.portfolio.security.JwtAuthenticationFilter;
import com.emmanuelgabe.portfolio.security.UploadRateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration
 * Configures JWT-based authentication and authorization
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthRateLimitFilter authRateLimitFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UploadRateLimitFilter uploadRateLimitFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Password encoder bean
     * Uses BCrypt algorithm for secure password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication provider bean
     * Configures authentication with UserDetailsService and PasswordEncoder
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication manager bean
     * Required for manual authentication in AuthService
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Security filter chain
     * Configures HTTP security with JWT authentication
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS with configuration from CorsConfig
                .cors(Customizer.withDefaults())

                // Disable CSRF (not needed for JWT stateless authentication)
                .csrf(AbstractHttpConfigurer::disable)

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public health check endpoints
                        .requestMatchers("/health", "/api/version", "/actuator/health", "/api/health/**").permitAll()

                        // Prometheus metrics endpoint - accessible within app but protected at network level
                        // Security: Nginx does NOT route /actuator/* to external traffic
                        // Only accessible from Docker internal network (Prometheus scraping)
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/contact").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/visitors/heartbeat").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/graphql", "/graphiql/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/cv/current", "/api/cv/download").permitAll()

                        // Admin endpoints (require ADMIN role)
                        // Standardized pattern: /api/admin/{resource}/*
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Public read-only endpoints (GET only)
                        // Note: In Spring Security 6.x, /** does NOT match the root path
                        // Must include both /api/x and /api/x/** patterns
                        .requestMatchers(HttpMethod.GET, "/api/projects", "/api/projects/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/skills", "/api/skills/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags", "/api/tags/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/experiences", "/api/experiences/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles", "/api/articles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/hero", "/api/hero/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/configuration").permitAll()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Stateless session management (JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Set authentication provider
                .authenticationProvider(authenticationProvider())

                // Add rate limiting filter before JWT filter for auth endpoints
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Add upload rate limiting filter after JWT (needs authentication context)
                .addFilterAfter(uploadRateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
