package com.emmanuelgabe.portfolio.healthmonitor.tools;

import com.emmanuelgabe.portfolio.dto.audit.AuditStatsResponse;
import com.emmanuelgabe.portfolio.healthmonitor.HealthToolCallTracker;
import com.emmanuelgabe.portfolio.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Green tool: summarized audit activity for the last N days. Spikes in
 * failed actions, login failures or error categories help the agent tell
 * apart a usage surge from a genuine incident.
 */
@Slf4j
@RequiredArgsConstructor
public class AuditStatsTool {

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 30;

    private final AuditService auditService;
    private final HealthToolCallTracker tracker;

    @Tool(description = "Return aggregated audit statistics for the last N days: total actions, "
            + "failed actions count, per-action breakdown, per-entity-type breakdown, and "
            + "per-user activity. Use to detect attack patterns, unusual error spikes, or "
            + "correlate infrastructure anomalies with business activity.")
    public Map<String, Object> getAuditStats(
            @ToolParam(description = "Number of days to look back (1-30)") int days) {
        tracker.record("getAuditStats");
        int clamped = Math.max(MIN_DAYS, Math.min(MAX_DAYS, days));
        log.debug("[HEALTH_TOOL] getAuditStats - days={}", clamped);

        AuditStatsResponse stats = auditService.getStats(
                LocalDateTime.now().minusDays(clamped));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("daysLookback", clamped);
        result.put("totalCount", stats.getTotalCount());
        result.put("failedActionsCount", stats.getFailedActionsCount());
        result.put("actionCounts", stats.getActionCounts());
        result.put("entityTypeCounts", stats.getEntityTypeCounts());
        result.put("userActivity", stats.getUserActivity());
        return result;
    }
}
