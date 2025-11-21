package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.ProjectImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ProjectImage entity operations.
 */
@Repository
public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {

    /**
     * Find all images for a project ordered by display order.
     */
    List<ProjectImage> findByProjectIdOrderByDisplayOrderAsc(Long projectId);

    /**
     * Find the primary image for a project.
     */
    Optional<ProjectImage> findByProjectIdAndPrimaryTrue(Long projectId);

    /**
     * Count images for a project.
     */
    int countByProjectId(Long projectId);

    /**
     * Clear primary flag for all images of a project.
     */
    @Modifying
    @Query("UPDATE ProjectImage pi SET pi.primary = false WHERE pi.project.id = :projectId")
    void clearPrimaryForProject(@Param("projectId") Long projectId);

    /**
     * Decrement display order for images after a deleted image.
     */
    @Modifying
    @Query("UPDATE ProjectImage pi SET pi.displayOrder = pi.displayOrder - 1 "
            + "WHERE pi.project.id = :projectId AND pi.displayOrder > :deletedOrder")
    void decrementOrderAfter(@Param("projectId") Long projectId, @Param("deletedOrder") int deletedOrder);

    /**
     * Find image by project ID and image ID.
     */
    Optional<ProjectImage> findByIdAndProjectId(Long imageId, Long projectId);
}
