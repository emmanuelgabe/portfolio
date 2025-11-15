package com.emmanuelgabe.portfolio.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for updating an existing Tag.
 * All fields are optional (null values are ignored during update).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTagRequest {

    @Size(min = 2, max = 50, message = "Tag name must be between 2 and 50 characters")
    private String name;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
             message = "Color must be a valid hex color code (e.g., #FF5733)")
    private String color;
}
