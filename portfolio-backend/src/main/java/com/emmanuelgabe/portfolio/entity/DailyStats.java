package com.emmanuelgabe.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DailyStats entity for storing aggregated portfolio statistics.
 * Generated daily by the stats aggregation batch job.
 */
@Entity
@Table(name = "daily_stats")
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = "id")
public class DailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stats_date", nullable = false, unique = true)
    private LocalDate statsDate;

    @Column(name = "total_projects", nullable = false)
    private long totalProjects;

    @Column(name = "total_articles", nullable = false)
    private long totalArticles;

    @Column(name = "published_articles", nullable = false)
    private long publishedArticles;

    @Column(name = "draft_articles", nullable = false)
    private long draftArticles;

    @Column(name = "total_skills", nullable = false)
    private long totalSkills;

    @Column(name = "total_experiences", nullable = false)
    private long totalExperiences;

    @Column(name = "total_tags", nullable = false)
    private long totalTags;

    @Column(name = "total_project_images", nullable = false)
    private long totalProjectImages;

    @Column(name = "total_article_images", nullable = false)
    private long totalArticleImages;

    @Column(name = "contact_submissions", nullable = false)
    private long contactSubmissions;

    @Column(name = "audit_events_count", nullable = false)
    private long auditEventsCount;

    @Column(name = "failed_audit_events", nullable = false)
    private long failedAuditEvents;

    @Column(name = "unique_visitors", nullable = false)
    private long uniqueVisitors;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public DailyStats(LocalDate statsDate) {
        this.statsDate = statsDate;
    }
}
