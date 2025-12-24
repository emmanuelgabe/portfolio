package com.emmanuelgabe.portfolio.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * GraphQL input type for updating a project.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectInput {

    private String title;
    private String description;
    private String techStack;
    private String githubUrl;
    private String imageUrl;
    private String demoUrl;
    private Boolean featured;
    private Boolean hasDetails;
    private Set<Long> tagIds;
}
