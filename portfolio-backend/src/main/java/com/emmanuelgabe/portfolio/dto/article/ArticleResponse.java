package com.emmanuelgabe.portfolio.dto.article;

import com.emmanuelgabe.portfolio.dto.TagResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for article response with rendered HTML content.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {

    private Long id;
    private String title;
    private String slug;
    private String content;
    private String contentHtml;
    private String excerpt;
    private String authorName;
    private boolean draft;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer readingTimeMinutes;
    private List<TagResponse> tags;
    private List<ArticleImageResponse> images;
}
