package com.emmanuelgabe.portfolio.dto;

import com.emmanuelgabe.portfolio.entity.ExperienceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating a new experience.
 * Used for POST requests to create professional, educational, certification, or volunteering experiences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateExperienceRequest {

    @Size(max = 200, message = "Company/Organization must be at most 200 characters")
    private String company;

    @Size(max = 200, message = "Role/Position must be at most 200 characters")
    private String role;

    @PastOrPresent(message = "Start date cannot be in the future")
    private LocalDate startDate;

    @PastOrPresent(message = "End date cannot be in the future")
    private LocalDate endDate;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    private ExperienceType type;

    private Boolean showMonths;

}
