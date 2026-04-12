package com.emmanuelgabe.portfolio.entity;

import com.emmanuelgabe.portfolio.healthmonitor.action.ActionLevel;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionStatus;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * An action proposed by the diagnostic agent, awaiting admin decision.
 *
 * <p>Status transitions are driven by explicit compare-and-set queries on
 * {@link com.emmanuelgabe.portfolio.repository.HealthProposedActionRepository}
 * so double-clicks on approve cannot execute the same action twice.
 * The {@link Version} field adds a second layer of optimistic locking.</p>
 */
@Entity
@Table(name = "health_proposed_actions")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class HealthProposedAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActionLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private ActionType actionType;

    /** JSON-serialized action parameters (schema depends on {@link #actionType}). */
    @Column(name = "params_json", columnDefinition = "TEXT")
    private String paramsJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ActionStatus status;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /** JSON-serialized execution result (output, error, metadata). */
    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ActionStatus.PENDING;
        }
    }
}
