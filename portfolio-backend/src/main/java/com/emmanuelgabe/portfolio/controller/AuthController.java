package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.AccessTokenResponse;
import com.emmanuelgabe.portfolio.dto.AuthResponse;
import com.emmanuelgabe.portfolio.dto.ChangePasswordRequest;
import com.emmanuelgabe.portfolio.dto.LoginRequest;
import com.emmanuelgabe.portfolio.exception.InvalidTokenException;
import com.emmanuelgabe.portfolio.service.AuthService;
import com.emmanuelgabe.portfolio.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
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

    private static final int REFRESH_TOKEN_MAX_AGE_DAYS = 7;

    private final AuthService authService;

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    /**
     * Authenticate user and generate tokens.
     * Access token is returned in response body.
     * Refresh token is set as HttpOnly cookie for security.
     *
     * @param loginRequest Login credentials
     * @param response     HTTP response for setting cookie
     * @return Access token response (refresh token in HttpOnly cookie)
     */
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {
        log.info("[LOGIN_REQUEST] Login request received - username={}", loginRequest.getUsername());

        AuthResponse authResponse = authService.login(loginRequest);

        // Set refresh token as HttpOnly cookie
        ResponseCookie cookie = CookieUtils.createRefreshTokenCookie(
                authResponse.getRefreshToken(),
                REFRESH_TOKEN_MAX_AGE_DAYS,
                secureCookie
        );
        CookieUtils.addCookie(response, cookie);

        log.info("[LOGIN_REQUEST] Login successful - username={}", loginRequest.getUsername());
        return ResponseEntity.ok(buildAccessTokenResponse(authResponse));
    }

    /**
     * Refresh access token using refresh token from HttpOnly cookie.
     * Implements token rotation: new refresh token is issued on each refresh.
     *
     * @param request  HTTP request containing refresh token cookie
     * @param response HTTP response for setting new cookie
     * @return New access token response
     */
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("[REFRESH_REQUEST] Token refresh request received");

        // Get refresh token from HttpOnly cookie
        String refreshToken = CookieUtils.getRefreshTokenFromCookie(request)
                .orElseThrow(() -> {
                    log.warn("[REFRESH_REQUEST] No refresh token cookie found");
                    return new InvalidTokenException("No refresh token provided");
                });

        AuthResponse authResponse = authService.refreshToken(refreshToken);

        // Set new refresh token cookie (token rotation)
        ResponseCookie cookie = CookieUtils.createRefreshTokenCookie(
                authResponse.getRefreshToken(),
                REFRESH_TOKEN_MAX_AGE_DAYS,
                secureCookie
        );
        CookieUtils.addCookie(response, cookie);

        log.info("[REFRESH_REQUEST] Token refresh successful");
        return ResponseEntity.ok(buildAccessTokenResponse(authResponse));
    }

    /**
     * Logout user by revoking refresh token and clearing cookie.
     *
     * @param request  HTTP request containing refresh token cookie
     * @param response HTTP response for clearing cookie
     * @return Empty response with cleared cookie
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("[LOGOUT_REQUEST] Logout request received");

        // Get refresh token from cookie and revoke it
        CookieUtils.getRefreshTokenFromCookie(request)
                .ifPresent(token -> {
                    try {
                        authService.logout(token);
                    } catch (Exception e) {
                        log.warn("[LOGOUT_REQUEST] Failed to revoke token - error={}", e.getMessage());
                    }
                });

        // Always clear the cookie, even if token revocation fails
        ResponseCookie deleteCookie = CookieUtils.createDeleteRefreshTokenCookie(secureCookie);
        CookieUtils.addCookie(response, deleteCookie);

        log.info("[LOGOUT_REQUEST] Logout successful");
        return ResponseEntity.ok().build();
    }

    /**
     * Build AccessTokenResponse from AuthResponse.
     * Extracts only the fields needed for the response body (no refresh token).
     */
    private AccessTokenResponse buildAccessTokenResponse(AuthResponse authResponse) {
        return AccessTokenResponse.builder()
                .accessToken(authResponse.getAccessToken())
                .expiresIn(authResponse.getExpiresIn())
                .username(authResponse.getUsername())
                .role(authResponse.getRole())
                .build();
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
