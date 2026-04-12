package com.emmanuelgabe.portfolio.dto;

import com.emmanuelgabe.portfolio.moderation.ContactCategory;
import com.emmanuelgabe.portfolio.moderation.ModerationVerdict;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for contact history entries in the admin panel.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactHistoryResponse {

    private Long id;
    private String name;
    private String subject;
    private String message;
    private ContactCategory category;
    private ModerationVerdict moderationVerdict;
    private String moderationReason;
    private double confidenceScore;
    private LocalDateTime createdAt;
}
