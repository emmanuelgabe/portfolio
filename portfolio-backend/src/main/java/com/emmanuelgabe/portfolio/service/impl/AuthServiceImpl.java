package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.audit.AuditContextHolder;
import com.emmanuelgabe.portfolio.dto.AuthResponse;
import com.emmanuelgabe.portfolio.dto.ChangePasswordRequest;
import com.emmanuelgabe.portfolio.dto.LoginRequest;
import com.emmanuelgabe.portfolio.entity.RefreshToken;
import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.exception.InvalidCredentialsException;
import com.emmanuelgabe.portfolio.exception.InvalidTokenException;
import com.emmanuelgabe.portfolio.mapper.UserMapper;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.repository.UserRepository;
import com.emmanuelgabe.portfolio.security.JwtTokenProvider;
import com.emmanuelgabe.portfolio.service.AuditService;
import com.emmanuelgabe.portfolio.service.AuthService;
import com.emmanuelgabe.portfolio.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AuthService interface
 * Handles business logic for authentication and token management
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final BusinessMetrics metrics;
    private final AuditService auditService;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("[AUTH_LOGIN] Login attempt received");
        metrics.recordAuthAttempt();

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            User user = (User) authentication.getPrincipal();
            log.debug("[AUTH_LOGIN] Authentication successful - username={}, role={}",
                    user.getUsername(), user.getRole());

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            // Build response
            AuthResponse response = userMapper.toAuthResponse(user);
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken.getToken());
            response.setExpiresIn(accessTokenExpiration);

            log.info("[AUTH_LOGIN] Login successful - username={}, role={}", user.getUsername(), user.getRole());

            // Audit login success
            logAuthAudit(AuditAction.LOGIN, user.getUsername(), true, null);

            return response;

        } catch (BadCredentialsException ex) {
            metrics.recordAuthFailure();
            log.warn("[AUTH_LOGIN] Invalid credentials provided");

            // Audit login failure
            logAuthAudit(AuditAction.LOGIN_FAILED, loginRequest.getUsername(), false,
                    "Invalid credentials");

            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    @Override
    public AuthResponse refreshToken(String refreshTokenStr) {
        log.info("[AUTH_REFRESH] Token refresh attempt");

        try {
            // Find and verify refresh token
            RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr);
            refreshToken = refreshTokenService.verifyExpiration(refreshToken);

            User user = refreshToken.getUser();
            log.debug("[AUTH_REFRESH] Refresh token valid - username={}", user.getUsername());

            // Generate new access token
            String newAccessToken = jwtTokenProvider.generateAccessToken(user);

            // Build response
            AuthResponse response = userMapper.toAuthResponse(user);
            response.setAccessToken(newAccessToken);
            response.setRefreshToken(refreshToken.getToken());
            response.setExpiresIn(accessTokenExpiration);

            log.info("[AUTH_REFRESH] Token refresh successful - username={}", user.getUsername());

            return response;

        } catch (RuntimeException ex) {
            log.warn("[AUTH_REFRESH] Token refresh failed - error={}", ex.getMessage());
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
    }

    @Override
    public void logout(String refreshTokenStr) {
        log.info("[AUTH_LOGOUT] Logout attempt");

        try {
            RefreshToken token = refreshTokenService.findByToken(refreshTokenStr);
            String username = token.getUser().getUsername();

            refreshTokenService.revokeToken(refreshTokenStr);
            log.info("[AUTH_LOGOUT] Logout successful");

            // Audit logout
            logAuthAudit(AuditAction.LOGOUT, username, true, null);

        } catch (RuntimeException ex) {
            log.warn("[AUTH_LOGOUT] Logout failed - error={}", ex.getMessage());
            throw new InvalidTokenException("Invalid refresh token");
        }
    }

    @Override
    public void changePassword(String username, ChangePasswordRequest request) {
        log.info("[AUTH_CHANGE_PASSWORD] Password change attempt - username={}", username);

        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("[AUTH_CHANGE_PASSWORD] Password confirmation mismatch - username={}", username);
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[AUTH_CHANGE_PASSWORD] User not found - username={}", username);
                    return new UsernameNotFoundException("User not found");
                });

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("[AUTH_CHANGE_PASSWORD] Current password incorrect - username={}", username);
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Validate new password is different
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            log.warn("[AUTH_CHANGE_PASSWORD] New password same as current - username={}", username);
            throw new IllegalArgumentException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all existing refresh tokens for security
        refreshTokenService.revokeAllUserTokens(user);

        log.info("[AUTH_CHANGE_PASSWORD] Password changed successfully - username={}", username);

        // Audit password change
        logAuthAudit(AuditAction.PASSWORD_CHANGE, username, true, null);
    }

    /**
     * Helper method to log authentication audit events.
     */
    private void logAuthAudit(AuditAction action, String username, boolean success, String error) {
        try {
            var context = AuditContextHolder.getContext();
            String ipAddress = context != null ? context.getIpAddress() : null;
            String userAgent = context != null ? context.getUserAgent() : null;

            auditService.logAuthEvent(action, username, ipAddress, userAgent, success, error);
        } catch (Exception e) {
            log.warn("[AUTH_AUDIT] Failed to log auth event - action={}, error={}",
                    action, e.getMessage());
        }
    }
}
