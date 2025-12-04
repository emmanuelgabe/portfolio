package com.emmanuelgabe.portfolio.audit;

import lombok.Builder;
import lombok.Getter;

/**
 * Holds audit context information for the current request.
 * Populated by AuditContextFilter at request start.
 */
@Getter
@Builder
public class AuditContext {

    private final Long userId;
    private final String username;
    private final String userRole;
    private final String ipAddress;
    private final String userAgent;
    private final String requestMethod;
    private final String requestUri;
    private final String requestId;
}
