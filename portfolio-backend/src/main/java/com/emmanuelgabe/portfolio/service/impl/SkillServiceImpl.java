package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSkillRequest;
import com.emmanuelgabe.portfolio.entity.IconType;
import com.emmanuelgabe.portfolio.entity.Skill;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.SkillMapper;
import com.emmanuelgabe.portfolio.repository.SkillRepository;
import com.emmanuelgabe.portfolio.service.SkillService;
import com.emmanuelgabe.portfolio.service.SvgStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final SvgStorageService svgStorageService;

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

        // Delete associated SVG icon if exists
        if (skill.getIconType() == IconType.CUSTOM_SVG && skill.getCustomIconUrl() != null) {
            svgStorageService.deleteIconByUrl(skill.getCustomIconUrl());
        }

        skillRepository.delete(skill);
        log.info("[DELETE_SKILL] Skill deleted - id={}", id);
    }

    /**
     * Upload a custom SVG icon for a skill
     *
     * @param id Skill ID
     * @param file SVG file to upload
     * @return Updated skill response
     */
    @Override
    public SkillResponse uploadSkillIcon(Long id, MultipartFile file) {
        log.debug("[UPLOAD_SKILL_ICON] Uploading icon - skillId={}", id);

        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[UPLOAD_SKILL_ICON] Skill not found - id={}", id);
                    return new ResourceNotFoundException("Skill", "id", id);
                });

        // Upload SVG and get URL
        String iconUrl = svgStorageService.uploadSkillIcon(id, file);

        // Update skill with custom icon
        skill.setIconType(IconType.CUSTOM_SVG);
        skill.setCustomIconUrl(iconUrl);
        skill.setIcon(null); // Clear Font Awesome icon

        Skill updatedSkill = skillRepository.save(skill);
        log.info("[UPLOAD_SKILL_ICON] Icon uploaded - skillId={}, iconUrl={}", id, iconUrl);
        return skillMapper.toResponse(updatedSkill);
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
