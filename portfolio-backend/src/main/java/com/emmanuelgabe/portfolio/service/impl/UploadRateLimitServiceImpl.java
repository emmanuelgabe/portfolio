package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.service.UploadRateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis-based rate limiting implementation for file upload endpoints.
 * Uses fixed window strategy with 1-hour TTL, tracking by username.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadRateLimitServiceImpl implements UploadRateLimitService {

    private static final String UPLOAD_KEY_PREFIX = "rate_limit:upload:";
    private static final long WINDOW_SIZE_HOURS = 1;

    @Value("${app.rate-limit.upload.max-requests-per-hour:30}")
    private int maxUploadsPerHour;

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean isUploadAllowed(String username) {
        String key = UPLOAD_KEY_PREFIX + username;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            count = 0L;
        }

        if (count == 1) {
            Boolean expireSet = redisTemplate.expire(key, WINDOW_SIZE_HOURS, TimeUnit.HOURS);
            if (Boolean.FALSE.equals(expireSet)) {
                log.warn("[UPLOAD_RATE_LIMIT] Failed to set expiration - username={}, key={}",
                        username, key);
            }
        }

        boolean allowed = count <= maxUploadsPerHour;

        if (allowed) {
            long remaining = maxUploadsPerHour - count;
            log.debug("[UPLOAD_RATE_LIMIT] Upload allowed - username={}, count={}, remaining={}",
                    username, count, remaining);
        } else {
            log.warn("[UPLOAD_RATE_LIMIT] Rate limit exceeded - username={}, count={}, limit={}",
                    username, count, maxUploadsPerHour);
        }

        return allowed;
    }

    @Override
    public long getRemainingUploads(String username) {
        String key = UPLOAD_KEY_PREFIX + username;
        String value = redisTemplate.opsForValue().get(key);

        long count = 0;
        if (value != null) {
            try {
                count = Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("[UPLOAD_RATE_LIMIT] Invalid count value in Redis - username={}, value={}",
                        username, value);
            }
        }

        return Math.max(0, maxUploadsPerHour - count);
    }

    @Override
    public int getMaxUploadsPerHour() {
        return maxUploadsPerHour;
    }
}
