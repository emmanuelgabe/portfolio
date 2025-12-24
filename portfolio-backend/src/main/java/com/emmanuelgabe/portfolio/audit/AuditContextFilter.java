package com.emmanuelgabe.portfolio.audit;

import com.emmanuelgabe.portfolio.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filter that populates AuditContext for each request.
 * Extracts user information from SecurityContext and request metadata.
 * Must run after JwtAuthenticationFilter to access authenticated user.
 */
@Slf4j
@Component
@Order(10)
public class AuditContextFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";
    private static final String USER_AGENT = "User-Agent";

    /**
     * Maximum length for User-Agent header to prevent storage issues.
     */
    private static final int MAX_USER_AGENT_LENGTH = 500;

    /**
     * IPv4 pattern for validation.
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    /**
     * IPv6 pattern for validation (simplified).
     */
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::$|^::1$|^([0-9a-fA-F]{1,4}:)*:([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4}$"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            AuditContext.AuditContextBuilder builder = AuditContext.builder()
                    .ipAddress(extractIpAddress(request))
                    .userAgent(truncateUserAgent(request.getHeader(USER_AGENT)))
                    .requestMethod(request.getMethod())
                    .requestUri(request.getRequestURI())
                    .requestId(UUID.randomUUID().toString());

            // Extract user from security context if authenticated
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
                builder.userId(user.getId())
                        .username(user.getUsername())
                        .userRole(user.getRole().name());
                log.trace("[AUDIT_FILTER] User context set - username={}", user.getUsername());
            } else {
                builder.username("anonymous");
                log.trace("[AUDIT_FILTER] Anonymous request - path={}", request.getRequestURI());
            }

            AuditContextHolder.setContext(builder.build());

            filterChain.doFilter(request, response);

        } finally {
            AuditContextHolder.clear();
        }
    }

    /**
     * Extract client IP address from request headers or remote address.
     * Handles reverse proxy scenarios (Nginx, Cloudflare).
     * Uses the LAST IP in X-Forwarded-For chain as it's added by the trusted reverse proxy.
     * Validates IP format to prevent spoofing attacks.
     *
     * @param request HTTP request
     * @return validated client IP address, or "unknown" if invalid
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ip = null;

        // Priority 1: X-Forwarded-For header (take LAST IP - added by trusted proxy)
        String xForwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String[] ips = xForwardedFor.split(",");
            // Take the last IP in the chain (closest to our reverse proxy)
            ip = ips[ips.length - 1].trim();
        }

        // Priority 2: X-Real-IP header
        if (ip == null || ip.isEmpty()) {
            String xRealIp = request.getHeader(X_REAL_IP);
            if (xRealIp != null && !xRealIp.isEmpty()) {
                ip = xRealIp.trim();
            }
        }

        // Priority 3: Remote address
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        // Validate and return
        return isValidIpAddress(ip) ? ip : "unknown";
    }

    /**
     * Validates if the given string is a valid IPv4 or IPv6 address.
     *
     * @param ip the IP address to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip).matches() || IPV6_PATTERN.matcher(ip).matches();
    }

    /**
     * Truncates User-Agent header to prevent storage issues.
     *
     * @param userAgent the User-Agent header value
     * @return truncated User-Agent or null if input is null
     */
    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        if (userAgent.length() > MAX_USER_AGENT_LENGTH) {
            return userAgent.substring(0, MAX_USER_AGENT_LENGTH - 3) + "...";
        }
        return userAgent;
    }
}
