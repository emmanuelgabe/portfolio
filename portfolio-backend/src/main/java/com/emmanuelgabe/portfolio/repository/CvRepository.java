package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.Cv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CV entity operations
 */
@Repository
public interface CvRepository extends JpaRepository<Cv, Long> {

    /**
     * Find the current CV for a user
     * @param userId User ID
     * @return Optional containing the current CV if exists
     */
    Optional<Cv> findByUserIdAndCurrentTrue(Long userId);

    /**
     * Find all CVs for a user, ordered by upload date descending
     * @param userId User ID
     * @return List of CVs ordered by most recent first
     */
    List<Cv> findByUserIdOrderByUploadedAtDesc(Long userId);

    /**
     * Find all current CVs for a user (should be only one due to unique constraint)
     * @param userId User ID
     * @return List of current CVs
     */
    List<Cv> findByUserIdAndCurrent(Long userId, boolean current);

    /**
     * Find the current CV (any user - for public portfolio endpoint)
     * @return Optional containing the current CV if exists
     */
    Optional<Cv> findFirstByCurrentTrue();
}
