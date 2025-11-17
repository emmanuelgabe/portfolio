package com.emmanuelgabe.portfolio.config;

import com.emmanuelgabe.portfolio.security.JwtAuthenticationFilter;
import com.emmanuelgabe.portfolio.security.JwtTokenProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
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
}
