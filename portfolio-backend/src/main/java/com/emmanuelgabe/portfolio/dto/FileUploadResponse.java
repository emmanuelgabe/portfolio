package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for file upload operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    private String fileName;
    private String fileUrl;
    private String fileType;
    private long fileSize;
}
