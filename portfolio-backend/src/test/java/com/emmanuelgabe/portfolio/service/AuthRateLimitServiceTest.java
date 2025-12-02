package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.service.impl.AuthRateLimitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRateLimitServiceTest {

    private static final String TEST_IP = "192.168.1.100";
    private static final String LOGIN_KEY_PREFIX = "rate_limit:auth:login:";
    private static final String REFRESH_KEY_PREFIX = "rate_limit:auth:refresh:";
    private static final int MAX_LOGIN_REQUESTS = 5;
    private static final int MAX_REFRESH_REQUESTS = 10;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthRateLimitServiceImpl authRateLimitService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authRateLimitService, "maxLoginRequestsPerHour", MAX_LOGIN_REQUESTS);
        ReflectionTestUtils.setField(authRateLimitService, "maxRefreshRequestsPerHour", MAX_REFRESH_REQUESTS);
    }

    // ========== Login Rate Limiting Tests ==========

    @Test
    void should_allowLogin_when_firstRequest() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        boolean allowed = authRateLimitService.isLoginAllowed(TEST_IP);

        // Assert
        assertThat(allowed).isTrue();
        verify(valueOperations).increment(LOGIN_KEY_PREFIX + TEST_IP);
        verify(redisTemplate).expire(eq(LOGIN_KEY_PREFIX + TEST_IP), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void should_allowLogin_when_belowLimit() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(3L);

        // Act
        boolean allowed = authRateLimitService.isLoginAllowed(TEST_IP);

        // Assert
        assertThat(allowed).isTrue();
        verify(valueOperations).increment(LOGIN_KEY_PREFIX + TEST_IP);
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void should_allowLogin_when_exactlyAtLimit() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn((long) MAX_LOGIN_REQUESTS);

        // Act
        boolean allowed = authRateLimitService.isLoginAllowed(TEST_IP);

        // Assert
        assertThat(allowed).isTrue();
    }

    @Test
    void should_denyLogin_when_exceedsLimit() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn((long) MAX_LOGIN_REQUESTS + 1);

        // Act
        boolean allowed = authRateLimitService.isLoginAllowed(TEST_IP);

        // Assert
        assertThat(allowed).isFalse();
    }

    @Test
    void should_returnCorrectRemainingLoginAttempts_when_someRequestsMade() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_KEY_PREFIX + TEST_IP)).thenReturn("3");

        // Act
        long remaining = authRateLimitService.getRemainingLoginAttempts(TEST_IP);

        // Assert
        assertThat(remaining).isEqualTo(2L);
    }

    @Test
    void should_returnMaxAttempts_when_noLoginRequestsYet() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_KEY_PREFIX + TEST_IP)).thenReturn(null);

        // Act
        long remaining = authRateLimitService.getRemainingLoginAttempts(TEST_IP);

        // Assert
        assertThat(remaining).isEqualTo(MAX_LOGIN_REQUESTS);
    }

    @Test
    void should_returnZeroRemainingLoginAttempts_when_limitExceeded() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_KEY_PREFIX + TEST_IP)).thenReturn("10");

        // Act
        long remaining = authRateLimitService.getRemainingLoginAttempts(TEST_IP);

        // Assert
        assertThat(remaining).isEqualTo(0L);
    }

    // ========== Refresh Rate Limiting Tests ==========

    @Test
    void should_allowRefresh_when_firstRequest() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        boolean allowed = authRateLimitService.isRefreshAllowed(TEST_IP);

        // Assert
        assertThat(allowed).isTrue();
        verify(valueOperations).increment(REFRESH_KEY_PREFIX + TEST_IP);
        verify(redisTemplate).expire(eq(REFRESH_KEY_PREFIX + TEST_IP), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void should_allowRefresh_when_belowLimit() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(5L);

        // Act
        boolean allowed = authRateLimitService.isRefreshAllowed(TEST_IP);

        // Assert
        assertThat(allowed).isTrue();
    }

    @Test
    void should_allowRefresh_when_exactlyAtLimit() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn((long) MAX_REFRESH_REQUESTS);

        // Act
        boolean allowed = authRateLimitService.isRefreshAllowed(TEST_IP);

        // Assert
        assertThat(allowed).isTrue();
    }

    @Test
    void should_denyRefresh_when_exceedsLimit() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn((long) MAX_REFRESH_REQUESTS + 1);

        // Act
        boolean allowed = authRateLimitService.isRefreshAllowed(TEST_IP);

        // Assert
        assertThat(allowed).isFalse();
    }

    @Test
    void should_returnCorrectRemainingRefreshAttempts_when_someRequestsMade() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REFRESH_KEY_PREFIX + TEST_IP)).thenReturn("7");

        // Act
        long remaining = authRateLimitService.getRemainingRefreshAttempts(TEST_IP);

        // Assert
        assertThat(remaining).isEqualTo(3L);
    }

    @Test
    void should_returnMaxAttempts_when_noRefreshRequestsYet() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REFRESH_KEY_PREFIX + TEST_IP)).thenReturn(null);

        // Act
        long remaining = authRateLimitService.getRemainingRefreshAttempts(TEST_IP);

        // Assert
        assertThat(remaining).isEqualTo(MAX_REFRESH_REQUESTS);
    }

    // ========== Configuration Getters Tests ==========

    @Test
    void should_returnMaxLoginRequestsPerHour_when_called() {
        // Act
        int maxRequests = authRateLimitService.getMaxLoginRequestsPerHour();

        // Assert
        assertThat(maxRequests).isEqualTo(MAX_LOGIN_REQUESTS);
    }

    @Test
    void should_returnMaxRefreshRequestsPerHour_when_called() {
        // Act
        int maxRequests = authRateLimitService.getMaxRefreshRequestsPerHour();

        // Assert
        assertThat(maxRequests).isEqualTo(MAX_REFRESH_REQUESTS);
    }

    // ========== Edge Cases Tests ==========

    @Test
    void should_allowLogin_when_incrementReturnsNull() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(null);

        // Act
        boolean allowed = authRateLimitService.isLoginAllowed(TEST_IP);

        // Assert
        assertThat(allowed).isTrue();
    }

    @Test
    void should_returnMaxAttempts_when_redisValueIsInvalid() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_KEY_PREFIX + TEST_IP)).thenReturn("invalid");

        // Act
        long remaining = authRateLimitService.getRemainingLoginAttempts(TEST_IP);

        // Assert
        assertThat(remaining).isEqualTo(MAX_LOGIN_REQUESTS);
    }

    @Test
    void should_allowLogin_when_expireFailsGracefully() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        // Act
        boolean allowed = authRateLimitService.isLoginAllowed(TEST_IP);

        // Assert
        assertThat(allowed).isTrue();
    }
}
