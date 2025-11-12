package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSkillRequest;
import com.emmanuelgabe.portfolio.entity.Skill;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    /**
     * Get all skills ordered by display order
     * @return List of all skills sorted by displayOrder
     */
    @Transactional(readOnly = true)
    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(SkillResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get skill by ID
     * @param id Skill ID
     * @return Skill response
     * @throws ResourceNotFoundException if skill not found
     */
    @Transactional(readOnly = true)
    public SkillResponse getSkillById(Long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", id));
        return SkillResponse.fromEntity(skill);
    }

    /**
     * Create a new skill
     * @param request Create skill request
     * @return Created skill response
     */
    public SkillResponse createSkill(CreateSkillRequest request) {
        Skill skill = new Skill();
        skill.setName(request.getName());
        skill.setIcon(request.getIcon());
        skill.setColor(request.getColor());
        skill.setCategory(request.getCategory());
        skill.setLevel(request.getLevel());
        skill.setDisplayOrder(request.getDisplayOrder());

        Skill savedSkill = skillRepository.save(skill);
        return SkillResponse.fromEntity(savedSkill);
    }

    /**
     * Update an existing skill
     * @param id Skill ID
     * @param request Update skill request
     * @return Updated skill response
     * @throws ResourceNotFoundException if skill not found
     */
    public SkillResponse updateSkill(Long id, UpdateSkillRequest request) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", id));

        // Update only provided fields
        if (request.getName() != null) {
            skill.setName(request.getName());
        }
        if (request.getIcon() != null) {
            skill.setIcon(request.getIcon());
        }
        if (request.getColor() != null) {
            skill.setColor(request.getColor());
        }
        if (request.getCategory() != null) {
            skill.setCategory(request.getCategory());
        }
        if (request.getLevel() != null) {
            skill.setLevel(request.getLevel());
        }
        if (request.getDisplayOrder() != null) {
            skill.setDisplayOrder(request.getDisplayOrder());
        }

        Skill updatedSkill = skillRepository.save(skill);
        return SkillResponse.fromEntity(updatedSkill);
    }

    /**
     * Delete a skill
     * @param id Skill ID
     * @throws ResourceNotFoundException if skill not found
     */
    public void deleteSkill(Long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", id));
        skillRepository.delete(skill);
    }

    /**
     * Get skills by category
     * @param category Skill category
     * @return List of skills in the specified category, sorted by displayOrder
     */
    @Transactional(readOnly = true)
    public List<SkillResponse> getSkillsByCategory(SkillCategory category) {
        return skillRepository.findByCategoryOrderByDisplayOrderAsc(category).stream()
                .map(SkillResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
