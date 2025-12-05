package com.emmanuelgabe.portfolio.graphql.input.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Input type for filtering articles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleFilterInput {

    private List<Long> tagIds;
    private String searchQuery;
    private Boolean draft;
}
