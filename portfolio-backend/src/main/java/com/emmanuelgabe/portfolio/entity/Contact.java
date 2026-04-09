package com.emmanuelgabe.portfolio.entity;

import com.emmanuelgabe.portfolio.moderation.ContactCategory;
import com.emmanuelgabe.portfolio.moderation.ModerationVerdict;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Contact form submission with AI moderation results.
 *
 * <p>Stores contact metadata and moderation outcome for admin statistics
 * and history. Email address is intentionally excluded for GDPR compliance
 * (already available in the owner's mailbox).</p>
 *
 * <p>GDPR retention policy: entries older than 12 months should be purged.</p>
 */
@Entity
@Table(name = "contacts")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private ContactCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_verdict", nullable = false, length = 10)
    private ModerationVerdict moderationVerdict;

    @Column(name = "moderation_reason")
    private String moderationReason;

    @Column(name = "confidence_score", nullable = false)
    private double confidenceScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
