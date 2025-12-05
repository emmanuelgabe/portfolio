package com.emmanuelgabe.portfolio.graphql.input.filter;

import com.emmanuelgabe.portfolio.entity.ExperienceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input type for filtering experiences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceFilterInput {

    private ExperienceType type;
    private Boolean ongoing;
}
