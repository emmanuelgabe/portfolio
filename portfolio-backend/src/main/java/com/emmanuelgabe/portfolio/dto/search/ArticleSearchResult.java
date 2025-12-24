package com.emmanuelgabe.portfolio.dto.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for article search results.
 * Contains essential fields for displaying search results in admin panel.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSearchResult {

    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private boolean draft;
    private LocalDateTime publishedAt;
    private List<String> tags;
}
