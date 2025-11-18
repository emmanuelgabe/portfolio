package com.emmanuelgabe.portfolio.config;

import com.emmanuelgabe.portfolio.security.JwtAuthenticationFilter;
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

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
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
                // Disable CSRF (not needed for JWT stateless authentication)
                .csrf(AbstractHttpConfigurer::disable)

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/health", "/api/version", "/actuator/health").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        // Admin modification endpoints (require ADMIN role)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/projects/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/skills/admin/**").hasRole("ADMIN")

                        // Public read-only endpoints (GET only)
                        .requestMatchers(HttpMethod.GET, "/api/projects/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/skills/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Stateless session management (JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Set authentication provider
                .authenticationProvider(authenticationProvider())

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
