package com.emmanuelgabe.portfolio.service;

public interface RateLimitService {

    /**
     * Check if the given IP address is allowed to make a request
     *
     * @param ip IP address to check
     * @return true if allowed, false if rate limit exceeded
     */
    boolean isAllowed(String ip);

    /**
     * Get the remaining attempts for the given IP address
     *
     * @param ip IP address to check
     * @return number of remaining attempts
     */
    long getRemainingAttempts(String ip);
}
