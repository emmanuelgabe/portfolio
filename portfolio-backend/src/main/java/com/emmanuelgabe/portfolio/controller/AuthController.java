package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.AuthResponse;
import com.emmanuelgabe.portfolio.dto.ChangePasswordRequest;
import com.emmanuelgabe.portfolio.dto.LoginRequest;
import com.emmanuelgabe.portfolio.dto.TokenRefreshRequest;
import com.emmanuelgabe.portfolio.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication Controller
 * Handles authentication endpoints for login, token refresh, and logout
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticate user and generate tokens
     * @param loginRequest Login credentials
     * @return Authentication response with JWT and refresh token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("[LOGIN_REQUEST] Login request received - username={}", loginRequest.getUsername());

        AuthResponse response = authService.login(loginRequest);

        log.info("[LOGIN_REQUEST] Login successful - username={}", loginRequest.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token
     * @param refreshRequest Refresh token request
     * @return New authentication response with fresh tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest refreshRequest) {
        log.info("[REFRESH_REQUEST] Token refresh request received");

        AuthResponse response = authService.refreshToken(refreshRequest.getRefreshToken());

        log.info("[REFRESH_REQUEST] Token refresh successful");
        return ResponseEntity.ok(response);
    }

    /**
     * Logout user by revoking refresh token
     * @param refreshRequest Refresh token to revoke
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody TokenRefreshRequest refreshRequest) {
        log.info("[LOGOUT_REQUEST] Logout request received");

        authService.logout(refreshRequest.getRefreshToken());

        log.info("[LOGOUT_REQUEST] Logout successful");
        return ResponseEntity.ok().build();
    }

    /**
     * Change user password
     * Requires authentication
     * @param userDetails Authenticated user details
     * @param changePasswordRequest Password change request
     * @return Success message
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest
    ) {
        log.info("[CHANGE_PASSWORD_REQUEST] Password change request received - username={}",
                userDetails.getUsername());

        authService.changePassword(userDetails.getUsername(), changePasswordRequest);

        log.info("[CHANGE_PASSWORD_REQUEST] Password changed successfully - username={}",
                userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
