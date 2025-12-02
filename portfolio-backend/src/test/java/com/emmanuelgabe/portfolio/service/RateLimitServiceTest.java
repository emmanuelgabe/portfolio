package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.service.impl.RateLimitServiceImpl;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitServiceImpl rateLimitService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitService, "maxRequestsPerHour", 5);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void should_allowRequest_when_firstRequest() {
        // Arrange
        String ip = "192.168.1.1";
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        boolean allowed = rateLimitService.isAllowed(ip);

        // Assert
        assertThat(allowed).isTrue();
        verify(valueOperations).increment("rate_limit:contact:192.168.1.1");
        verify(redisTemplate).expire(eq("rate_limit:contact:192.168.1.1"), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void should_allowRequest_when_belowLimit() {
        // Arrange
        String ip = "192.168.1.1";
        when(valueOperations.increment(anyString())).thenReturn(3L);

        // Act
        boolean allowed = rateLimitService.isAllowed(ip);

        // Assert
        assertThat(allowed).isTrue();
        verify(valueOperations).increment("rate_limit:contact:192.168.1.1");
    }

    @Test
    void should_allowRequest_when_exactlyAtLimit() {
        // Arrange
        String ip = "192.168.1.1";
        when(valueOperations.increment(anyString())).thenReturn(5L);

        // Act
        boolean allowed = rateLimitService.isAllowed(ip);

        // Assert
        assertThat(allowed).isTrue();
        verify(valueOperations).increment("rate_limit:contact:192.168.1.1");
    }

    @Test
    void should_denyRequest_when_exceedsLimit() {
        // Arrange
        String ip = "192.168.1.1";
        when(valueOperations.increment(anyString())).thenReturn(6L);

        // Act
        boolean allowed = rateLimitService.isAllowed(ip);

        // Assert
        assertThat(allowed).isFalse();
        verify(valueOperations).increment("rate_limit:contact:192.168.1.1");
    }

    @Test
    void should_returnCorrectRemainingAttempts_when_someRequestsMade() {
        // Arrange
        String ip = "192.168.1.1";
        when(valueOperations.get(anyString())).thenReturn("3");

        // Act
        long remaining = rateLimitService.getRemainingAttempts(ip);

        // Assert
        assertThat(remaining).isEqualTo(2L);
        verify(valueOperations).get("rate_limit:contact:192.168.1.1");
    }

    @Test
    void should_returnZeroRemainingAttempts_when_limitExceeded() {
        // Arrange
        String ip = "192.168.1.1";
        when(valueOperations.get(anyString())).thenReturn("6");

        // Act
        long remaining = rateLimitService.getRemainingAttempts(ip);

        // Assert
        assertThat(remaining).isEqualTo(0L);
        verify(valueOperations).get("rate_limit:contact:192.168.1.1");
    }

    @Test
    void should_returnMaxAttempts_when_noRequestsYet() {
        // Arrange
        String ip = "192.168.1.1";
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        long remaining = rateLimitService.getRemainingAttempts(ip);

        // Assert
        assertThat(remaining).isEqualTo(5L);
        verify(valueOperations).get("rate_limit:contact:192.168.1.1");
    }

    @Test
    void should_handleInvalidRedisValue_when_valueIsNotNumeric() {
        // Arrange
        String ip = "192.168.1.1";
        when(valueOperations.get(anyString())).thenReturn("invalid");

        // Act
        long remaining = rateLimitService.getRemainingAttempts(ip);

        // Assert
        assertThat(remaining).isEqualTo(5L);
        verify(valueOperations).get("rate_limit:contact:192.168.1.1");
    }
}
