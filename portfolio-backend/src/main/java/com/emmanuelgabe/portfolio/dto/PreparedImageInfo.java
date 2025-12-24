package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contains information about a prepared image ready for async processing.
 * Used to pass image details from ImageService to the caller for event publishing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreparedImageInfo {

    private String imageUrl;
    private String thumbnailUrl;
    private String tempFilePath;
    private String optimizedFilePath;
    private String thumbnailFilePath;
    private Long fileSize;
}
