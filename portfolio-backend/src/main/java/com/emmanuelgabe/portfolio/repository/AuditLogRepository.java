package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity with statistics queries.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>,
        JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    Page<AuditLog> findByUsername(String username, Pageable pageable);

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType "
            + "AND a.entityId = :entityId ORDER BY a.createdAt DESC")
    List<AuditLog> findEntityHistory(@Param("entityType") String entityType,
                                     @Param("entityId") Long entityId);

    // Statistics queries
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a "
            + "WHERE a.createdAt >= :since GROUP BY a.action")
    List<Object[]> countByActionSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.entityType, COUNT(a) FROM AuditLog a "
            + "WHERE a.createdAt >= :since GROUP BY a.entityType")
    List<Object[]> countByEntityTypeSince(@Param("since") LocalDateTime since);

    @Query("SELECT CAST(a.createdAt AS date), COUNT(a) FROM AuditLog a "
            + "WHERE a.createdAt >= :since GROUP BY CAST(a.createdAt AS date) "
            + "ORDER BY CAST(a.createdAt AS date)")
    List<Object[]> countByDateSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.username, COUNT(a) FROM AuditLog a "
            + "WHERE a.createdAt >= :since GROUP BY a.username ORDER BY COUNT(a) DESC")
    List<Object[]> countByUserSince(@Param("since") LocalDateTime since);

    long countBySuccessFalseAndCreatedAtAfter(LocalDateTime since);

    long countByCreatedAtAfter(LocalDateTime since);

    /**
     * Combined statistics query to reduce N+1 problem.
     * Returns: [totalCount, failedCount] in a single query.
     */
    @Query("SELECT COUNT(a), SUM(CASE WHEN a.success = false THEN 1 ELSE 0 END) "
            + "FROM AuditLog a WHERE a.createdAt >= :since")
    Object[] countTotalAndFailedSince(@Param("since") LocalDateTime since);

    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentLogs(Pageable pageable);

    /**
     * Delete audit logs older than the specified date.
     * Used by the cleanup batch job for retention policy.
     *
     * @param cutoffDate logs before this date will be deleted
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :cutoffDate")
    long deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
