package com.emmanuelgabe.portfolio.dto.article;

import com.emmanuelgabe.portfolio.entity.ImageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for article image response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleImageResponse {

    private Long id;
    private String imageUrl;
    private String thumbnailUrl;
    private ImageStatus status;
    private LocalDateTime uploadedAt;
}
