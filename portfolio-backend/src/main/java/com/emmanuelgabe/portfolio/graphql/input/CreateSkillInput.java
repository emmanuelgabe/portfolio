package com.emmanuelgabe.portfolio.graphql.input;

import com.emmanuelgabe.portfolio.entity.IconType;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GraphQL input type for creating a skill.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSkillInput {

    private String name;
    private String icon;
    private IconType iconType;
    private String customIconUrl;
    private String color;
    private SkillCategory category;
    private Integer displayOrder;
}
