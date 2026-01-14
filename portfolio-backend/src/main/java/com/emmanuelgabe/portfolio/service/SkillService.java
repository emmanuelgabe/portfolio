package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.ReorderRequest;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSkillRequest;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for Skill operations
 * Provides business logic for skill management
 */
public interface SkillService {

    /**
     * Get all skills ordered by display order
     * @return List of all skills sorted by displayOrder
     */
    List<SkillResponse> getAllSkills();

    /**
     * Get skill by ID
     * @param id Skill ID
     * @return Skill response
     */
    SkillResponse getSkillById(Long id);

    /**
     * Create a new skill
     * @param request Create skill request
     * @return Created skill response
     */
    SkillResponse createSkill(CreateSkillRequest request);

    /**
     * Update an existing skill
     * @param id Skill ID
     * @param request Update skill request
     * @return Updated skill response
     */
    SkillResponse updateSkill(Long id, UpdateSkillRequest request);

    /**
     * Delete a skill
     * @param id Skill ID
     */
    void deleteSkill(Long id);

    /**
     * Get skills by category
     * @param category Skill category
     * @return List of skills in the specified category, sorted by displayOrder
     */
    List<SkillResponse> getSkillsByCategory(SkillCategory category);

    /**
     * Upload a custom SVG icon for a skill
     * @param id Skill ID
     * @param file SVG file to upload
     * @return Updated skill response with custom icon URL
     */
    SkillResponse uploadSkillIcon(Long id, MultipartFile file);

    /**
     * Reorder skills by updating their display order
     * @param request Reorder request with ordered IDs
     */
    void reorderSkills(ReorderRequest request);
}
