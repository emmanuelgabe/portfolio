package com.emmanuelgabe.portfolio.security;

import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.entity.UserRole;
import com.emmanuelgabe.portfolio.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setEmail("admin@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setRole(UserRole.ROLE_ADMIN);
        testUser.setEnabled(true);
        testUser.setAccountNonExpired(true);
        testUser.setAccountNonLocked(true);
        testUser.setCredentialsNonExpired(true);
    }

    // ========== loadUserByUsername Tests ==========

    @Test
    void should_returnUserDetails_when_loadUserByUsernameCalledWithExistingUser() {
        // Arrange
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = customUserDetailsService.loadUserByUsername("admin");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getPassword()).isEqualTo("hashedPassword");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isAccountNonExpired()).isTrue();
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.isCredentialsNonExpired()).isTrue();
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_ADMIN");

        verify(userRepository).findByUsername("admin");
    }

    @Test
    void should_throwUsernameNotFoundException_when_loadUserByUsernameCalledWithNonExistingUser() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with username: unknown");

        verify(userRepository).findByUsername("unknown");
    }

    @Test
    void should_returnCorrectAuthorities_when_loadUserByUsernameCalledWithGuestUser() {
        // Arrange
        testUser.setRole(UserRole.ROLE_GUEST);
        when(userRepository.findByUsername("guest")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = customUserDetailsService.loadUserByUsername("guest");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_GUEST");

        verify(userRepository).findByUsername("guest");
    }

    @Test
    void should_returnDisabledUser_when_loadUserByUsernameCalledWithDisabledUser() {
        // Arrange
        testUser.setEnabled(false);
        when(userRepository.findByUsername("disabled")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = customUserDetailsService.loadUserByUsername("disabled");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isEnabled()).isFalse();

        verify(userRepository).findByUsername("disabled");
    }

    @Test
    void should_returnLockedUser_when_loadUserByUsernameCalledWithLockedUser() {
        // Arrange
        testUser.setAccountNonLocked(false);
        when(userRepository.findByUsername("locked")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = customUserDetailsService.loadUserByUsername("locked");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isAccountNonLocked()).isFalse();

        verify(userRepository).findByUsername("locked");
    }
}
