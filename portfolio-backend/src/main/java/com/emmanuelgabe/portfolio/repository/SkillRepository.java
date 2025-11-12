package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.Skill;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    /**
     * Find all skills ordered by display order ascending
     * @return List of skills sorted by displayOrder
     */
    List<Skill> findAllByOrderByDisplayOrderAsc();

    /**
     * Find skills by category, ordered by display order
     * @param category The skill category to filter by
     * @return List of skills in the specified category, sorted by displayOrder
     */
    List<Skill> findByCategoryOrderByDisplayOrderAsc(SkillCategory category);
}
