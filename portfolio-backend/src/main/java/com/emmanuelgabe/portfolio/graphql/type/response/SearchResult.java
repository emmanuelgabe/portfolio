package com.emmanuelgabe.portfolio.graphql.type.response;

import com.emmanuelgabe.portfolio.dto.ProjectResponse;
import com.emmanuelgabe.portfolio.dto.article.ArticleResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Combined search results across multiple entity types.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private List<ProjectResponse> projects;
    private List<ArticleResponse> articles;
    private int totalCount;
}
