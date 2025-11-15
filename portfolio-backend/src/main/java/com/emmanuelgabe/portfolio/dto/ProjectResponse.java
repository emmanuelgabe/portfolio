package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private boolean featured;
    private Set<TagResponse> tags;
}
