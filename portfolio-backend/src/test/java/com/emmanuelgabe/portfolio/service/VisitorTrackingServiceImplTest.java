package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.service.impl.VisitorTrackingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorTrackingServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private VisitorTrackingServiceImpl visitorTrackingService;

    // ========== registerHeartbeat Tests ==========

    @Test
    void should_storeSessionInRedis_when_registerHeartbeatCalled() {
        // Arrange
        String sessionId = "test-session-123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        // Act
        visitorTrackingService.registerHeartbeat(sessionId);

        // Assert
        verify(valueOperations).set(eq("visitor:test-session-123"), anyString(), eq(Duration.ofSeconds(60)));
        verify(setOperations).add(anyString(), eq(sessionId));
    }

    @Test
    void should_storeUuidSession_when_registerHeartbeatCalledWithUuid() {
        // Arrange
        String sessionId = "550e8400-e29b-41d4-a716-446655440000";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        // Act
        visitorTrackingService.registerHeartbeat(sessionId);

        // Assert
        verify(valueOperations).set(eq("visitor:550e8400-e29b-41d4-a716-446655440000"), anyString(), eq(Duration.ofSeconds(60)));
        verify(setOperations).add(anyString(), eq(sessionId));
    }

    // ========== getActiveVisitorsCount Tests ==========

    @Test
    void should_returnZero_when_noActiveVisitors() {
        // Arrange
        when(redisTemplate.keys("visitor:*")).thenReturn(new HashSet<>());

        // Act
        int count = visitorTrackingService.getActiveVisitorsCount();

        // Assert
        assertThat(count).isEqualTo(0);
        verify(redisTemplate).keys("visitor:*");
    }

    @Test
    void should_returnCorrectCount_when_multipleActiveVisitors() {
        // Arrange
        Set<String> keys = new HashSet<>();
        keys.add("visitor:session1");
        keys.add("visitor:session2");
        keys.add("visitor:session3");
        when(redisTemplate.keys("visitor:*")).thenReturn(keys);

        // Act
        int count = visitorTrackingService.getActiveVisitorsCount();

        // Assert
        assertThat(count).isEqualTo(3);
        verify(redisTemplate).keys("visitor:*");
    }

    @Test
    void should_returnZero_when_keysReturnsNull() {
        // Arrange
        when(redisTemplate.keys("visitor:*")).thenReturn(null);

        // Act
        int count = visitorTrackingService.getActiveVisitorsCount();

        // Assert
        assertThat(count).isEqualTo(0);
        verify(redisTemplate).keys("visitor:*");
    }

    @Test
    void should_returnOne_when_singleActiveVisitor() {
        // Arrange
        Set<String> keys = new HashSet<>();
        keys.add("visitor:single-session");
        when(redisTemplate.keys("visitor:*")).thenReturn(keys);

        // Act
        int count = visitorTrackingService.getActiveVisitorsCount();

        // Assert
        assertThat(count).isEqualTo(1);
    }
}
