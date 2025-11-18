package com.emmanuelgabe.portfolio.dto;

import com.emmanuelgabe.portfolio.entity.SkillCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillResponse {

    private Long id;
    private String name;
    private String icon;
    private String color;
    private SkillCategory category;
    private String categoryDisplayName;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
