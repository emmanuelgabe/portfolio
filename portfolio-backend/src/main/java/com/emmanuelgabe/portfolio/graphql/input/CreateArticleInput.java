package com.emmanuelgabe.portfolio.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * GraphQL input type for creating an article.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateArticleInput {

    private String title;
    private String content;
    private String excerpt;
    private Boolean draft;
    private Set<Long> tagIds;
}
