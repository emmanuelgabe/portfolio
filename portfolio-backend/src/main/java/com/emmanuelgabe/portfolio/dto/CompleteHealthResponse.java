package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for complete system health status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteHealthResponse {

    private String status;
    private Map<String, Object> checks;
    private Long timestamp;
}
