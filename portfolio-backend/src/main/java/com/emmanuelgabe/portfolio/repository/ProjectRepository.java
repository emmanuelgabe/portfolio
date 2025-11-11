package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Find all featured projects
     * @return List of featured projects
     */
    List<Project> findByFeaturedTrue();

    /**
     * Find projects by title containing a specific string (case-insensitive)
     * @param title The title to search for
     * @return List of projects matching the title
     */
    List<Project> findByTitleContainingIgnoreCase(String title);

    /**
     * Find projects by technology in tech stack
     * Uses custom query to search within the techStack field
     * @param technology The technology to search for
     * @return List of projects using the specified technology
     */
    @Query("SELECT p FROM Project p WHERE LOWER(p.techStack) LIKE LOWER(CONCAT('%', :technology, '%'))")
    List<Project> findByTechnology(@Param("technology") String technology);

    /**
     * Find projects by tag name
     * @param tagName The tag name to search for
     * @return List of projects with the specified tag
     */
    @Query("SELECT DISTINCT p FROM Project p JOIN p.tags t WHERE LOWER(t.name) = LOWER(:tagName)")
    List<Project> findByTagName(@Param("tagName") String tagName);
}
