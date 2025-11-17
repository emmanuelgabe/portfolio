package com.emmanuelgabe.portfolio.security;

import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.entity.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Initialize JwtTokenProvider with test configuration
        String secret = "mySecretKeyForJWTTokenGenerationAndValidationThatIsLongEnoughForHS256Algorithm";
        long accessTokenExpiration = 900000L; // 15 minutes
        long refreshTokenExpiration = 604800000L; // 7 days
        String issuer = "portfolio-backend";
        String audience = "portfolio-frontend";

        jwtTokenProvider = new JwtTokenProvider(
                secret,
                accessTokenExpiration,
                refreshTokenExpiration,
                issuer,
                audience
        );

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
    }

    @Test
    void generateAccessToken_WithValidUser_ReturnsToken() {
        // Act
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    void generateRefreshToken_WithValidUser_ReturnsToken() {
        // Act
        String token = jwtTokenProvider.generateRefreshToken(testUser);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractUsername_WithValidToken_ReturnsUsername() {
        // Arrange
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // Act
        String username = jwtTokenProvider.extractUsername(token);

        // Assert
        assertThat(username).isEqualTo("admin");
    }

    @Test
    void extractExpiration_WithValidToken_ReturnsExpirationDate() {
        // Arrange
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // Act
        Date expiration = jwtTokenProvider.extractExpiration(token);

        // Assert
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void isTokenExpired_WithValidToken_ReturnsFalse() {
        // Arrange
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // Act
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Assert
        assertThat(isExpired).isFalse();
    }

    @Test
    void isTokenExpired_WithExpiredToken_ThrowsException() {
        // Arrange - Create provider with negative expiration (already expired)
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(
                "mySecretKeyForJWTTokenGenerationAndValidationThatIsLongEnoughForHS256Algorithm",
                -1000L, // Negative expiration means already expired
                1L,
                "portfolio-backend",
                "portfolio-frontend"
        );
        String token = shortExpirationProvider.generateAccessToken(testUser);

        // Act & Assert - Expired tokens throw ExpiredJwtException when parsing
        assertThatThrownBy(() -> shortExpirationProvider.isTokenExpired(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void validateToken_WithValidToken_ReturnsTrue() {
        // Arrange
        String token = jwtTokenProvider.generateAccessToken(testUser);
        UserDetails userDetails = testUser;

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token, userDetails);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_WithWrongUsername_ReturnsFalse() {
        // Arrange
        String token = jwtTokenProvider.generateAccessToken(testUser);

        User differentUser = new User();
        differentUser.setUsername("differentUser");
        differentUser.setPassword("password");
        differentUser.setEmail("different@example.com");
        differentUser.setRole(UserRole.ROLE_ADMIN);
        differentUser.setEnabled(true);

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token, differentUser);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WithInvalidSignature_ThrowsException() {
        // Arrange
        String token = jwtTokenProvider.generateAccessToken(testUser);
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        // Act & Assert
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(tamperedToken, testUser))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void validateToken_WithMalformedToken_ThrowsException() {
        // Arrange
        String malformedToken = "not.a.valid.jwt.token";

        // Act & Assert
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(malformedToken, testUser))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void validateToken_WithExpiredToken_ThrowsException() {
        // Arrange - Create provider with very short expiration
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(
                "mySecretKeyForJWTTokenGenerationAndValidationThatIsLongEnoughForHS256Algorithm",
                1L, // 1 millisecond
                1L,
                "portfolio-backend",
                "portfolio-frontend"
        );
        String token = shortExpirationProvider.generateAccessToken(testUser);

        // Wait for token to expire
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act & Assert
        assertThatThrownBy(() -> shortExpirationProvider.validateToken(token, testUser))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void validateTokenStructure_WithValidToken_ReturnsTrue() {
        // Arrange
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // Act
        boolean isValid = jwtTokenProvider.validateTokenStructure(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void validateTokenStructure_WithInvalidToken_ReturnsFalse() {
        // Arrange
        String invalidToken = "invalid.token.structure";

        // Act
        boolean isValid = jwtTokenProvider.validateTokenStructure(invalidToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void validateTokenStructure_WithExpiredToken_ReturnsFalse() {
        // Arrange - Create provider with very short expiration
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(
                "mySecretKeyForJWTTokenGenerationAndValidationThatIsLongEnoughForHS256Algorithm",
                1L,
                1L,
                "portfolio-backend",
                "portfolio-frontend"
        );
        String token = shortExpirationProvider.generateAccessToken(testUser);

        // Wait for token to expire
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        boolean isValid = shortExpirationProvider.validateTokenStructure(token);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void accessTokenAndRefreshToken_AreDifferent() {
        // Act
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        // Assert
        assertThat(accessToken).isNotEqualTo(refreshToken);
    }

    @Test
    void extractClaim_WithValidToken_ExtractsCorrectClaim() {
        // Arrange
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // Act
        String subject = jwtTokenProvider.extractClaim(token, claims -> claims.getSubject());
        String issuer = jwtTokenProvider.extractClaim(token, claims -> claims.getIssuer());
        String audience = jwtTokenProvider.extractClaim(token, claims -> claims.getAudience().iterator().next());

        // Assert
        assertThat(subject).isEqualTo("admin");
        assertThat(issuer).isEqualTo("portfolio-backend");
        assertThat(audience).isEqualTo("portfolio-frontend");
    }
}
