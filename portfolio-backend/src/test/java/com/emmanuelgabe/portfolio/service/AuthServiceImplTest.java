package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.AuthResponse;
import com.emmanuelgabe.portfolio.dto.ChangePasswordRequest;
import com.emmanuelgabe.portfolio.dto.LoginRequest;
import com.emmanuelgabe.portfolio.entity.RefreshToken;
import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.entity.UserRole;
import com.emmanuelgabe.portfolio.exception.InvalidCredentialsException;
import com.emmanuelgabe.portfolio.exception.InvalidTokenException;
import com.emmanuelgabe.portfolio.mapper.UserMapper;
import com.emmanuelgabe.portfolio.repository.UserRepository;
import com.emmanuelgabe.portfolio.security.JwtTokenProvider;
import com.emmanuelgabe.portfolio.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RefreshToken testRefreshToken;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;
    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    void setUp() {
        // Set access token expiration via reflection
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 900000L);

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

        // Setup test refresh token
        testRefreshToken = new RefreshToken();
        testRefreshToken.setId(1L);
        testRefreshToken.setToken("refresh-token-uuid");
        testRefreshToken.setUser(testUser);
        testRefreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        testRefreshToken.setRevoked(false);
        testRefreshToken.setCreatedAt(LocalDateTime.now());

        // Setup login request
        loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password123");

        // Setup auth response
        authResponse = new AuthResponse();
        authResponse.setUsername("admin");
        authResponse.setEmail("admin@example.com");
        authResponse.setRole(UserRole.ROLE_ADMIN.name());

        // Setup change password request
        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("OldPassword123!");
        changePasswordRequest.setNewPassword("NewPassword123!");
        changePasswordRequest.setConfirmPassword("NewPassword123!");
    }

    @Test
    void should_returnAuthResponse_when_loginCalledWithValidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(testUser))
                .thenReturn("access-token-jwt");
        when(refreshTokenService.createRefreshToken(testUser))
                .thenReturn(testRefreshToken);
        when(userMapper.toAuthResponse(testUser))
                .thenReturn(authResponse);

        // Act
        AuthResponse result = authService.login(loginRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token-jwt");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token-uuid");
        assertThat(result.getExpiresIn()).isEqualTo(900000L);

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, times(1)).generateAccessToken(testUser);
        verify(refreshTokenService, times(1)).createRefreshToken(testUser);
    }

    @Test
    void should_throwInvalidCredentialsException_when_loginCalledWithInvalidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void should_returnAuthResponse_when_refreshTokenCalledWithValidToken() {
        // Arrange
        when(refreshTokenService.findByToken("refresh-token-uuid"))
                .thenReturn(testRefreshToken);
        when(refreshTokenService.verifyExpiration(testRefreshToken))
                .thenReturn(testRefreshToken);
        when(jwtTokenProvider.generateAccessToken(testUser))
                .thenReturn("new-access-token-jwt");
        when(userMapper.toAuthResponse(testUser))
                .thenReturn(authResponse);

        // Act
        AuthResponse result = authService.refreshToken("refresh-token-uuid");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("new-access-token-jwt");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token-uuid");
        assertThat(result.getExpiresIn()).isEqualTo(900000L);

        verify(refreshTokenService, times(1)).findByToken("refresh-token-uuid");
        verify(refreshTokenService, times(1)).verifyExpiration(testRefreshToken);
        verify(jwtTokenProvider, times(1)).generateAccessToken(testUser);
    }

    @Test
    void should_throwInvalidTokenException_when_refreshTokenCalledWithInvalidToken() {
        // Arrange
        when(refreshTokenService.findByToken("invalid-token"))
                .thenThrow(new RuntimeException("Refresh token not found"));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken("invalid-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired refresh token");

        verify(refreshTokenService, times(1)).findByToken("invalid-token");
    }

    @Test
    void should_throwInvalidTokenException_when_refreshTokenCalledWithExpiredToken() {
        // Arrange
        when(refreshTokenService.findByToken("expired-token"))
                .thenReturn(testRefreshToken);
        when(refreshTokenService.verifyExpiration(testRefreshToken))
                .thenThrow(new RuntimeException("Refresh token was expired"));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken("expired-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired refresh token");

        verify(refreshTokenService, times(1)).findByToken("expired-token");
        verify(refreshTokenService, times(1)).verifyExpiration(testRefreshToken);
    }

    @Test
    void should_revokeToken_when_logoutCalledWithValidToken() {
        // Arrange - no exceptions thrown means success

        // Act
        authService.logout("refresh-token-uuid");

        // Assert
        verify(refreshTokenService, times(1)).revokeToken("refresh-token-uuid");
    }

    @Test
    void should_throwInvalidTokenException_when_logoutCalledWithInvalidToken() {
        // Arrange
        doThrow(new RuntimeException("Refresh token not found"))
                .when(refreshTokenService).revokeToken("invalid-token");

        // Act & Assert
        assertThatThrownBy(() -> authService.logout("invalid-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid refresh token");

        verify(refreshTokenService, times(1)).revokeToken("invalid-token");
    }

    @Test
    void should_changePassword_when_changePasswordCalledWithValidRequest() {
        // Arrange
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPassword123!", testUser.getPassword()))
                .thenReturn(true);
        when(passwordEncoder.encode("NewPassword123!"))
                .thenReturn("$2a$10$newEncodedPassword");
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        // Act
        authService.changePassword("admin", changePasswordRequest);

        // Assert
        verify(userRepository, times(1)).findByUsername("admin");
        verify(passwordEncoder, times(1)).matches("OldPassword123!", "$2a$10$encodedPassword");
        verify(passwordEncoder, times(1)).encode("NewPassword123!");
        verify(userRepository, times(1)).save(testUser);
        verify(refreshTokenService, times(1)).revokeAllUserTokens(testUser);
    }

    @Test
    void should_throwIllegalArgumentException_when_changePasswordCalledWithMismatchedPasswords() {
        // Arrange
        changePasswordRequest.setConfirmPassword("differentPassword");

        // Act & Assert
        assertThatThrownBy(() -> authService.changePassword("admin", changePasswordRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("New password and confirmation do not match");
    }

    @Test
    void should_throwUsernameNotFoundException_when_changePasswordCalledWithNonExistentUser() {
        // Arrange
        when(userRepository.findByUsername("nonexistent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.changePassword("nonexistent", changePasswordRequest))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    void should_throwInvalidCredentialsException_when_changePasswordCalledWithIncorrectCurrentPassword() {
        // Arrange
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPassword123!", testUser.getPassword()))
                .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.changePassword("admin", changePasswordRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Current password is incorrect");

        verify(userRepository, times(1)).findByUsername("admin");
        verify(passwordEncoder, times(1)).matches("OldPassword123!", testUser.getPassword());
    }

    @Test
    void should_throwIllegalArgumentException_when_changePasswordCalledWithSamePassword() {
        // Arrange
        changePasswordRequest.setNewPassword("OldPassword123!");
        changePasswordRequest.setConfirmPassword("OldPassword123!");

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPassword123!", testUser.getPassword()))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.changePassword("admin", changePasswordRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("New password must be different from current password");

        verify(userRepository, times(1)).findByUsername("admin");
        verify(passwordEncoder, times(1)).matches("OldPassword123!", testUser.getPassword());
    }
}
