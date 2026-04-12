package com.emmanuelgabe.portfolio.healthmonitor;

import com.emmanuelgabe.portfolio.entity.HealthReport;
import com.emmanuelgabe.portfolio.healthmonitor.action.HealthCycleType;
import com.emmanuelgabe.portfolio.healthmonitor.action.ReportSeverity;
import lombok.extern.slf4j.Slf4j;

/**
 * Fallback agent used when {@code ai.health-monitor.enabled=false} or the LLM
 * stack is unavailable. Returns a lightweight report so callers don't need to
 * special-case a null agent.
 */
@Slf4j
public class NoOpHealthAgentService implements HealthAgentService {

    @Override
    public HealthReport analyze(HealthCycleType cycleType) {
        log.debug("[HEALTH_AGENT] NoOp analyze - cycleType={}", cycleType);
        HealthReport report = new HealthReport();
        report.setCycleType(cycleType);
        report.setSeverity(ReportSeverity.UNKNOWN);
        report.setSummary("AI health monitor est desactive (ai.health-monitor.enabled=false).");
        report.setAnomaliesJson("[]");
        report.setToolCallsJson("[]");
        report.setDurationMs(0L);
        return report;
    }
}
