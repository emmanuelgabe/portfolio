package com.emmanuelgabe.portfolio.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for extracting IP addresses from HTTP requests
 * Handles proxy headers (X-Forwarded-For, X-Real-IP) and falls back to remote address
 */
public final class IpAddressExtractor {

    private IpAddressExtractor() {
        // Utility class, prevent instantiation
    }

    /**
     * Extract the real IP address from the HTTP request
     * Checks X-Forwarded-For and X-Real-IP headers before falling back to remote address
     *
     * @param request HTTP request
     * @return IP address as string
     */
    public static String extractIpAddress(HttpServletRequest request) {
        // Try to get real IP from proxy headers
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }
}
