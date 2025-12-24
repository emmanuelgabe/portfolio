package com.emmanuelgabe.portfolio.graphql.input.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Input type for filtering projects.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectFilterInput {

    private List<Long> tagIds;
    private Boolean featured;
    private String searchQuery;
    private String technology;
}
