package com.emmanuelgabe.portfolio.security;

import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.service.UploadRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Rate limiting filter for file upload endpoints.
 * Protects /api/admin/.../images endpoints against resource exhaustion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadRateLimitFilter extends OncePerRequestFilter {

    private static final String IMAGES_PATH_PATTERN = "/images";

    private final UploadRateLimitService uploadRateLimitService;
    private final BusinessMetrics metrics;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (!"POST".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = authentication.getName();

        if (!uploadRateLimitService.isUploadAllowed(username)) {
            log.warn("[UPLOAD_RATE_LIMIT] Upload blocked - username={}, path={}", username, path);
            metrics.recordRateLimitHit();
            sendRateLimitResponse(response, uploadRateLimitService.getMaxUploadsPerHour());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendRateLimitResponse(HttpServletResponse response, int maxUploads) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        String message = String.format(
                "Upload rate limit exceeded. Maximum %d uploads per hour. Please try again later.",
                maxUploads
        );

        String jsonResponse = String.format(
                "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message, timestamp
        );

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/admin/") || !path.contains(IMAGES_PATH_PATTERN);
    }
}
