package com.emmanuelgabe.portfolio.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Token Provider service
 * Handles JWT token generation, validation, and parsing
 */
@Slf4j
@Service
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final String issuer;
    private final String audience;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.issuer = issuer;
        this.audience = audience;
        log.info("[JWT_CONFIG] JwtTokenProvider initialized - accessExpiration={}ms, refreshExpiration={}ms",
                accessTokenExpiration, refreshTokenExpiration);
    }

    /**
     * Generate access token from user details
     * @param userDetails User details
     * @return JWT access token
     */
    public String generateAccessToken(UserDetails userDetails) {
        log.debug("[TOKEN_GENERATE] Generating access token - username={}", userDetails.getUsername());

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("authorities", userDetails.getAuthorities());

        String token = createToken(claims, userDetails.getUsername(), accessTokenExpiration);

        log.info("[TOKEN_GENERATE] Access token generated - username={}", userDetails.getUsername());
        return token;
    }

    /**
     * Generate refresh token from user details
     * @param userDetails User details
     * @return JWT refresh token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        log.debug("[TOKEN_GENERATE] Generating refresh token - username={}", userDetails.getUsername());

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        String token = createToken(claims, userDetails.getUsername(), refreshTokenExpiration);

        log.info("[TOKEN_GENERATE] Refresh token generated - username={}", userDetails.getUsername());
        return token;
    }

    /**
     * Create token with claims and subject
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract username from token
     * @param token JWT token
     * @return Username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from token
     * @param token JWT token
     * @return Expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if token is expired
     * @param token JWT token
     * @return true if expired
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate token against user details
     * @param token JWT token
     * @param userDetails User details
     * @return true if valid
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        log.debug("[TOKEN_VALIDATE] Validating token - username={}", userDetails.getUsername());

        try {
            final String username = extractUsername(token);
            boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);

            if (isValid) {
                log.debug("[TOKEN_VALIDATE] Token valid - username={}", username);
            } else {
                log.warn("[TOKEN_VALIDATE] Token invalid - username={}, expired={}", username, isTokenExpired(token));
            }

            return isValid;

        } catch (SignatureException ex) {
            log.error("[TOKEN_VALIDATE] Invalid JWT signature - error={}", ex.getMessage());
            throw ex;
        } catch (MalformedJwtException ex) {
            log.error("[TOKEN_VALIDATE] Invalid JWT token - error={}", ex.getMessage());
            throw ex;
        } catch (ExpiredJwtException ex) {
            log.warn("[TOKEN_VALIDATE] Expired JWT token - error={}", ex.getMessage());
            throw ex;
        } catch (UnsupportedJwtException ex) {
            log.error("[TOKEN_VALIDATE] Unsupported JWT token - error={}", ex.getMessage());
            throw ex;
        } catch (IllegalArgumentException ex) {
            log.error("[TOKEN_VALIDATE] JWT claims string is empty - error={}", ex.getMessage());
            throw ex;
        }
    }

    /**
     * Validate token without user details
     * @param token JWT token
     * @return true if token is structurally valid and not expired
     */
    public boolean validateTokenStructure(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("[TOKEN_VALIDATE] Invalid token structure - error={}", e.getMessage());
            return false;
        }
    }
}
