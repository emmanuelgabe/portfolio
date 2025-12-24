package com.emmanuelgabe.portfolio.security;

import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.service.AuthRateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRateLimitFilterTest {

    private static final String TEST_IP = "192.168.1.100";
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REFRESH_PATH = "/api/auth/refresh";

    @Mock
    private AuthRateLimitService authRateLimitService;

    @Mock
    private BusinessMetrics metrics;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ObjectMapper objectMapper;

    private AuthRateLimitFilter authRateLimitFilter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authRateLimitFilter = new AuthRateLimitFilter(authRateLimitService, metrics, objectMapper);
    }

    // ========== shouldNotFilter Tests ==========

    @Test
    void should_notFilter_when_nonAuthEndpoint() {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/projects");

        // Act
        boolean shouldNotFilter = authRateLimitFilter.shouldNotFilter(request);

        // Assert
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    void should_filter_when_loginEndpoint() {
        // Arrange
        when(request.getRequestURI()).thenReturn(LOGIN_PATH);

        // Act
        boolean shouldNotFilter = authRateLimitFilter.shouldNotFilter(request);

        // Assert
        assertThat(shouldNotFilter).isFalse();
    }

    @Test
    void should_filter_when_refreshEndpoint() {
        // Arrange
        when(request.getRequestURI()).thenReturn(REFRESH_PATH);

        // Act
        boolean shouldNotFilter = authRateLimitFilter.shouldNotFilter(request);

        // Assert
        assertThat(shouldNotFilter).isFalse();
    }

    // ========== Login Endpoint Tests ==========

    @Test
    void should_allowLogin_when_rateLimitNotExceeded() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn(LOGIN_PATH);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        when(request.getMethod()).thenReturn("POST");
        when(authRateLimitService.isLoginAllowed(TEST_IP)).thenReturn(true);

        // Act
        authRateLimitFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void should_blockLogin_when_rateLimitExceeded() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn(LOGIN_PATH);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        when(request.getMethod()).thenReturn("POST");
        when(authRateLimitService.isLoginAllowed(TEST_IP)).thenReturn(false);
        when(authRateLimitService.getMaxLoginRequestsPerHour()).thenReturn(5);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act
        authRateLimitFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");

        writer.flush();
        String responseBody = stringWriter.toString();
        assertThat(responseBody).contains("429");
        assertThat(responseBody).contains("Too Many Requests");
        assertThat(responseBody).contains("login");
    }

    @Test
    void should_allowGetRequest_when_loginEndpoint() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn(LOGIN_PATH);
        when(request.getMethod()).thenReturn("GET");

        // Act
        authRateLimitFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(authRateLimitService, never()).isLoginAllowed(TEST_IP);
    }

    // ========== Refresh Endpoint Tests ==========

    @Test
    void should_allowRefresh_when_rateLimitNotExceeded() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn(REFRESH_PATH);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        when(request.getMethod()).thenReturn("POST");
        when(authRateLimitService.isRefreshAllowed(TEST_IP)).thenReturn(true);

        // Act
        authRateLimitFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void should_blockRefresh_when_rateLimitExceeded() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn(REFRESH_PATH);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        when(request.getMethod()).thenReturn("POST");
        when(authRateLimitService.isRefreshAllowed(TEST_IP)).thenReturn(false);
        when(authRateLimitService.getMaxRefreshRequestsPerHour()).thenReturn(10);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Act
        authRateLimitFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");

        writer.flush();
        String responseBody = stringWriter.toString();
        assertThat(responseBody).contains("429");
        assertThat(responseBody).contains("Too Many Requests");
        assertThat(responseBody).contains("refresh");
    }

    // ========== IP Address Extraction Tests ==========

    @Test
    void should_useXForwardedFor_when_headerPresent() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn(LOGIN_PATH);
        when(request.getMethod()).thenReturn("POST");
        String forwardedIp = "10.0.0.1";
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedIp);
        when(authRateLimitService.isLoginAllowed(forwardedIp)).thenReturn(true);

        // Act
        authRateLimitFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(authRateLimitService).isLoginAllowed(forwardedIp);
    }

    @Test
    void should_useFirstIp_when_multipleIpsInXForwardedFor() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn(LOGIN_PATH);
        when(request.getMethod()).thenReturn("POST");
        String forwardedIps = "10.0.0.1, 10.0.0.2, 10.0.0.3";
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedIps);
        when(authRateLimitService.isLoginAllowed("10.0.0.1")).thenReturn(true);

        // Act
        authRateLimitFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(authRateLimitService).isLoginAllowed("10.0.0.1");
    }

    @Test
    void should_useXRealIp_when_xForwardedForNotPresent() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn(LOGIN_PATH);
        when(request.getMethod()).thenReturn("POST");
        String realIp = "10.0.0.5";
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(realIp);
        when(authRateLimitService.isLoginAllowed(realIp)).thenReturn(true);

        // Act
        authRateLimitFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(authRateLimitService).isLoginAllowed(realIp);
    }

    @Test
    void should_useRemoteAddress_when_noProxyHeaders() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn(LOGIN_PATH);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
        when(authRateLimitService.isLoginAllowed(TEST_IP)).thenReturn(true);

        // Act
        authRateLimitFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(authRateLimitService).isLoginAllowed(TEST_IP);
    }
}
