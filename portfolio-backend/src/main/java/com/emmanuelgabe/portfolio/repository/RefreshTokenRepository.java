package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.RefreshToken;
import com.emmanuelgabe.portfolio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for RefreshToken entity
 * Provides database operations for refresh token management
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find refresh token by token string
     * @param token Token string to search for
     * @return Optional containing refresh token if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all tokens for a specific user
     * @param user User to search tokens for
     * @return List of refresh tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false")
    java.util.List<RefreshToken> findActiveTokensByUser(@Param("user") User user);

    /**
     * Delete all tokens for a specific user
     * @param user User to delete tokens for
     */
    void deleteByUser(User user);

    /**
     * Delete all expired tokens
     * Cleanup method to remove old tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Revoke all tokens for a specific user
     * @param user User to revoke tokens for
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user")
    void revokeAllUserTokens(@Param("user") User user);
}
