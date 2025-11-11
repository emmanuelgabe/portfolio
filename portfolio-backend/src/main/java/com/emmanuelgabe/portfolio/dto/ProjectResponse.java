package com.emmanuelgabe.portfolio.dto;

import com.emmanuelgabe.portfolio.entity.Project;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public class ProjectResponse {

    private Long id;
    private String title;
    private String description;
    private String techStack;
    private String githubUrl;
    private String imageUrl;
    private String demoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean featured;
    private Set<TagResponse> tags;

    public ProjectResponse() {
    }

    public ProjectResponse(Long id, String title, String description, String techStack,
                           String githubUrl, String imageUrl, String demoUrl,
                           LocalDateTime createdAt, LocalDateTime updatedAt,
                           Boolean featured, Set<TagResponse> tags) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.techStack = techStack;
        this.githubUrl = githubUrl;
        this.imageUrl = imageUrl;
        this.demoUrl = demoUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.featured = featured;
        this.tags = tags;
    }

    // Méthode statique pour convertir une entité en DTO
    public static ProjectResponse fromEntity(Project project) {
        if (project == null) {
            return null;
        }

        Set<TagResponse> tagResponses = project.getTags().stream()
                .map(TagResponse::fromEntity)
                .collect(Collectors.toSet());

        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getTechStack(),
                project.getGithubUrl(),
                project.getImageUrl(),
                project.getDemoUrl(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getFeatured(),
                tagResponses
        );
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTechStack() {
        return techStack;
    }

    public void setTechStack(String techStack) {
        this.techStack = techStack;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDemoUrl() {
        return demoUrl;
    }

    public void setDemoUrl(String demoUrl) {
        this.demoUrl = demoUrl;
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

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public Set<TagResponse> getTags() {
        return tags;
    }

    public void setTags(Set<TagResponse> tags) {
        this.tags = tags;
    }
}
