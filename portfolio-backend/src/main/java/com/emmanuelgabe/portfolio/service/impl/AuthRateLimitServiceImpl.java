package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.service.AuthRateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis-based rate limiting implementation for authentication endpoints.
 * Uses fixed window strategy with 1-hour TTL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRateLimitServiceImpl implements AuthRateLimitService {

    private static final String LOGIN_KEY_PREFIX = "rate_limit:auth:login:";
    private static final String REFRESH_KEY_PREFIX = "rate_limit:auth:refresh:";
    private static final long WINDOW_SIZE_HOURS = 1;

    @Value("${app.rate-limit.auth.login.max-requests-per-hour:5}")
    private int maxLoginRequestsPerHour;

    @Value("${app.rate-limit.auth.refresh.max-requests-per-hour:10}")
    private int maxRefreshRequestsPerHour;

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean isLoginAllowed(String ip) {
        return isAllowed(ip, LOGIN_KEY_PREFIX, maxLoginRequestsPerHour, "login");
    }

    @Override
    public boolean isRefreshAllowed(String ip) {
        return isAllowed(ip, REFRESH_KEY_PREFIX, maxRefreshRequestsPerHour, "refresh");
    }

    @Override
    public long getRemainingLoginAttempts(String ip) {
        return getRemainingAttempts(ip, LOGIN_KEY_PREFIX, maxLoginRequestsPerHour);
    }

    @Override
    public long getRemainingRefreshAttempts(String ip) {
        return getRemainingAttempts(ip, REFRESH_KEY_PREFIX, maxRefreshRequestsPerHour);
    }

    @Override
    public int getMaxLoginRequestsPerHour() {
        return maxLoginRequestsPerHour;
    }

    @Override
    public int getMaxRefreshRequestsPerHour() {
        return maxRefreshRequestsPerHour;
    }

    private boolean isAllowed(String ip, String keyPrefix, int maxRequests, String endpoint) {
        String key = keyPrefix + ip;

        // Increment the counter atomically
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            count = 0L;
        }

        // Set expiration on first request (atomic operation to prevent race condition)
        if (count == 1) {
            Boolean expireSet = redisTemplate.expire(key, WINDOW_SIZE_HOURS, TimeUnit.HOURS);
            if (Boolean.FALSE.equals(expireSet)) {
                log.warn("[AUTH_RATE_LIMIT] Failed to set expiration - endpoint={}, ip={}, key={}",
                        endpoint, ip, key);
            }
        }

        boolean allowed = count <= maxRequests;

        if (allowed) {
            long remaining = maxRequests - count;
            log.debug("[AUTH_RATE_LIMIT] Request allowed - endpoint={}, ip={}, count={}, remaining={}",
                    endpoint, ip, count, remaining);
        } else {
            log.warn("[AUTH_RATE_LIMIT] Rate limit exceeded - endpoint={}, ip={}, count={}, limit={}",
                    endpoint, ip, count, maxRequests);
        }

        return allowed;
    }

    private long getRemainingAttempts(String ip, String keyPrefix, int maxRequests) {
        String key = keyPrefix + ip;
        String value = redisTemplate.opsForValue().get(key);

        long count = 0;
        if (value != null) {
            try {
                count = Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("[AUTH_RATE_LIMIT] Invalid count value in Redis - ip={}, value={}", ip, value);
            }
        }

        return Math.max(0, maxRequests - count);
    }
}
