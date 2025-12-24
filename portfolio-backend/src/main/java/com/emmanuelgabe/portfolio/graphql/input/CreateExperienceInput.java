package com.emmanuelgabe.portfolio.graphql.input;

import com.emmanuelgabe.portfolio.entity.ExperienceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * GraphQL input type for creating an experience.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateExperienceInput {

    private String company;
    private String role;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private ExperienceType type;
}
