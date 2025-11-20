package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for public skill endpoints.
 * Admin endpoints are in AdminSkillController under /api/admin/skills.
 */
@Slf4j
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    /**
     * Get all skills ordered by display order
     * @return List of all skills
     */
    @GetMapping
    public ResponseEntity<List<SkillResponse>> getAllSkills() {
        log.debug("[SKILLS] Fetching all skills");
        List<SkillResponse> skills = skillService.getAllSkills();
        log.debug("[SKILLS] Found {} skills", skills.size());
        return ResponseEntity.ok(skills);
    }

    /**
     * Get skill by ID
     * @param id Skill ID
     * @return Skill details
     */
    @GetMapping("/{id}")
    public ResponseEntity<SkillResponse> getSkillById(@PathVariable Long id) {
        log.debug("[SKILLS] Fetching skill id={}", id);
        SkillResponse skill = skillService.getSkillById(id);
        log.debug("[SKILLS] Found skill: {}", skill.getName());
        return ResponseEntity.ok(skill);
    }

    /**
     * Get skills by category
     * @param category Skill category
     * @return List of skills in the specified category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<SkillResponse>> getSkillsByCategory(@PathVariable SkillCategory category) {
        log.debug("[SKILLS] Fetching skills by category={}", category);
        List<SkillResponse> skills = skillService.getSkillsByCategory(category);
        log.debug("[SKILLS] Found {} skills in category={}", skills.size(), category);
        return ResponseEntity.ok(skills);
    }
}
