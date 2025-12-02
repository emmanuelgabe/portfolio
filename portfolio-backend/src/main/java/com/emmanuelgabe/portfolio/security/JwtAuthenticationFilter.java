package com.emmanuelgabe.portfolio.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JWT Authentication Filter
 * Intercepts HTTP requests to extract and validate JWT tokens
 * Sets authentication in SecurityContext if token is valid
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && StringUtils.hasText(jwt)) {
                log.debug("[JWT_FILTER] JWT token found in request - path={}", request.getRequestURI());

                String username = jwtTokenProvider.extractUsername(jwt);
                log.debug("[JWT_FILTER] Username extracted from token - username={}", username);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtTokenProvider.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.info("[JWT_FILTER] Authentication set in security context - username={}, authorities={}",
                                username, userDetails.getAuthorities());
                    } else {
                        log.warn("[JWT_FILTER] JWT token validation failed - username={}", username);
                    }
                }
            } else {
                log.trace("[JWT_FILTER] No JWT token found in request - path={}", request.getRequestURI());
            }

        } catch (ExpiredJwtException ex) {
            log.warn("[JWT_FILTER] JWT token expired - path={}, username={}",
                    request.getRequestURI(), ex.getClaims().getSubject());
            sendUnauthorizedResponse(response, "JWT token has expired");
            return;

        } catch (SignatureException ex) {
            log.error("[JWT_FILTER] Invalid JWT signature - path={}, error={}",
                    request.getRequestURI(), ex.getMessage());
            sendUnauthorizedResponse(response, "Invalid JWT signature");
            return;

        } catch (MalformedJwtException ex) {
            log.error("[JWT_FILTER] Malformed JWT token - path={}, error={}",
                    request.getRequestURI(), ex.getMessage());
            sendUnauthorizedResponse(response, "Malformed JWT token");
            return;

        } catch (UnsupportedJwtException ex) {
            log.error("[JWT_FILTER] Unsupported JWT token - path={}, error={}",
                    request.getRequestURI(), ex.getMessage());
            sendUnauthorizedResponse(response, "Unsupported JWT token");
            return;

        } catch (IllegalArgumentException ex) {
            log.error("[JWT_FILTER] JWT claims string is empty - path={}, error={}",
                    request.getRequestURI(), ex.getMessage());
            sendUnauthorizedResponse(response, "JWT claims string is empty");
            return;

        } catch (Exception ex) {
            log.error("[JWT_FILTER] Cannot set user authentication - path={}, error={}",
                    request.getRequestURI(), ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Send 401 Unauthorized response with JSON error message
     * @param response HTTP response
     * @param message Error message
     * @throws IOException if writing response fails
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        String jsonResponse = String.format(
                "{\"status\":401,\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message, timestamp
        );

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();

        log.debug("[JWT_FILTER] Sent 401 Unauthorized response - message={}", message);
    }

    /**
     * Extract JWT token from Authorization header
     * @param request HTTP request
     * @return JWT token or null
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
