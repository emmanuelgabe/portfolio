package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.DailyStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DailyStats entity.
 * Provides methods for querying aggregated portfolio statistics.
 */
@Repository
public interface DailyStatsRepository extends JpaRepository<DailyStats, Long> {

    /**
     * Find stats for a specific date.
     *
     * @param statsDate the date to find stats for
     * @return optional containing stats if found
     */
    Optional<DailyStats> findByStatsDate(LocalDate statsDate);

    /**
     * Check if stats exist for a specific date.
     *
     * @param statsDate the date to check
     * @return true if stats exist for this date
     */
    boolean existsByStatsDate(LocalDate statsDate);

    /**
     * Find stats between two dates ordered by date descending.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of stats for the date range
     */
    List<DailyStats> findByStatsDateBetweenOrderByStatsDateDesc(
            LocalDate startDate, LocalDate endDate);

    /**
     * Find stats with pagination ordered by date descending.
     *
     * @param pageable pagination information
     * @return page of stats
     */
    Page<DailyStats> findAllByOrderByStatsDateDesc(Pageable pageable);

    /**
     * Find the most recent stats entry.
     *
     * @return optional containing the latest stats
     */
    Optional<DailyStats> findTopByOrderByStatsDateDesc();

    /**
     * Find stats for the last N days.
     *
     * @param since the earliest date to include
     * @return list of stats since the given date
     */
    @Query("SELECT d FROM DailyStats d WHERE d.statsDate >= :since ORDER BY d.statsDate DESC")
    List<DailyStats> findStatsSince(@Param("since") LocalDate since);

    /**
     * Delete stats older than a specific date.
     * Used for retention management.
     *
     * @param cutoffDate stats before this date will be deleted
     * @return number of deleted records
     */
    long deleteByStatsDateBefore(LocalDate cutoffDate);

    /**
     * Sum unique visitors between two dates.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return total unique visitors for the period
     */
    @Query("SELECT COALESCE(SUM(d.uniqueVisitors), 0) FROM DailyStats d "
            + "WHERE d.statsDate BETWEEN :startDate AND :endDate")
    long sumUniqueVisitorsBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
