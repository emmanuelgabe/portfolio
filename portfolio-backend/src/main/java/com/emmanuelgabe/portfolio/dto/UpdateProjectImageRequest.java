package com.emmanuelgabe.portfolio.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating project image metadata (alt text and caption).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectImageRequest {

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    private String altText;

    @Size(max = 500, message = "Caption must not exceed 500 characters")
    private String caption;
}
