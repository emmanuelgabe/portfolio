package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.AuthResponse;
import com.emmanuelgabe.portfolio.dto.ChangePasswordRequest;
import com.emmanuelgabe.portfolio.dto.LoginRequest;
import com.emmanuelgabe.portfolio.dto.TokenRefreshRequest;
import com.emmanuelgabe.portfolio.entity.UserRole;
import com.emmanuelgabe.portfolio.exception.InvalidCredentialsException;
import com.emmanuelgabe.portfolio.exception.InvalidTokenException;
import com.emmanuelgabe.portfolio.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private LoginRequest loginRequest;
    private AuthResponse authResponse;
    private TokenRefreshRequest tokenRefreshRequest;
    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    void setUp() {
        // Setup login request
        loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password123");

        // Setup auth response
        authResponse = new AuthResponse();
        authResponse.setUsername("admin");
        authResponse.setEmail("admin@example.com");
        authResponse.setRole(UserRole.ROLE_ADMIN.name());
        authResponse.setAccessToken("eyJhbGciOiJIUzI1NiJ9.test.token");
        authResponse.setRefreshToken("refresh-token-uuid");
        authResponse.setExpiresIn(900000L);

        // Setup token refresh request
        tokenRefreshRequest = new TokenRefreshRequest();
        tokenRefreshRequest.setRefreshToken("refresh-token-uuid");

        // Setup change password request
        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("OldPassword123!");
        changePasswordRequest.setNewPassword("NewPassword123!");
        changePasswordRequest.setConfirmPassword("NewPassword123!");
    }

    @Test
    void should_returnAuthResponse_when_loginCalledWithValidCredentials() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.email", is("admin@example.com")))
                .andExpect(jsonPath("$.role", is("ROLE_ADMIN")))
                .andExpect(jsonPath("$.accessToken", is("eyJhbGciOiJIUzI1NiJ9.test.token")))
                .andExpect(jsonPath("$.refreshToken", is("refresh-token-uuid")))
                .andExpect(jsonPath("$.expiresIn", is(900000)));

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
                .andExpect(jsonPath("$.message", containsString("Invalid username or password")));

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

    @Test
    void should_returnNewAuthResponse_when_refreshTokenCalledWithValidToken() throws Exception {
        // Arrange
        when(authService.refreshToken(any(String.class)))
                .thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRefreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.email", is("admin@example.com")))
                .andExpect(jsonPath("$.accessToken", is("eyJhbGciOiJIUzI1NiJ9.test.token")))
                .andExpect(jsonPath("$.refreshToken", is("refresh-token-uuid")));

        verify(authService, times(1)).refreshToken(any(String.class));
    }

    @Test
    void should_returnUnauthorized_when_refreshTokenCalledWithInvalidToken() throws Exception {
        // Arrange
        when(authService.refreshToken(any(String.class)))
                .thenThrow(new InvalidTokenException("Invalid or expired refresh token"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRefreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Invalid or expired refresh token")));

        verify(authService, times(1)).refreshToken(any(String.class));
    }

    @Test
    void should_returnBadRequest_when_refreshTokenCalledWithBlankToken() throws Exception {
        // Arrange
        tokenRefreshRequest.setRefreshToken("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRefreshRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_returnOk_when_logoutCalledWithValidToken() throws Exception {
        // Arrange
        doNothing().when(authService).logout(any(String.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRefreshRequest)))
                .andExpect(status().isOk());

        verify(authService, times(1)).logout(any(String.class));
    }

    @Test
    void should_returnUnauthorized_when_logoutCalledWithInvalidToken() throws Exception {
        // Arrange
        doThrow(new InvalidTokenException("Invalid refresh token"))
                .when(authService).logout(any(String.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRefreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Invalid refresh token")));

        verify(authService, times(1)).logout(any(String.class));
    }

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
                .andExpect(jsonPath("$.message", containsString("Current password is incorrect")));

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