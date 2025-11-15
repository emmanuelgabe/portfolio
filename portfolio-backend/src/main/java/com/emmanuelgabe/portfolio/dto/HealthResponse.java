package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for simple health check responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {

    private String status;
    private String message;
    private Long timestamp;
}
