package com.emmanuelgabe.portfolio.config;

import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.security.JwtAuthenticationFilter;
import com.emmanuelgabe.portfolio.security.JwtTokenProvider;
import com.emmanuelgabe.portfolio.service.AuthRateLimitService;
import com.emmanuelgabe.portfolio.service.ImageService;
import com.emmanuelgabe.portfolio.service.SvgStorageService;
import com.emmanuelgabe.portfolio.service.UploadRateLimitService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test configuration for security beans
 * Provides mocked security beans for @WebMvcTest tests
 */
@TestConfiguration
public class TestSecurityConfig {

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private FileStorageProperties fileStorageProperties;

    @MockBean
    private ImageStorageProperties imageStorageProperties;

    @MockBean
    private CvStorageProperties cvStorageProperties;

    @MockBean
    private ImageService imageService;

    @MockBean
    private SvgStorageProperties svgStorageProperties;

    @MockBean
    private SvgStorageService svgStorageService;

    @MockBean
    private AuthRateLimitService authRateLimitService;

    @MockBean
    private UploadRateLimitService uploadRateLimitService;

    @MockBean
    private BusinessMetrics businessMetrics;
}
