package com.emmanuelgabe.portfolio.dto.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO for project search results.
 * Contains essential fields for displaying search results in admin panel.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSearchResult {

    private Long id;
    private String title;
    private String description;
    private String techStack;
    private boolean featured;
    private List<String> tags;
}
