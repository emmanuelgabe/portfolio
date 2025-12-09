package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.service.impl.UploadRateLimitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadRateLimitServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private UploadRateLimitServiceImpl uploadRateLimitService;

    private static final String TEST_USERNAME = "admin";
    private static final int MAX_UPLOADS_PER_HOUR = 30;

    @BeforeEach
    void setUp() {
        uploadRateLimitService = new UploadRateLimitServiceImpl(redisTemplate);
        ReflectionTestUtils.setField(uploadRateLimitService, "maxUploadsPerHour", MAX_UPLOADS_PER_HOUR);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ========== isUploadAllowed Tests ==========

    @Test
    void should_returnTrue_when_isUploadAllowedCalledWithFirstRequest() {
        // Arrange
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        boolean result = uploadRateLimitService.isUploadAllowed(TEST_USERNAME);

        // Assert
        assertThat(result).isTrue();
        verify(redisTemplate).expire(eq("rate_limit:upload:" + TEST_USERNAME), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void should_returnTrue_when_isUploadAllowedCalledWithinLimit() {
        // Arrange
        when(valueOperations.increment(anyString())).thenReturn(15L);

        // Act
        boolean result = uploadRateLimitService.isUploadAllowed(TEST_USERNAME);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void should_returnTrue_when_isUploadAllowedCalledAtExactLimit() {
        // Arrange
        when(valueOperations.increment(anyString())).thenReturn((long) MAX_UPLOADS_PER_HOUR);

        // Act
        boolean result = uploadRateLimitService.isUploadAllowed(TEST_USERNAME);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void should_returnFalse_when_isUploadAllowedCalledExceedingLimit() {
        // Arrange
        when(valueOperations.increment(anyString())).thenReturn((long) MAX_UPLOADS_PER_HOUR + 1);

        // Act
        boolean result = uploadRateLimitService.isUploadAllowed(TEST_USERNAME);

        // Assert
        assertThat(result).isFalse();
    }

    // ========== getRemainingUploads Tests ==========

    @Test
    void should_returnMaxUploads_when_getRemainingUploadsCalledWithNoRequests() {
        // Arrange
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        long remaining = uploadRateLimitService.getRemainingUploads(TEST_USERNAME);

        // Assert
        assertThat(remaining).isEqualTo(MAX_UPLOADS_PER_HOUR);
    }

    @Test
    void should_returnCorrectRemaining_when_getRemainingUploadsCalledWithSomeRequests() {
        // Arrange
        when(valueOperations.get(anyString())).thenReturn("10");

        // Act
        long remaining = uploadRateLimitService.getRemainingUploads(TEST_USERNAME);

        // Assert
        assertThat(remaining).isEqualTo(20);
    }

    @Test
    void should_returnZero_when_getRemainingUploadsCalledAtLimit() {
        // Arrange
        when(valueOperations.get(anyString())).thenReturn(String.valueOf(MAX_UPLOADS_PER_HOUR));

        // Act
        long remaining = uploadRateLimitService.getRemainingUploads(TEST_USERNAME);

        // Assert
        assertThat(remaining).isEqualTo(0);
    }

    @Test
    void should_returnZero_when_getRemainingUploadsCalledExceedingLimit() {
        // Arrange
        when(valueOperations.get(anyString())).thenReturn(String.valueOf(MAX_UPLOADS_PER_HOUR + 10));

        // Act
        long remaining = uploadRateLimitService.getRemainingUploads(TEST_USERNAME);

        // Assert
        assertThat(remaining).isEqualTo(0);
    }

    // ========== getMaxUploadsPerHour Tests ==========

    @Test
    void should_returnConfiguredValue_when_getMaxUploadsPerHourCalled() {
        // Act
        int maxUploads = uploadRateLimitService.getMaxUploadsPerHour();

        // Assert
        assertThat(maxUploads).isEqualTo(MAX_UPLOADS_PER_HOUR);
    }
}
