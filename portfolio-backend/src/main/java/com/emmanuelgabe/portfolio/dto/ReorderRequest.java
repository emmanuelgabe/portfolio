package com.emmanuelgabe.portfolio.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic request DTO for reordering entities.
 * Contains list of entity IDs in the desired display order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRequest {

    @NotNull(message = "IDs list is required")
    private List<Long> orderedIds;
}
