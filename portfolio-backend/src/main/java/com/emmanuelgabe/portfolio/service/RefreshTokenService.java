package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.entity.RefreshToken;
import com.emmanuelgabe.portfolio.entity.User;

/**
 * Service interface for Refresh Token operations
 * Provides business logic for token refresh mechanism
 */
public interface RefreshTokenService {

    /**
     * Create a new refresh token for a user
     * @param user User to create token for
     * @return Created refresh token
     */
    RefreshToken createRefreshToken(User user);

    /**
     * Find refresh token by token string
     * @param token Token string to search for
     * @return Refresh token if found
     */
    RefreshToken findByToken(String token);

    /**
     * Verify if refresh token is valid
     * @param token Refresh token to verify
     * @return Verified token if valid
     * @throws RuntimeException if token is expired or revoked
     */
    RefreshToken verifyExpiration(RefreshToken token);

    /**
     * Revoke a specific refresh token
     * @param token Token to revoke
     */
    void revokeToken(String token);

    /**
     * Revoke all refresh tokens for a user
     * @param user User to revoke tokens for
     */
    void revokeAllUserTokens(User user);

    /**
     * Delete all expired tokens (cleanup)
     */
    void deleteExpiredTokens();
}
