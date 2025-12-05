package com.emmanuelgabe.portfolio.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GraphQL input type for sorting projects.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSortInput {

    private ProjectSortField field = ProjectSortField.CREATED_AT;
    private SortDirection direction = SortDirection.DESC;
}
