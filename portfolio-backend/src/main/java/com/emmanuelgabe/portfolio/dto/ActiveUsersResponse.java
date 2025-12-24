package com.emmanuelgabe.portfolio.dto;

import java.time.Instant;

/**
 * Response DTO for active users count.
 * Used for both REST and SSE responses.
 */
public record ActiveUsersResponse(
        int count,
        Instant timestamp
) {
}
