package com.emmanuelgabe.portfolio.ratelimit;

/**
 * Generic per-identifier rate limiter abstraction.
 * Implementations enforce a {@link RateLimitPolicy} against a backing store
 * (typically Redis) and expose atomic acquire + quota inspection operations.
 *
 * <p>Each call to {@link #tryAcquire(RateLimitPolicy, String)} is an atomic
 * reservation: it increments the counter and returns whether the request
 * fits within the quota. {@link #remainingQuota(RateLimitPolicy, String)}
 * performs a read-only inspection and never mutates state.</p>
 */
public interface RateLimiter {

    /**
     * Atomically attempt to consume one request from the quota.
     * The counter is always incremented: subsequent calls will reflect
     * the attempt even if it was denied.
     *
     * @param policy     the rate limit policy to apply
     * @param identifier the unique client identifier (typically IP address)
     * @return {@code true} if the request is within the quota, {@code false} otherwise
     */
    boolean tryAcquire(RateLimitPolicy policy, String identifier);

    /**
     * Inspect the number of requests remaining in the current window.
     * Never mutates the counter.
     *
     * @param policy     the rate limit policy to apply
     * @param identifier the unique client identifier
     * @return the number of remaining requests, floored at 0
     */
    long remainingQuota(RateLimitPolicy policy, String identifier);
}
