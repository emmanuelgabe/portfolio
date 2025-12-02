package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:contact:";
    private static final long WINDOW_SIZE_HOURS = 1;

    @Value("${app.rate-limit.contact.max-requests-per-hour:5}")
    private int maxRequestsPerHour;

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean isAllowed(String ip) {
        String key = RATE_LIMIT_KEY_PREFIX + ip;

        // Increment the counter atomically
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            count = 0L;
        }

        // Set expiration on first request (atomic operation to prevent race condition)
        if (count == 1) {
            Boolean expireSet = redisTemplate.expire(key, WINDOW_SIZE_HOURS, TimeUnit.HOURS);
            if (Boolean.FALSE.equals(expireSet)) {
                log.warn("[RATE_LIMIT] Failed to set expiration - ip={}, key={}", ip, key);
            }
        }

        boolean allowed = count <= maxRequestsPerHour;

        if (allowed) {
            long remaining = maxRequestsPerHour - count;
            log.info("[RATE_LIMIT] Request allowed - ip={}, count={}, remaining={}", ip, count, remaining);
        } else {
            log.warn("[RATE_LIMIT] Request denied - ip={}, count={}, limit exceeded", ip, count);
        }

        return allowed;
    }

    @Override
    public long getRemainingAttempts(String ip) {
        String key = RATE_LIMIT_KEY_PREFIX + ip;
        String value = redisTemplate.opsForValue().get(key);

        long count = 0;
        if (value != null) {
            try {
                count = Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("[RATE_LIMIT] Invalid count value in Redis - ip={}, value={}", ip, value);
            }
        }

        long remaining = Math.max(0, maxRequestsPerHour - count);
        log.debug("[RATE_LIMIT] Check remaining - ip={}, count={}, remaining={}", ip, count, remaining);
        return remaining;
    }
}
