package com.emmanuelgabe.portfolio.entity;

import com.emmanuelgabe.portfolio.healthmonitor.action.HealthCycleType;
import com.emmanuelgabe.portfolio.healthmonitor.action.ReportSeverity;
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
 * Snapshot of a single diagnostic cycle produced by the agent.
 *
 * <p>Stored for historical display, audit, and inter-cycle memory (recent reports
 * are injected into the next LLM prompt so the agent doesn't repeat itself).</p>
 */
@Entity
@Table(name = "health_reports")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class HealthReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle_type", nullable = false, length = 20)
    private HealthCycleType cycleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ReportSeverity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    /** JSON array of anomaly strings produced by the agent. */
    @Column(name = "anomalies_json", columnDefinition = "TEXT")
    private String anomaliesJson;

    /** JSON array of tool names the agent invoked during this cycle. */
    @Column(name = "tool_calls_json", columnDefinition = "TEXT")
    private String toolCallsJson;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
