package com.emmanuelgabe.portfolio.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GraphQL input type for sorting articles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSortInput {

    private ArticleSortField field = ArticleSortField.PUBLISHED_AT;
    private SortDirection direction = SortDirection.DESC;
}
