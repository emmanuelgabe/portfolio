package com.emmanuelgabe.portfolio.ratelimit;

import java.time.Duration;

/**
 * Immutable rate limit policy describing a sliding window quota per identifier.
 * A policy is composed of a bucket name (used to namespace Redis keys),
 * a maximum number of requests, and the window duration.
 *
 * @param bucketName  logical name used as Redis key prefix, e.g. {@code "contact"} or {@code "chat"}
 * @param maxRequests maximum number of allowed requests within the window
 * @param window      rolling window duration
 */
public record RateLimitPolicy(String bucketName, int maxRequests, Duration window) {

    public RateLimitPolicy {
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalArgumentException("bucketName must not be blank");
        }
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be a positive duration");
        }
    }

    /**
     * Convenience factory for an hourly policy.
     *
     * @param bucketName  logical name of the rate limit bucket
     * @param maxRequests maximum requests per hour
     * @return a policy with a 1-hour window
     */
    public static RateLimitPolicy perHour(String bucketName, int maxRequests) {
        return new RateLimitPolicy(bucketName, maxRequests, Duration.ofHours(1));
    }
}
