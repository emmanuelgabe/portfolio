package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.ReorderRequest;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSkillRequest;
import com.emmanuelgabe.portfolio.service.SkillService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for admin skill management.
 * Provides CRUD operations for skills.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/skills")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSkillController {

    private final SkillService skillService;

    /**
     * Get all skills
     * @return List of all skills
     */
    @GetMapping
    public ResponseEntity<List<SkillResponse>> getAllSkills() {
        log.debug("[ADMIN_SKILLS] Fetching all skills");
        List<SkillResponse> skills = skillService.getAllSkills();
        log.debug("[ADMIN_SKILLS] Found {} skills", skills.size());
        return ResponseEntity.ok(skills);
    }

    /**
     * Get skill by ID
     * @param id Skill ID
     * @return Skill details
     */
    @GetMapping("/{id}")
    public ResponseEntity<SkillResponse> getSkillById(@PathVariable Long id) {
        log.debug("[ADMIN_SKILLS] Fetching skill id={}", id);
        SkillResponse skill = skillService.getSkillById(id);
        log.debug("[ADMIN_SKILLS] Found skill: {}", skill.getName());
        return ResponseEntity.ok(skill);
    }

    /**
     * Create a new skill
     * @param request Create skill request
     * @return Created skill
     */
    @PostMapping
    public ResponseEntity<SkillResponse> createSkill(@Valid @RequestBody CreateSkillRequest request) {
        log.info("[ADMIN_SKILLS] Creating skill - name={}", request.getName());
        SkillResponse createdSkill = skillService.createSkill(request);
        log.info("[ADMIN_SKILLS] Created skill id={}, name={}", createdSkill.getId(), createdSkill.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSkill);
    }

    /**
     * Update an existing skill
     * @param id Skill ID
     * @param request Update skill request
     * @return Updated skill
     */
    @PutMapping("/{id}")
    public ResponseEntity<SkillResponse> updateSkill(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSkillRequest request) {
        log.info("[ADMIN_SKILLS] Updating skill id={}", id);
        SkillResponse updatedSkill = skillService.updateSkill(id, request);
        log.info("[ADMIN_SKILLS] Updated skill id={}, name={}", updatedSkill.getId(), updatedSkill.getName());
        return ResponseEntity.ok(updatedSkill);
    }

    /**
     * Delete a skill
     * @param id Skill ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        log.info("[ADMIN_SKILLS] Deleting skill id={}", id);
        skillService.deleteSkill(id);
        log.info("[ADMIN_SKILLS] Deleted skill id={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Upload a custom SVG icon for a skill
     * @param id Skill ID
     * @param file SVG file to upload
     * @return Updated skill with custom icon URL
     */
    @PostMapping("/{id}/icon")
    public ResponseEntity<SkillResponse> uploadSkillIcon(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        log.info("[ADMIN_SKILLS] Uploading icon for skill id={}", id);
        SkillResponse updatedSkill = skillService.uploadSkillIcon(id, file);
        log.info("[ADMIN_SKILLS] Icon uploaded for skill id={}, iconUrl={}",
                updatedSkill.getId(), updatedSkill.getCustomIconUrl());
        return ResponseEntity.ok(updatedSkill);
    }

    /**
     * Reorder skills
     * @param request Reorder request with ordered IDs
     * @return No content
     */
    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderSkills(@Valid @RequestBody ReorderRequest request) {
        log.info("[ADMIN_SKILLS] Reordering skills - count={}", request.getOrderedIds().size());
        skillService.reorderSkills(request);
        log.info("[ADMIN_SKILLS] Skills reordered");
        return ResponseEntity.noContent().build();
    }
}
