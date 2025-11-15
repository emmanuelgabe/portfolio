package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSkillRequest;
import com.emmanuelgabe.portfolio.entity.Skill;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.SkillMapper;
import com.emmanuelgabe.portfolio.repository.SkillRepository;
import com.emmanuelgabe.portfolio.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of SkillService interface
 * Handles business logic for skill management
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SkillResponse> getAllSkills() {
        log.debug("[LIST_SKILLS] Fetching all skills");
        List<Skill> skills = skillRepository.findAllByOrderByDisplayOrderAsc();
        log.debug("[LIST_SKILLS] Found {} skills", skills.size());
        return skills.stream()
                .map(skillMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SkillResponse getSkillById(Long id) {
        log.debug("[GET_SKILL] Fetching skill - id={}", id);
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[GET_SKILL] Skill not found - id={}", id);
                    return new ResourceNotFoundException("Skill", "id", id);
                });
        return skillMapper.toResponse(skill);
    }

    @Override
    public SkillResponse createSkill(CreateSkillRequest request) {
        log.debug("[CREATE_SKILL] Creating skill - name={}, category={}", request.getName(), request.getCategory());

        Skill skill = skillMapper.toEntity(request);
        Skill savedSkill = skillRepository.save(skill);

        log.info("[CREATE_SKILL] Skill created - id={}, name={}, category={}",
                savedSkill.getId(), savedSkill.getName(), savedSkill.getCategory());
        return skillMapper.toResponse(savedSkill);
    }

    @Override
    public SkillResponse updateSkill(Long id, UpdateSkillRequest request) {
        log.debug("[UPDATE_SKILL] Updating skill - id={}", id);

        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[UPDATE_SKILL] Skill not found - id={}", id);
                    return new ResourceNotFoundException("Skill", "id", id);
                });

        // Update only provided fields using MapStruct
        skillMapper.updateEntityFromRequest(request, skill);

        Skill updatedSkill = skillRepository.save(skill);
        log.info("[UPDATE_SKILL] Skill updated - id={}, name={}", updatedSkill.getId(), updatedSkill.getName());
        return skillMapper.toResponse(updatedSkill);
    }

    @Override
    public void deleteSkill(Long id) {
        log.debug("[DELETE_SKILL] Deleting skill - id={}", id);

        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[DELETE_SKILL] Skill not found - id={}", id);
                    return new ResourceNotFoundException("Skill", "id", id);
                });

        skillRepository.delete(skill);
        log.info("[DELETE_SKILL] Skill deleted - id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillResponse> getSkillsByCategory(SkillCategory category) {
        log.debug("[LIST_SKILLS_CATEGORY] Fetching skills by category - category={}", category);
        List<Skill> skills = skillRepository.findByCategoryOrderByDisplayOrderAsc(category);
        log.debug("[LIST_SKILLS_CATEGORY] Found {} skills", skills.size());
        return skills.stream()
                .map(skillMapper::toResponse)
                .collect(Collectors.toList());
    }
}
