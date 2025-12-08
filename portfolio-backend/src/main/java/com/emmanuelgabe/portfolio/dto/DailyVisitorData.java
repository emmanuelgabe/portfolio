package com.emmanuelgabe.portfolio.dto;

import java.time.LocalDate;

/**
 * DTO for daily visitor data used in dashboard charts.
 */
public record DailyVisitorData(
        LocalDate date,
        long count
) { }
