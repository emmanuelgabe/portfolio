package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.ExperienceResponse;
import com.emmanuelgabe.portfolio.entity.ExperienceType;
import com.emmanuelgabe.portfolio.service.ExperienceService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for public experience endpoints.
 * Admin endpoints are in AdminExperienceController under /api/admin/experiences.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/experiences")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService experienceService;

    /**
     * Get all experiences ordered by start date descending
     * @return List of all experiences
     */
    @GetMapping
    public ResponseEntity<List<ExperienceResponse>> getAllExperiences() {
        log.debug("[EXPERIENCES] Fetching all experiences");
        List<ExperienceResponse> experiences = experienceService.getAllExperiences();
        log.debug("[EXPERIENCES] Found {} experiences", experiences.size());
        return ResponseEntity.ok(experiences);
    }

    /**
     * Get experience by ID
     * @param id Experience ID
     * @return Experience details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExperienceResponse> getExperienceById(@PathVariable Long id) {
        log.debug("[EXPERIENCES] Fetching experience id={}", id);
        ExperienceResponse experience = experienceService.getExperienceById(id);
        log.debug("[EXPERIENCES] Found experience: {} at {}", experience.getRole(), experience.getCompany());
        return ResponseEntity.ok(experience);
    }

    /**
     * Get experiences filtered by type
     * @param type Experience type (WORK, EDUCATION, CERTIFICATION, VOLUNTEERING)
     * @return List of experiences of the specified type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<ExperienceResponse>> getExperiencesByType(@PathVariable ExperienceType type) {
        log.debug("[EXPERIENCES] Fetching experiences by type={}", type);
        List<ExperienceResponse> experiences = experienceService.getExperiencesByType(type);
        log.debug("[EXPERIENCES] Found {} experiences of type={}", experiences.size(), type);
        return ResponseEntity.ok(experiences);
    }

    /**
     * Get ongoing experiences (where endDate is null)
     * @return List of ongoing experiences
     */
    @GetMapping("/ongoing")
    public ResponseEntity<List<ExperienceResponse>> getOngoingExperiences() {
        log.debug("[EXPERIENCES] Fetching ongoing experiences");
        List<ExperienceResponse> experiences = experienceService.getOngoingExperiences();
        log.debug("[EXPERIENCES] Found {} ongoing experiences", experiences.size());
        return ResponseEntity.ok(experiences);
    }

    /**
     * Get top N most recent experiences
     * Used for displaying a summary on the home page
     * @param limit Maximum number of experiences to return (default: 3)
     * @return List of most recent experiences
     */
    @GetMapping("/recent")
    public ResponseEntity<List<ExperienceResponse>> getRecentExperiences(
            @RequestParam(defaultValue = "3")
            @Min(value = 1, message = "Limit must be at least 1")
            @Max(value = 50, message = "Limit must not exceed 50")
            int limit) {
        log.debug("[EXPERIENCES] Fetching recent experiences - limit={}", limit);
        List<ExperienceResponse> experiences = experienceService.getRecentExperiences(limit);
        log.debug("[EXPERIENCES] Found {} recent experiences", experiences.size());
        return ResponseEntity.ok(experiences);
    }
}
