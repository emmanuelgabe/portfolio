package com.emmanuelgabe.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    @Size(max = 500, message = "Tech stack cannot exceed 500 characters")
    private String techStack;

    @Pattern(regexp = "^(https?://)?([\\w\\-]+\\.)+[\\w\\-]+(/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]*)?$|^$",
            message = "GitHub URL must be a valid URL")
    @Size(max = 255, message = "GitHub URL cannot exceed 255 characters")
    private String githubUrl;

    @Pattern(regexp = "^(https?://)?([\\w\\-]+\\.)+[\\w\\-]+(/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]*)?$|^/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+$|^$",
            message = "Image URL must be a valid URL or path")
    @Size(max = 255, message = "Image URL cannot exceed 255 characters")
    private String imageUrl;

    @Pattern(regexp = "^(https?://)?([\\w\\-]+\\.)+[\\w\\-]+(/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]*)?$|^$",
            message = "Demo URL must be a valid URL")
    @Size(max = 255, message = "Demo URL cannot exceed 255 characters")
    private String demoUrl;

    private boolean featured = false;

    private boolean hasDetails = true;

    private Set<Long> tagIds;
}
