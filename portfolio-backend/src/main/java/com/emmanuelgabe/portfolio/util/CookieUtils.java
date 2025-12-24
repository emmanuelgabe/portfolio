package com.emmanuelgabe.portfolio.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Utility class for HTTP cookie operations.
 * Provides secure cookie handling for refresh tokens.
 */
public final class CookieUtils {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private CookieUtils() {
    }

    /**
     * Create a secure HttpOnly cookie for refresh token.
     *
     * @param token      The refresh token value
     * @param maxAgeDays Maximum age in days
     * @param secure     Whether to set Secure flag (true for HTTPS)
     * @return ResponseCookie configured for security
     */
    public static ResponseCookie createRefreshTokenCookie(String token, int maxAgeDays, boolean secure) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .maxAge(Duration.ofDays(maxAgeDays))
                .sameSite("Strict")
                .build();
    }

    /**
     * Create a cookie that deletes the refresh token.
     *
     * @param secure Whether to set Secure flag
     * @return ResponseCookie with maxAge=0 to delete
     */
    public static ResponseCookie createDeleteRefreshTokenCookie(boolean secure) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    /**
     * Extract refresh token from request cookies.
     *
     * @param request HTTP request
     * @return Optional containing the token if found
     */
    public static Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    /**
     * Add cookie to response using Set-Cookie header.
     *
     * @param response HTTP response
     * @param cookie   Cookie to add
     */
    public static void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
