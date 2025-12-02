package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.AuthResponse;
import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private UserMapper userMapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Get mapper instance from MapStruct factory
        userMapper = Mappers.getMapper(UserMapper.class);

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
    void should_mapCorrectly_when_toAuthResponseCalledWithValidUser() {
        // Act
        AuthResponse result = userMapper.toAuthResponse(testUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getEmail()).isEqualTo("admin@example.com");
        assertThat(result.getRole()).isEqualTo(UserRole.ROLE_ADMIN.name());

        // Verify ignored fields are null
        assertThat(result.getAccessToken()).isNull();
        assertThat(result.getRefreshToken()).isNull();
        assertThat(result.getTokenType()).isNotNull(); // Has default value "Bearer"
        assertThat(result.getExpiresIn()).isNull();
    }

    @Test
    void should_mapCorrectly_when_toAuthResponseCalledWithGuestUser() {
        // Arrange
        testUser.setRole(UserRole.ROLE_GUEST);

        // Act
        AuthResponse result = userMapper.toAuthResponse(testUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getEmail()).isEqualTo("admin@example.com");
        assertThat(result.getRole()).isEqualTo(UserRole.ROLE_GUEST.name());
    }

    @Test
    void should_returnNull_when_toAuthResponseCalledWithNullUser() {
        // Act
        AuthResponse result = userMapper.toAuthResponse(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void should_mapCorrectly_when_toAuthResponseCalledWithDifferentUserData() {
        // Arrange
        User differentUser = new User();
        differentUser.setId(2L);
        differentUser.setUsername("guest");
        differentUser.setEmail("guest@example.com");
        differentUser.setPassword("password");
        differentUser.setRole(UserRole.ROLE_GUEST);

        // Act
        AuthResponse result = userMapper.toAuthResponse(differentUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("guest");
        assertThat(result.getEmail()).isEqualTo("guest@example.com");
        assertThat(result.getRole()).isEqualTo(UserRole.ROLE_GUEST.name());
    }

    @Test
    void should_notIncludePassword_when_toAuthResponseCalled() {
        // Act
        AuthResponse result = userMapper.toAuthResponse(testUser);

        // Assert
        assertThat(result).isNotNull();
        // Verify that there's no password field in AuthResponse
        // This is a security check to ensure password is never exposed
        assertThat(result.toString()).doesNotContain("password");
        assertThat(result.toString()).doesNotContain("$2a$10$encodedPassword");
    }
}
