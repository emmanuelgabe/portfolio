package com.emmanuelgabe.portfolio.dto;

import java.time.Instant;

/**
 * Response DTO for visitor statistics.
 * Contains current active visitors and historical data.
 *
 * @param activeCount current number of active visitors
 * @param lastMonthCount total unique visitors from last month
 * @param timestamp when the stats were generated
 */
public record VisitorStatsResponse(
        int activeCount,
        long lastMonthCount,
        Instant timestamp
) {
}
