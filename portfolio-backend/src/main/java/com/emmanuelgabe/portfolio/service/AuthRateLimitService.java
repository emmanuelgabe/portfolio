package com.emmanuelgabe.portfolio.service;

/**
 * Rate limiting service for authentication endpoints.
 * Provides protection against brute force attacks on login and token refresh.
 */
public interface AuthRateLimitService {

    /**
     * Check if the given IP address is allowed to attempt login
     *
     * @param ip IP address to check
     * @return true if allowed, false if rate limit exceeded
     */
    boolean isLoginAllowed(String ip);

    /**
     * Check if the given IP address is allowed to refresh token
     *
     * @param ip IP address to check
     * @return true if allowed, false if rate limit exceeded
     */
    boolean isRefreshAllowed(String ip);

    /**
     * Get the remaining login attempts for the given IP address
     *
     * @param ip IP address to check
     * @return number of remaining attempts
     */
    long getRemainingLoginAttempts(String ip);

    /**
     * Get the remaining refresh attempts for the given IP address
     *
     * @param ip IP address to check
     * @return number of remaining attempts
     */
    long getRemainingRefreshAttempts(String ip);

    /**
     * Get the maximum login requests allowed per hour
     *
     * @return maximum login requests per hour
     */
    int getMaxLoginRequestsPerHour();

    /**
     * Get the maximum refresh requests allowed per hour
     *
     * @return maximum refresh requests per hour
     */
    int getMaxRefreshRequestsPerHour();

    /**
     * Reset login attempts counter for the given IP address.
     * Should be called after a successful login to allow legitimate users
     * to continue using the service.
     *
     * @param ip IP address to reset
     */
    void resetLoginAttempts(String ip);
}
