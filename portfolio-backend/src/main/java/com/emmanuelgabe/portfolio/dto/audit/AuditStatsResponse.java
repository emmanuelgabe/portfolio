package com.emmanuelgabe.portfolio.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for audit statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditStatsResponse {

    private Map<String, Long> actionCounts;
    private Map<String, Long> entityTypeCounts;
    private Map<String, Long> dailyActivity;
    private Map<String, Long> userActivity;
    private long failedActionsCount;
    private long totalCount;
}
