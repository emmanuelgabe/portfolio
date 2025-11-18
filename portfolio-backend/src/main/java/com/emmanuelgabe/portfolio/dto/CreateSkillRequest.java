package com.emmanuelgabe.portfolio.dto;

import com.emmanuelgabe.portfolio.entity.SkillCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSkillRequest {

    @NotBlank(message = "Skill name is required")
    @Size(min = 2, max = 50, message = "Skill name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Icon is required")
    @Size(max = 50, message = "Icon class cannot exceed 50 characters")
    private String icon;

    @NotBlank(message = "Color is required")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Color must be a valid hex color code (e.g., #FF5733)")
    private String color;

    @NotNull(message = "Category is required")
    private SkillCategory category;

    @NotNull(message = "Display order is required")
    @Min(value = 0, message = "Display order must be at least 0")
    private Integer displayOrder;
}
