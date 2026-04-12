package com.emmanuelgabe.portfolio.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis-backed implementation of {@link RateLimiter} using an atomic Lua script.
 * A single EVAL call atomically increments the counter and sets the TTL on first
 * access, eliminating the INCR-then-EXPIRE race window.
 *
 * <p>Key format: {@code rate_limit:{bucketName}:{identifier}}. Each bucket lives in
 * its own namespace so policies with different quotas do not interfere.</p>
 *
 * <p>Fail-open on Redis errors: if Redis is unreachable the request is allowed through
 * to avoid blocking legitimate traffic. The error is logged at WARN level.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "rate_limit:";

    /** Atomically increment key and set TTL on first access. Returns the new counter value. */
    private static final RedisScript<Long> INCREMENT_AND_EXPIRE = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) "
            + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
            + "return count",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryAcquire(RateLimitPolicy policy, String identifier) {
        String key = buildKey(policy, identifier);
        try {
            Long count = redisTemplate.execute(
                    INCREMENT_AND_EXPIRE,
                    List.of(key),
                    String.valueOf(policy.window().toSeconds()));

            if (count == null) {
                log.warn("[RATE_LIMIT] Redis script returned null - bucket={}, identifier={}",
                        policy.bucketName(), identifier);
                return true;
            }

            boolean allowed = count <= policy.maxRequests();
            if (allowed) {
                log.debug("[RATE_LIMIT] Request allowed - bucket={}, identifier={}, count={}, max={}",
                        policy.bucketName(), identifier, count, policy.maxRequests());
            } else {
                log.warn("[RATE_LIMIT] Request denied - bucket={}, identifier={}, count={}, max={}",
                        policy.bucketName(), identifier, count, policy.maxRequests());
            }
            return allowed;

        } catch (Exception e) {
            log.error("[RATE_LIMIT] Redis error on tryAcquire, failing open - bucket={}, identifier={}, error={}",
                    policy.bucketName(), identifier, e.getMessage());
            return true;
        }
    }

    @Override
    public long remainingQuota(RateLimitPolicy policy, String identifier) {
        String key = buildKey(policy, identifier);
        try {
            String value = redisTemplate.opsForValue().get(key);
            long count = 0L;
            if (value != null) {
                try {
                    count = Long.parseLong(value);
                } catch (NumberFormatException e) {
                    log.warn("[RATE_LIMIT] Invalid counter value in Redis - bucket={}, key={}, value={}",
                            policy.bucketName(), key, value);
                }
            }
            return Math.max(0L, policy.maxRequests() - count);
        } catch (Exception e) {
            log.error("[RATE_LIMIT] Redis error on remainingQuota - bucket={}, identifier={}, error={}",
                    policy.bucketName(), identifier, e.getMessage());
            return policy.maxRequests();
        }
    }

    private static String buildKey(RateLimitPolicy policy, String identifier) {
        return KEY_PREFIX + policy.bucketName() + ":" + identifier;
    }
}
