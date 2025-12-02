package com.emmanuelgabe.portfolio.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for reordering project images.
 * Contains list of image IDs in the desired display order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderProjectImagesRequest {

    @NotNull(message = "Image IDs list is required")
    private List<Long> imageIds;
}
