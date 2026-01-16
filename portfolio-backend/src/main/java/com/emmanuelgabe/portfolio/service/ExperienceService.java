package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CreateExperienceRequest;
import com.emmanuelgabe.portfolio.dto.ExperienceResponse;
import com.emmanuelgabe.portfolio.dto.ReorderRequest;
import com.emmanuelgabe.portfolio.dto.UpdateExperienceRequest;
import com.emmanuelgabe.portfolio.entity.ExperienceType;

import java.util.List;

/**
 * Service interface for Experience operations.
 * Provides business logic for managing professional, educational, certification, and volunteering experiences.
 */
public interface ExperienceService {

    /**
     * Get all experiences ordered by display order (manual) then start date descending
     * @return List of all experiences
     */
    List<ExperienceResponse> getAllExperiences();

    /**
     * Get experience by ID
     * @param id Experience ID
     * @return Experience response
     */
    ExperienceResponse getExperienceById(Long id);

    /**
     * Create a new experience
     * @param request Create experience request
     * @return Created experience response
     */
    ExperienceResponse createExperience(CreateExperienceRequest request);

    /**
     * Update an existing experience
     * @param id Experience ID
     * @param request Update experience request
     * @return Updated experience response
     */
    ExperienceResponse updateExperience(Long id, UpdateExperienceRequest request);

    /**
     * Delete an experience
     * @param id Experience ID
     */
    void deleteExperience(Long id);

    /**
     * Reorder experiences by updating their display order
     * @param request Reorder request with ordered IDs
     */
    void reorderExperiences(ReorderRequest request);


    /**
     * Get experiences filtered by type
     * @param type Experience type
     * @return List of experiences of the specified type
     */
    List<ExperienceResponse> getExperiencesByType(ExperienceType type);

    /**
     * Get ongoing experiences (where endDate is null)
     * @return List of ongoing experiences
     */
    List<ExperienceResponse> getOngoingExperiences();

    /**
     * Get top N most recent experiences
     * Used for displaying a summary on the home page
     * @param limit Maximum number of experiences to return
     * @return List of most recent experiences
     */
    List<ExperienceResponse> getRecentExperiences(int limit);
}
