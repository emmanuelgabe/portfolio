package com.emmanuelgabe.portfolio.healthmonitor;

import com.emmanuelgabe.portfolio.dto.health.HealthReportResponse;
import com.emmanuelgabe.portfolio.dto.health.ProposedActionResponse;
import com.emmanuelgabe.portfolio.entity.HealthProposedAction;
import com.emmanuelgabe.portfolio.entity.HealthReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manual mapper between health monitor entities and their DTOs.
 * JSON fields (anomalies, tool calls, params) are parsed into typed structures
 * here rather than at the JPA layer so the entities stay free of Jackson deps.
 */
@Slf4j
@Component
public class HealthMonitorMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() { };

    public HealthReportResponse toResponse(HealthReport report) {
        return new HealthReportResponse(
                report.getId(),
                report.getCycleType(),
                report.getSeverity(),
                report.getSummary(),
                parseStringList(report.getAnomaliesJson()),
                parseStringList(report.getToolCallsJson()),
                report.getDurationMs(),
                report.getCreatedAt()
        );
    }

    public ProposedActionResponse toResponse(HealthProposedAction action) {
        return new ProposedActionResponse(
                action.getId(),
                action.getReportId(),
                action.getLevel(),
                action.getActionType(),
                parseObjectMap(action.getParamsJson()),
                action.getJustification(),
                action.getStatus(),
                action.getApprovedBy(),
                action.getApprovedAt(),
                action.getExecutedAt(),
                action.getRejectionReason(),
                action.getResultJson(),
                action.getExpiresAt(),
                action.getCreatedAt()
        );
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) {
            return Collections.emptyList();
        }
        try {
            List<String> value = MAPPER.readValue(json, STRING_LIST);
            return value == null ? Collections.emptyList() : value;
        } catch (JsonProcessingException e) {
            log.debug("[HEALTH_MAPPER] String list parse failed - error={}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, Object> parseObjectMap(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> value = MAPPER.readValue(json, OBJECT_MAP);
            return value == null ? Collections.emptyMap() : value;
        } catch (JsonProcessingException e) {
            log.debug("[HEALTH_MAPPER] Object map parse failed - error={}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
