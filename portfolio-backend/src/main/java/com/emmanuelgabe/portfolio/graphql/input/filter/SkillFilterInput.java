package com.emmanuelgabe.portfolio.graphql.input.filter;

import com.emmanuelgabe.portfolio.entity.SkillCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input type for filtering skills.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillFilterInput {

    private SkillCategory category;
}
