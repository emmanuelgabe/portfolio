package com.emmanuelgabe.portfolio.dto;

import com.emmanuelgabe.portfolio.entity.ExperienceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for experience responses.
 * Used in GET requests to return experience data to clients.
 * Includes all experience fields plus timestamps.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceResponse {

    private Long id;
    private String company;
    private String role;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private ExperienceType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean ongoing;
}
