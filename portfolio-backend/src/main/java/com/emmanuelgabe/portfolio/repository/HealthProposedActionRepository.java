package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.HealthProposedAction;
import com.emmanuelgabe.portfolio.healthmonitor.action.ActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HealthProposedActionRepository extends JpaRepository<HealthProposedAction, Long> {

    List<HealthProposedAction> findByStatusOrderByCreatedAtDesc(ActionStatus status);

    List<HealthProposedAction> findByReportIdOrderByCreatedAtAsc(Long reportId);

    List<HealthProposedAction> findByStatusAndExpiresAtBefore(ActionStatus status, LocalDateTime cutoff);

    Page<HealthProposedAction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<HealthProposedAction> findTop10ByStatusOrderByCreatedAtDesc(ActionStatus status);

    long countByStatusAndApprovedAtAfter(ActionStatus status, LocalDateTime since);

    /**
     * Atomic compare-and-set transition: updates status only if the current
     * value still matches {@code expectedStatus}. Returns the number of rows
     * updated (0 or 1). Guards against double-click races on approve/reject.
     */
    @Modifying
    @Query("UPDATE HealthProposedAction a SET a.status = :newStatus, "
            + "a.approvedBy = :actor, a.approvedAt = :now "
            + "WHERE a.id = :id AND a.status = :expectedStatus")
    int tryApprove(@Param("id") Long id,
                   @Param("expectedStatus") ActionStatus expectedStatus,
                   @Param("newStatus") ActionStatus newStatus,
                   @Param("actor") String actor,
                   @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE HealthProposedAction a SET a.status = :newStatus, "
            + "a.approvedBy = :actor, a.approvedAt = :now, a.rejectionReason = :reason "
            + "WHERE a.id = :id AND a.status = :expectedStatus")
    int tryReject(@Param("id") Long id,
                  @Param("expectedStatus") ActionStatus expectedStatus,
                  @Param("newStatus") ActionStatus newStatus,
                  @Param("actor") String actor,
                  @Param("now") LocalDateTime now,
                  @Param("reason") String reason);

    /**
     * Bulk-expire actions that were never processed within their validity window.
     * Returns the number of expired rows.
     */
    @Modifying
    @Query("UPDATE HealthProposedAction a SET a.status = "
            + "com.emmanuelgabe.portfolio.healthmonitor.action.ActionStatus.EXPIRED "
            + "WHERE a.status = com.emmanuelgabe.portfolio.healthmonitor.action.ActionStatus.PENDING "
            + "AND a.expiresAt < :now")
    int expireStaleActions(@Param("now") LocalDateTime now);
}
