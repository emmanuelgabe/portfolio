package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.entity.RefreshToken;
import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.entity.UserRole;
import com.emmanuelgabe.portfolio.repository.RefreshTokenRepository;
import com.emmanuelgabe.portfolio.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        // Set refresh token expiration via reflection (7 days in milliseconds)
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 604800000L);

        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setEmail("admin@example.com");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setRole(UserRole.ROLE_ADMIN);
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // Setup test refresh token
        testRefreshToken = new RefreshToken();
        testRefreshToken.setId(1L);
        testRefreshToken.setToken("refresh-token-uuid");
        testRefreshToken.setUser(testUser);
        testRefreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        testRefreshToken.setRevoked(false);
        testRefreshToken.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createRefreshToken_WithValidUser_CreatesToken() {
        // Arrange
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(testRefreshToken);

        // Act
        RefreshToken result = refreshTokenService.createRefreshToken(testUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.isRevoked()).isFalse();

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(1)).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(testUser);
        assertThat(savedToken.getToken()).isNotNull();
        assertThat(savedToken.isRevoked()).isFalse();
        assertThat(savedToken.getExpiryDate()).isAfter(LocalDateTime.now());
    }

    @Test
    void findByToken_WithExistingToken_ReturnsToken() {
        // Arrange
        when(refreshTokenRepository.findByToken("refresh-token-uuid"))
                .thenReturn(Optional.of(testRefreshToken));

        // Act
        RefreshToken result = refreshTokenService.findByToken("refresh-token-uuid");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("refresh-token-uuid");
        assertThat(result.getUser()).isEqualTo(testUser);

        verify(refreshTokenRepository, times(1)).findByToken("refresh-token-uuid");
    }

    @Test
    void findByToken_WithNonExistentToken_ThrowsException() {
        // Arrange
        when(refreshTokenRepository.findByToken("non-existent-token"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.findByToken("non-existent-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Refresh token not found");

        verify(refreshTokenRepository, times(1)).findByToken("non-existent-token");
    }

    @Test
    void verifyExpiration_WithValidToken_ReturnsToken() {
        // Arrange
        testRefreshToken.setExpiryDate(LocalDateTime.now().plusDays(1));
        testRefreshToken.setRevoked(false);

        // Act
        RefreshToken result = refreshTokenService.verifyExpiration(testRefreshToken);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testRefreshToken);
    }

    @Test
    void verifyExpiration_WithExpiredToken_ThrowsException() {
        // Arrange
        testRefreshToken.setExpiryDate(LocalDateTime.now().minusDays(1));
        testRefreshToken.setRevoked(false);

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(testRefreshToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Refresh token was expired. Please make a new login request");

        verify(refreshTokenRepository, times(1)).delete(testRefreshToken);
    }

    @Test
    void verifyExpiration_WithRevokedToken_ThrowsException() {
        // Arrange
        testRefreshToken.setExpiryDate(LocalDateTime.now().plusDays(1));
        testRefreshToken.setRevoked(true);

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(testRefreshToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Refresh token was revoked");
    }

    @Test
    void revokeToken_WithValidToken_RevokesToken() {
        // Arrange
        when(refreshTokenRepository.findByToken("refresh-token-uuid"))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(testRefreshToken);

        // Act
        refreshTokenService.revokeToken("refresh-token-uuid");

        // Assert
        verify(refreshTokenRepository, times(1)).findByToken("refresh-token-uuid");
        verify(refreshTokenRepository, times(1)).save(testRefreshToken);
        assertThat(testRefreshToken.isRevoked()).isTrue();
    }

    @Test
    void revokeToken_WithNonExistentToken_ThrowsException() {
        // Arrange
        when(refreshTokenRepository.findByToken("non-existent-token"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.revokeToken("non-existent-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Refresh token not found");

        verify(refreshTokenRepository, times(1)).findByToken("non-existent-token");
    }

    @Test
    void revokeAllUserTokens_WithValidUser_RevokesAllTokens() {
        // Act
        refreshTokenService.revokeAllUserTokens(testUser);

        // Assert
        verify(refreshTokenRepository, times(1)).revokeAllUserTokens(testUser);
    }

    @Test
    void deleteExpiredTokens_DeletesExpiredTokens() {
        // Act
        refreshTokenService.deleteExpiredTokens();

        // Assert
        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(refreshTokenRepository, times(1)).deleteExpiredTokens(dateCaptor.capture());

        LocalDateTime capturedDate = dateCaptor.getValue();
        assertThat(capturedDate).isBefore(LocalDateTime.now().plusSeconds(1));
        assertThat(capturedDate).isAfter(LocalDateTime.now().minusSeconds(1));
    }
}
