package com.emmanuelgabe.portfolio.security;

import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.service.AuthRateLimitService;
import com.emmanuelgabe.portfolio.util.IpAddressExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rate limiting filter for authentication endpoints.
 * Protects /api/auth/login and /api/auth/refresh against brute force attacks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REFRESH_PATH = "/api/auth/refresh";

    private final AuthRateLimitService authRateLimitService;
    private final BusinessMetrics metrics;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only apply rate limiting to POST requests on auth endpoints
        if (!"POST".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = IpAddressExtractor.extractIpAddress(request);

        if (LOGIN_PATH.equals(path)) {
            if (!authRateLimitService.isLoginAllowed(ip)) {
                log.warn("[AUTH_RATE_LIMIT] Login blocked - ip={}, path={}", ip, path);
                metrics.recordRateLimitHit();
                sendRateLimitResponse(response, "login", authRateLimitService.getMaxLoginRequestsPerHour());
                return;
            }
        } else if (REFRESH_PATH.equals(path)) {
            if (!authRateLimitService.isRefreshAllowed(ip)) {
                log.warn("[AUTH_RATE_LIMIT] Refresh blocked - ip={}, path={}", ip, path);
                metrics.recordRateLimitHit();
                sendRateLimitResponse(response, "refresh", authRateLimitService.getMaxRefreshRequestsPerHour());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Send 429 Too Many Requests response with JSON error message.
     *
     * @param response HTTP response
     * @param endpoint Endpoint name (login or refresh)
     * @param maxRequests Maximum requests per hour
     * @throws IOException if writing response fails
     */
    private void sendRateLimitResponse(HttpServletResponse response, String endpoint, int maxRequests)
            throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String message = String.format(
                "Rate limit exceeded for %s. Maximum %d attempts per hour. Please try again later.",
                endpoint, maxRequests
        );

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("status", 429);
        errorResponse.put("error", "Too Many Requests");
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only filter login and refresh endpoints
        return !LOGIN_PATH.equals(path) && !REFRESH_PATH.equals(path);
    }
}
