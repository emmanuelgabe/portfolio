package com.emmanuelgabe.portfolio.dto.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO for experience search results.
 * Contains essential fields for displaying search results in admin panel.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceSearchResult {

    private Long id;
    private String company;
    private String role;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
}
