package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.Experience;
import com.emmanuelgabe.portfolio.entity.ExperienceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Experience entity operations.
 * Provides methods for retrieving experiences sorted by date and filtered by type.
 */
@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {

    /**
     * Find all experiences ordered by start date descending (most recent first)
     * @return List of all experiences sorted by start date DESC
     */
    List<Experience> findAllByOrderByStartDateDesc();

    /**
     * Find experiences by type ordered by start date descending
     * @param type The experience type to filter by
     * @return List of experiences matching the type, sorted by start date DESC
     */
    List<Experience> findByTypeOrderByStartDateDesc(ExperienceType type);

    /**
     * Find ongoing experiences (where endDate is null) ordered by start date descending
     * @return List of ongoing experiences sorted by start date DESC
     */
    List<Experience> findByEndDateIsNullOrderByStartDateDesc();
}
