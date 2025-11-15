package com.emmanuelgabe.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for creating a new Tag.
 * Contains validation rules for tag creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTagRequest {

    @NotBlank(message = "Tag name is required")
    @Size(min = 2, max = 50, message = "Tag name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Color is required")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
             message = "Color must be a valid hex color code (e.g., #FF5733)")
    private String color;
}
