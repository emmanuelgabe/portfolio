package com.emmanuelgabe.portfolio.dto.health;

import com.emmanuelgabe.portfolio.healthmonitor.action.HealthCycleType;
import com.emmanuelgabe.portfolio.healthmonitor.action.ReportSeverity;

import java.time.LocalDateTime;
import java.util.List;

public record HealthReportResponse(
        Long id,
        HealthCycleType cycleType,
        ReportSeverity severity,
        String summary,
        List<String> anomalies,
        List<String> toolCalls,
        long durationMs,
        LocalDateTime createdAt
) { }
