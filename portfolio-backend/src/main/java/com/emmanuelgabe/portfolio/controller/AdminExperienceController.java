package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CreateExperienceRequest;
import com.emmanuelgabe.portfolio.dto.ExperienceResponse;
import com.emmanuelgabe.portfolio.dto.UpdateExperienceRequest;
import com.emmanuelgabe.portfolio.service.ExperienceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for admin experience management.
 * Provides CRUD operations for experiences.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/experiences")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminExperienceController {

    private final ExperienceService experienceService;

    /**
     * Get all experiences
     * @return List of all experiences
     */
    @GetMapping
    public ResponseEntity<List<ExperienceResponse>> getAllExperiences() {
        log.debug("[ADMIN_EXPERIENCES] Fetching all experiences");
        List<ExperienceResponse> experiences = experienceService.getAllExperiences();
        log.debug("[ADMIN_EXPERIENCES] Found {} experiences", experiences.size());
        return ResponseEntity.ok(experiences);
    }

    /**
     * Get experience by ID
     * @param id Experience ID
     * @return Experience details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExperienceResponse> getExperienceById(@PathVariable Long id) {
        log.debug("[ADMIN_EXPERIENCES] Fetching experience id={}", id);
        ExperienceResponse experience = experienceService.getExperienceById(id);
        log.debug("[ADMIN_EXPERIENCES] Found experience: {} at {}", experience.getRole(), experience.getCompany());
        return ResponseEntity.ok(experience);
    }

    /**
     * Create a new experience
     * @param request Create experience request
     * @return Created experience
     */
    @PostMapping
    public ResponseEntity<ExperienceResponse> createExperience(@Valid @RequestBody CreateExperienceRequest request) {
        log.info("[ADMIN_EXPERIENCES] Creating experience - company={}, role={}", request.getCompany(), request.getRole());
        ExperienceResponse createdExperience = experienceService.createExperience(request);
        log.info("[ADMIN_EXPERIENCES] Created experience id={}, company={}", createdExperience.getId(), createdExperience.getCompany());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdExperience);
    }

    /**
     * Update an existing experience
     * @param id Experience ID
     * @param request Update experience request
     * @return Updated experience
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExperienceResponse> updateExperience(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExperienceRequest request) {
        log.info("[ADMIN_EXPERIENCES] Updating experience id={}", id);
        ExperienceResponse updatedExperience = experienceService.updateExperience(id, request);
        log.info("[ADMIN_EXPERIENCES] Updated experience id={}, company={}", updatedExperience.getId(), updatedExperience.getCompany());
        return ResponseEntity.ok(updatedExperience);
    }

    /**
     * Delete an experience
     * @param id Experience ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExperience(@PathVariable Long id) {
        log.info("[ADMIN_EXPERIENCES] Deleting experience id={}", id);
        experienceService.deleteExperience(id);
        log.info("[ADMIN_EXPERIENCES] Deleted experience id={}", id);
        return ResponseEntity.noContent().build();
    }
}
