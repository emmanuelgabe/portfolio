package com.emmanuelgabe.portfolio.dto;

import com.emmanuelgabe.portfolio.entity.ExperienceType;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for updating an existing experience.
 * All fields are optional to allow partial updates.
 * Used for PUT requests to update professional, educational, certification, or volunteering experiences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExperienceRequest {

    @Size(min = 2, max = 200, message = "Company/Organization must be between 2 and 200 characters")
    private String company;

    @Size(min = 2, max = 200, message = "Role/Position must be between 2 and 200 characters")
    private String role;

    @PastOrPresent(message = "Start date cannot be in the future")
    private LocalDate startDate;

    @PastOrPresent(message = "End date cannot be in the future")
    private LocalDate endDate;

    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    private ExperienceType type;
}
