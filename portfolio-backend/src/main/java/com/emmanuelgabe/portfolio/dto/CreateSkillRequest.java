package com.emmanuelgabe.portfolio.dto;

import com.emmanuelgabe.portfolio.entity.SkillCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

    @NotNull(message = "Level is required")
    @Min(value = 0, message = "Level must be at least 0")
    @Max(value = 100, message = "Level cannot exceed 100")
    private Integer level;

    @NotNull(message = "Display order is required")
    @Min(value = 0, message = "Display order must be at least 0")
    private Integer displayOrder;

    // Constructeurs
    public CreateSkillRequest() {
    }

    public CreateSkillRequest(String name, String icon, String color, SkillCategory category, Integer level, Integer displayOrder) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.category = category;
        this.level = level;
        this.displayOrder = displayOrder;
    }

    // Getters et Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public SkillCategory getCategory() {
        return category;
    }

    public void setCategory(SkillCategory category) {
        this.category = category;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
