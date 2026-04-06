package com.emmanuelgabe.portfolio.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisRateLimiter rateLimiter;

    private RateLimitPolicy policy;

    @BeforeEach
    void setUp() {
        policy = RateLimitPolicy.perHour("chat", 10);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ========== tryAcquire: Happy Path ==========

    @Test
    @SuppressWarnings("unchecked")
    void should_returnTrue_when_firstRequestWithinQuota() {
        // Arrange
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(1L);

        // Act
        boolean allowed = rateLimiter.tryAcquire(policy, "192.168.1.1");

        // Assert
        assertThat(allowed).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_returnTrue_when_requestWithinQuota() {
        // Arrange
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(5L);

        // Act
        boolean allowed = rateLimiter.tryAcquire(policy, "192.168.1.1");

        // Assert
        assertThat(allowed).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_returnTrue_when_requestEqualsQuotaLimit() {
        // Arrange
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(10L);

        // Act
        boolean allowed = rateLimiter.tryAcquire(policy, "192.168.1.1");

        // Assert
        assertThat(allowed).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_returnFalse_when_requestExceedsQuota() {
        // Arrange
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(11L);

        // Act
        boolean allowed = rateLimiter.tryAcquire(policy, "192.168.1.1");

        // Assert
        assertThat(allowed).isFalse();
    }

    // ========== tryAcquire: Key Format ==========

    @Test
    @SuppressWarnings("unchecked")
    void should_buildKeyWithBucketNamespace_when_tryAcquireCalled() {
        // Arrange
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(1L);

        // Act
        rateLimiter.tryAcquire(policy, "192.168.1.1");

        // Assert
        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of("rate_limit:chat:192.168.1.1")),
                eq("3600"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_isolateDifferentBuckets_when_keyBuilt() {
        // Arrange
        RateLimitPolicy contactPolicy = RateLimitPolicy.perHour("contact", 5);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(1L);

        // Act
        rateLimiter.tryAcquire(contactPolicy, "192.168.1.1");

        // Assert
        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of("rate_limit:contact:192.168.1.1")),
                eq("3600"));
    }

    // ========== tryAcquire: Fail-Open ==========

    @Test
    @SuppressWarnings("unchecked")
    void should_returnTrue_when_redisThrowsException() {
        // Arrange
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new RuntimeException("Redis down"));

        // Act
        boolean allowed = rateLimiter.tryAcquire(policy, "192.168.1.1");

        // Assert
        assertThat(allowed).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_returnTrue_when_scriptReturnsNull() {
        // Arrange
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(null);

        // Act
        boolean allowed = rateLimiter.tryAcquire(policy, "192.168.1.1");

        // Assert
        assertThat(allowed).isTrue();
    }

    // ========== remainingQuota ==========

    @Test
    void should_returnFullQuota_when_noRequestsYet() {
        // Arrange
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        long remaining = rateLimiter.remainingQuota(policy, "192.168.1.1");

        // Assert
        assertThat(remaining).isEqualTo(10L);
    }

    @Test
    void should_returnRemaining_when_someRequestsMade() {
        // Arrange
        when(valueOperations.get(anyString())).thenReturn("3");

        // Act
        long remaining = rateLimiter.remainingQuota(policy, "192.168.1.1");

        // Assert
        assertThat(remaining).isEqualTo(7L);
    }

    @Test
    void should_returnZero_when_quotaExhausted() {
        // Arrange
        when(valueOperations.get(anyString())).thenReturn("10");

        // Act
        long remaining = rateLimiter.remainingQuota(policy, "192.168.1.1");

        // Assert
        assertThat(remaining).isZero();
    }

    @Test
    void should_returnZero_when_counterExceedsQuota() {
        // Arrange
        when(valueOperations.get(anyString())).thenReturn("15");

        // Act
        long remaining = rateLimiter.remainingQuota(policy, "192.168.1.1");

        // Assert
        assertThat(remaining).isZero();
    }

    @Test
    void should_returnFullQuota_when_counterValueInvalid() {
        // Arrange
        when(valueOperations.get(anyString())).thenReturn("not-a-number");

        // Act
        long remaining = rateLimiter.remainingQuota(policy, "192.168.1.1");

        // Assert
        assertThat(remaining).isEqualTo(10L);
    }

    @Test
    void should_returnFullQuota_when_redisThrowsExceptionOnGet() {
        // Arrange
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis down"));

        // Act
        long remaining = rateLimiter.remainingQuota(policy, "192.168.1.1");

        // Assert -- fail-open on read: assume quota available
        assertThat(remaining).isEqualTo(10L);
    }

    @Test
    void should_useNamespacedKey_when_remainingQuotaCalled() {
        // Arrange
        when(valueOperations.get(anyString())).thenReturn("3");

        // Act
        rateLimiter.remainingQuota(policy, "192.168.1.1");

        // Assert
        verify(valueOperations).get(eq("rate_limit:chat:192.168.1.1"));
    }
}
