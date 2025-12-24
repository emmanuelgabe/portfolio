package com.emmanuelgabe.portfolio.service;

/**
 * Rate limiting service for file upload endpoints.
 * Limits the number of uploads per user to prevent resource exhaustion.
 */
public interface UploadRateLimitService {

    /**
     * Check if the given user is allowed to upload a file.
     *
     * @param username Username to check
     * @return true if allowed, false if rate limit exceeded
     */
    boolean isUploadAllowed(String username);

    /**
     * Get the remaining upload attempts for the given user.
     *
     * @param username Username to check
     * @return number of remaining attempts
     */
    long getRemainingUploads(String username);

    /**
     * Get the maximum uploads allowed per hour.
     *
     * @return maximum uploads per hour
     */
    int getMaxUploadsPerHour();
}
