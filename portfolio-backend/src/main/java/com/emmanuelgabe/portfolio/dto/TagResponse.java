package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Tag entity.
 * Used to serialize tag information in API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagResponse {

    private Long id;
    private String name;
    private String color;
}
