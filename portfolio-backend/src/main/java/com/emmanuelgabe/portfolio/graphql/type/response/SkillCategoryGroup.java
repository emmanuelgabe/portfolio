package com.emmanuelgabe.portfolio.graphql.type.response;

import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Group of skills by category.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillCategoryGroup {

    private SkillCategory category;
    private String categoryDisplayName;
    private List<SkillResponse> skills;
}
