package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.AuthResponse;
import com.emmanuelgabe.portfolio.dto.ChangePasswordRequest;
import com.emmanuelgabe.portfolio.dto.LoginRequest;
import com.emmanuelgabe.portfolio.entity.UserRole;
import com.emmanuelgabe.portfolio.exception.InvalidCredentialsException;
import com.emmanuelgabe.portfolio.exception.InvalidTokenException;
import com.emmanuelgabe.portfolio.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private LoginRequest loginRequest;
    private AuthResponse authResponse;
    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    void setUp() {
        // Setup login request
        loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password123");

        // Setup auth response (from AuthService)
        authResponse = new AuthResponse();
        authResponse.setUsername("admin");
        authResponse.setEmail("admin@example.com");
        authResponse.setRole(UserRole.ROLE_ADMIN.name());
        authResponse.setAccessToken("eyJhbGciOiJIUzI1NiJ9.test.token");
        authResponse.setRefreshToken("refresh-token-uuid");
        authResponse.setExpiresIn(900000L);

        // Setup change password request
        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("OldPassword123!");
        changePasswordRequest.setNewPassword("NewPassword123!");
        changePasswordRequest.setConfirmPassword("NewPassword123!");
    }

    // ========== Login Tests ==========

    @Test
    void should_returnAccessTokenResponse_when_loginCalledWithValidCredentials() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.role", is("ROLE_ADMIN")))
                .andExpect(jsonPath("$.accessToken", is("eyJhbGciOiJIUzI1NiJ9.test.token")))
                .andExpect(jsonPath("$.expiresIn", is(900000)))
                // Verify refresh token is NOT in response body
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                // Verify refresh token is set as HttpOnly cookie
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(cookie().value(REFRESH_TOKEN_COOKIE_NAME, "refresh-token-uuid"))
                .andExpect(cookie().httpOnly(REFRESH_TOKEN_COOKIE_NAME, true));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void should_returnUnauthorized_when_loginCalledWithInvalidCredentials() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid username or password"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void should_returnBadRequest_when_loginCalledWithBlankUsername() throws Exception {
        // Arrange
        loginRequest.setUsername("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_returnBadRequest_when_loginCalledWithBlankPassword() throws Exception {
        // Arrange
        loginRequest.setPassword("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    // ========== Refresh Token Tests ==========

    @Test
    void should_returnNewAccessTokenResponse_when_refreshTokenCalledWithValidCookie() throws Exception {
        // Arrange
        when(authService.refreshToken("refresh-token-uuid"))
                .thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE_NAME, "refresh-token-uuid")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.accessToken", is("eyJhbGciOiJIUzI1NiJ9.test.token")))
                // Verify refresh token is NOT in response body
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                // Verify new refresh token is set as HttpOnly cookie (token rotation)
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(cookie().httpOnly(REFRESH_TOKEN_COOKIE_NAME, true));

        verify(authService, times(1)).refreshToken("refresh-token-uuid");
    }

    @Test
    void should_returnUnauthorized_when_refreshTokenCalledWithInvalidCookie() throws Exception {
        // Arrange
        when(authService.refreshToken("invalid-token"))
                .thenThrow(new InvalidTokenException("Invalid or expired refresh token"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE_NAME, "invalid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(authService, times(1)).refreshToken("invalid-token");
    }

    @Test
    void should_returnUnauthorized_when_refreshTokenCalledWithoutCookie() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(authService, never()).refreshToken(any());
    }

    // ========== Logout Tests ==========

    @Test
    void should_returnOkAndClearCookie_when_logoutCalledWithValidCookie() throws Exception {
        // Arrange
        doNothing().when(authService).logout("refresh-token-uuid");

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE_NAME, "refresh-token-uuid")))
                .andExpect(status().isOk())
                // Verify cookie is cleared (max-age = 0)
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, 0));

        verify(authService, times(1)).logout("refresh-token-uuid");
    }

    @Test
    void should_returnOkAndClearCookie_when_logoutCalledWithoutCookie() throws Exception {
        // Act & Assert - logout should succeed even without cookie
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                // Verify cookie is cleared
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, 0));

        // Verify logout was not called on service (no token to revoke)
        verify(authService, never()).logout(any());
    }

    @Test
    void should_returnOkAndClearCookie_when_logoutCalledWithInvalidCookie() throws Exception {
        // Arrange - logout should succeed even if token revocation fails
        doThrow(new InvalidTokenException("Invalid refresh token"))
                .when(authService).logout("invalid-token");

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE_NAME, "invalid-token")))
                .andExpect(status().isOk())
                // Cookie should still be cleared
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, 0));

        verify(authService, times(1)).logout("invalid-token");
    }

    // ========== Change Password Tests ==========

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_returnOk_when_changePasswordCalledWithValidRequest() throws Exception {
        // Arrange
        doNothing().when(authService).changePassword(eq("admin"), any(ChangePasswordRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk());

        verify(authService, times(1)).changePassword(eq("admin"), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_returnUnauthorized_when_changePasswordCalledWithIncorrectCurrentPassword() throws Exception {
        // Arrange
        doThrow(new InvalidCredentialsException("Current password is incorrect"))
                .when(authService).changePassword(eq("admin"), any(ChangePasswordRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(authService, times(1)).changePassword(eq("admin"), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_returnBadRequest_when_changePasswordCalledWithMismatchedPasswords() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("New password and confirmation do not match"))
                .when(authService).changePassword(eq("admin"), any(ChangePasswordRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, times(1)).changePassword(eq("admin"), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_returnBadRequest_when_changePasswordCalledWithBlankCurrentPassword() throws Exception {
        // Arrange
        changePasswordRequest.setCurrentPassword("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_returnBadRequest_when_changePasswordCalledWithBlankNewPassword() throws Exception {
        // Arrange
        changePasswordRequest.setNewPassword("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isBadRequest());
    }
}
