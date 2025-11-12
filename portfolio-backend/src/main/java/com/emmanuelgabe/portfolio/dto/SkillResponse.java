package com.emmanuelgabe.portfolio.dto;

import com.emmanuelgabe.portfolio.entity.Skill;
import com.emmanuelgabe.portfolio.entity.SkillCategory;

import java.time.LocalDateTime;

public class SkillResponse {

    private Long id;
    private String name;
    private String icon;
    private String color;
    private SkillCategory category;
    private String categoryDisplayName;
    private Integer level;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SkillResponse() {
    }

    public SkillResponse(Long id, String name, String icon, String color, SkillCategory category,
                         String categoryDisplayName, Integer level, Integer displayOrder,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.category = category;
        this.categoryDisplayName = categoryDisplayName;
        this.level = level;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Méthode statique pour convertir une entité en DTO
    public static SkillResponse fromEntity(Skill skill) {
        if (skill == null) {
            return null;
        }

        return new SkillResponse(
                skill.getId(),
                skill.getName(),
                skill.getIcon(),
                skill.getColor(),
                skill.getCategory(),
                skill.getCategory() != null ? skill.getCategory().getDisplayName() : null,
                skill.getLevel(),
                skill.getDisplayOrder(),
                skill.getCreatedAt(),
                skill.getUpdatedAt()
        );
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getCategoryDisplayName() {
        return categoryDisplayName;
    }

    public void setCategoryDisplayName(String categoryDisplayName) {
        this.categoryDisplayName = categoryDisplayName;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
