package com.emmanuelgabe.portfolio.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * GraphQL input type for updating an article.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateArticleInput {

    private String title;
    private String content;
    private String excerpt;
    private Set<Long> tagIds;
}
