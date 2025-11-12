package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSkillRequest;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    /**
     * Get all skills ordered by display order
     * @return List of all skills
     */
    @GetMapping
    public ResponseEntity<List<SkillResponse>> getAllSkills() {
        List<SkillResponse> skills = skillService.getAllSkills();
        return ResponseEntity.ok(skills);
    }

    /**
     * Get skill by ID
     * @param id Skill ID
     * @return Skill details
     */
    @GetMapping("/{id}")
    public ResponseEntity<SkillResponse> getSkillById(@PathVariable Long id) {
        SkillResponse skill = skillService.getSkillById(id);
        return ResponseEntity.ok(skill);
    }

    /**
     * Get skills by category
     * @param category Skill category
     * @return List of skills in the specified category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<SkillResponse>> getSkillsByCategory(@PathVariable SkillCategory category) {
        List<SkillResponse> skills = skillService.getSkillsByCategory(category);
        return ResponseEntity.ok(skills);
    }

    /**
     * Create a new skill (Admin only)
     * @param request Create skill request
     * @return Created skill
     */
    @PostMapping("/admin")
    public ResponseEntity<SkillResponse> createSkill(@Valid @RequestBody CreateSkillRequest request) {
        SkillResponse createdSkill = skillService.createSkill(request);
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
        SkillResponse updatedSkill = skillService.updateSkill(id, request);
        return ResponseEntity.ok(updatedSkill);
    }

    /**
     * Delete a skill (Admin only)
     * @param id Skill ID
     * @return No content
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }
}
