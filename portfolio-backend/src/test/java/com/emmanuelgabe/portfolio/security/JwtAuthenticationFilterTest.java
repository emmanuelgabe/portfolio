package com.emmanuelgabe.portfolio.security;

import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.entity.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        // Clear security context before each test
        SecurityContextHolder.clearContext();

        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setEmail("admin@example.com");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setRole(UserRole.ROLE_ADMIN);
        testUser.setEnabled(true);
        testUser.setAccountNonExpired(true);
        testUser.setAccountNonLocked(true);
        testUser.setCredentialsNonExpired(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        validToken = "valid.jwt.token";
    }

    @Test
    void doFilterInternal_WithValidToken_SetsAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getRequestURI()).thenReturn("/api/projects");
        when(jwtTokenProvider.extractUsername(validToken)).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(testUser);
        when(jwtTokenProvider.validateToken(validToken, testUser)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(testUser);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .hasSize(1);

        verify(jwtTokenProvider, times(1)).extractUsername(validToken);
        verify(userDetailsService, times(1)).loadUserByUsername("admin");
        verify(jwtTokenProvider, times(1)).validateToken(validToken, testUser);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithoutAuthorizationHeader_DoesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/projects");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider, never()).extractUsername(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithInvalidBearerFormat_DoesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat " + validToken);
        when(request.getRequestURI()).thenReturn("/api/projects");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider, never()).extractUsername(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithEmptyToken_DoesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        when(request.getRequestURI()).thenReturn("/api/projects");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider, never()).extractUsername(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithInvalidToken_DoesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getRequestURI()).thenReturn("/api/projects");
        when(jwtTokenProvider.extractUsername(validToken)).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(testUser);
        when(jwtTokenProvider.validateToken(validToken, testUser)).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider, times(1)).extractUsername(validToken);
        verify(userDetailsService, times(1)).loadUserByUsername("admin");
        verify(jwtTokenProvider, times(1)).validateToken(validToken, testUser);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WhenAuthenticationAlreadySet_SkipsTokenValidation() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getRequestURI()).thenReturn("/api/projects");
        when(jwtTokenProvider.extractUsername(validToken)).thenReturn("admin");

        // Set authentication manually before filter runs
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken existingAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        testUser, null, testUser.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existingAuth);

        verify(jwtTokenProvider, times(1)).extractUsername(validToken);
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtTokenProvider, never()).validateToken(any(), any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithExceptionDuringTokenExtraction_DoesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getRequestURI()).thenReturn("/api/projects");
        when(jwtTokenProvider.extractUsername(validToken))
                .thenThrow(new RuntimeException("Token extraction failed"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider, times(1)).extractUsername(validToken);
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithNullUsername_DoesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getRequestURI()).thenReturn("/api/projects");
        when(jwtTokenProvider.extractUsername(validToken)).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider, times(1)).extractUsername(validToken);
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_AlwaysCallsFilterChain() throws ServletException, IOException {
        // Arrange - Multiple scenarios
        when(request.getRequestURI()).thenReturn("/api/projects");

        // Test 1: No token
        when(request.getHeader("Authorization")).thenReturn(null);
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);

        // Test 2: Valid token
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.extractUsername(validToken)).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(testUser);
        when(jwtTokenProvider.validateToken(validToken, testUser)).thenReturn(true);

        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(2)).doFilter(request, response);
    }
}
