package com.emmanuelgabe.portfolio.dto;

import java.util.Map;

/**
 * DTO for complete system health status
 */
public class CompleteHealthResponse {
    private String status;
    private Map<String, Object> checks;
    private Long timestamp;

    public CompleteHealthResponse() {
    }

    public CompleteHealthResponse(String status, Map<String, Object> checks, Long timestamp) {
        this.status = status;
        this.checks = checks;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getChecks() {
        return checks;
    }

    public void setChecks(Map<String, Object> checks) {
        this.checks = checks;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
