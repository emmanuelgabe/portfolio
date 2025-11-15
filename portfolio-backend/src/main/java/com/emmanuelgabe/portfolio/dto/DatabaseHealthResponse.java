package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for database health check responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseHealthResponse {

    private String status;
    private String message;
    private String database;
    private String url;
    private String error;
}
