package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for contact moderation statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactStatsResponse {

    private long totalCount;
    private Map<String, Long> categoryCounts;
    private Map<String, Long> verdictCounts;
    private Map<String, Long> dailyActivity;
}
