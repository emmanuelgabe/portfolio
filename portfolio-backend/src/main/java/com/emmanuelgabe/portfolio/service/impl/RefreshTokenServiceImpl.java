package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.entity.RefreshToken;
import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.repository.RefreshTokenRepository;
import com.emmanuelgabe.portfolio.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of RefreshTokenService interface
 * Handles business logic for refresh token management
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Override
    public RefreshToken createRefreshToken(User user) {
        log.debug("[CREATE_REFRESH_TOKEN] Creating refresh token - userId={}, username={}",
                user.getId(), user.getUsername());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));
        refreshToken.setRevoked(false);

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);

        log.info("[CREATE_REFRESH_TOKEN] Refresh token created - userId={}, username={}, expiryDate={}",
                user.getId(), user.getUsername(), savedToken.getExpiryDate());

        return savedToken;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        log.debug("[FIND_REFRESH_TOKEN] Finding refresh token - token={}...", token.substring(0, 8));

        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("[FIND_REFRESH_TOKEN] Refresh token not found - token={}...", token.substring(0, 8));
                    return new RuntimeException("Refresh token not found");
                });
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        log.debug("[VERIFY_REFRESH_TOKEN] Verifying refresh token - userId={}, expiryDate={}",
                token.getUser().getId(), token.getExpiryDate());

        if (token.isRevoked()) {
            log.warn("[VERIFY_REFRESH_TOKEN] Refresh token is revoked - userId={}", token.getUser().getId());
            throw new RuntimeException("Refresh token was revoked");
        }

        if (token.isExpired()) {
            log.warn("[VERIFY_REFRESH_TOKEN] Refresh token is expired - userId={}, expiryDate={}",
                    token.getUser().getId(), token.getExpiryDate());
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new login request");
        }

        log.debug("[VERIFY_REFRESH_TOKEN] Refresh token is valid - userId={}", token.getUser().getId());
        return token;
    }

    @Override
    public void revokeToken(String token) {
        log.debug("[REVOKE_TOKEN] Revoking refresh token - token={}...", token.substring(0, 8));

        RefreshToken refreshToken = findByToken(token);
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        log.info("[REVOKE_TOKEN] Refresh token revoked - userId={}", refreshToken.getUser().getId());
    }

    @Override
    public void revokeAllUserTokens(User user) {
        log.debug("[REVOKE_ALL_TOKENS] Revoking all tokens for user - userId={}, username={}",
                user.getId(), user.getUsername());

        refreshTokenRepository.revokeAllUserTokens(user);

        log.info("[REVOKE_ALL_TOKENS] All tokens revoked - userId={}, username={}",
                user.getId(), user.getUsername());
    }

    @Override
    public void deleteExpiredTokens() {
        log.debug("[CLEANUP_TOKENS] Deleting expired refresh tokens");

        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());

        log.info("[CLEANUP_TOKENS] Expired tokens deleted");
    }
}
