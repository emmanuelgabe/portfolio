package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSkillRequest;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        log.debug("[LIST_SKILLS] Request received");
        List<SkillResponse> skills = skillService.getAllSkills();
        log.info("[LIST_SKILLS] Success - count={}", skills.size());
        return ResponseEntity.ok(skills);
    }

    /**
     * Get skill by ID
     * @param id Skill ID
     * @return Skill details
     */
    @GetMapping("/{id}")
    public ResponseEntity<SkillResponse> getSkillById(@PathVariable Long id) {
        log.debug("[GET_SKILL] Request received - id={}", id);
        SkillResponse skill = skillService.getSkillById(id);
        log.debug("[GET_SKILL] Success - id={}, name={}", skill.getId(), skill.getName());
        return ResponseEntity.ok(skill);
    }

    /**
     * Get skills by category
     * @param category Skill category
     * @return List of skills in the specified category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<SkillResponse>> getSkillsByCategory(@PathVariable SkillCategory category) {
        log.debug("[LIST_SKILLS_CATEGORY] Request received - category={}", category);
        List<SkillResponse> skills = skillService.getSkillsByCategory(category);
        log.info("[LIST_SKILLS_CATEGORY] Success - category={}, count={}", category, skills.size());
        return ResponseEntity.ok(skills);
    }

    /**
     * Create a new skill (Admin only)
     * @param request Create skill request
     * @return Created skill
     */
    @PostMapping("/admin")
    public ResponseEntity<SkillResponse> createSkill(@Valid @RequestBody CreateSkillRequest request) {
        log.info("[CREATE_SKILL] Request received - name={}", request.getName());
        SkillResponse createdSkill = skillService.createSkill(request);
        log.info("[CREATE_SKILL] Success - id={}, name={}", createdSkill.getId(), createdSkill.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSkill);
    }

    /**
     * Update an existing skill (Admin only)
     * @param id Skill ID
     * @param request Update skill request
     * @return Updated skill
     */
    @PutMapping("/admin/{id}")
    public ResponseEntity<SkillResponse> updateSkill(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSkillRequest request) {
        log.info("[UPDATE_SKILL] Request received - id={}", id);
        SkillResponse updatedSkill = skillService.updateSkill(id, request);
        log.info("[UPDATE_SKILL] Success - id={}, name={}", updatedSkill.getId(), updatedSkill.getName());
        return ResponseEntity.ok(updatedSkill);
    }

    /**
     * Delete a skill (Admin only)
     * @param id Skill ID
     * @return No content
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        log.info("[DELETE_SKILL] Request received - id={}", id);
        skillService.deleteSkill(id);
        log.info("[DELETE_SKILL] Success - id={}", id);
        return ResponseEntity.noContent().build();
    }
}
