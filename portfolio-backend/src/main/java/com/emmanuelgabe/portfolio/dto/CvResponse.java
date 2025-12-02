package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for CV entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CvResponse {

    private Long id;
    private String fileName;
    private String originalFileName;
    private String fileUrl;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private boolean current;
}
