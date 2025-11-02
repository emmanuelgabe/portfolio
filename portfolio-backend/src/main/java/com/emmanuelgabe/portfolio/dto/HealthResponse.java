package com.emmanuelgabe.portfolio.dto;

/**
 * DTO for simple health check responses
 */
public class HealthResponse {
    private String status;
    private String message;
    private Long timestamp;

    public HealthResponse() {
    }

    public HealthResponse(String status, String message, Long timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
