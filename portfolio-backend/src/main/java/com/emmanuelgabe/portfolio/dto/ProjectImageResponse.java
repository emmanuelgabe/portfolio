package com.emmanuelgabe.portfolio.dto;

import com.emmanuelgabe.portfolio.entity.ImageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for project image data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectImageResponse {

    private Long id;

    private String imageUrl;

    private String thumbnailUrl;

    private String altText;

    private String caption;

    private Integer displayOrder;

    private boolean primary;

    private ImageStatus status;

    private LocalDateTime uploadedAt;
}
