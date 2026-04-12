package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.HealthReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthReportRepository extends JpaRepository<HealthReport, Long> {

    /**
     * Return the most recent reports ordered newest-first.
     * Used by the agent to inject recent context into the next diagnostic prompt
     * (inter-cycle memory) and by the admin UI timeline.
     */
    List<HealthReport> findTopNByOrderByCreatedAtDesc(Pageable pageable);

    Page<HealthReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
