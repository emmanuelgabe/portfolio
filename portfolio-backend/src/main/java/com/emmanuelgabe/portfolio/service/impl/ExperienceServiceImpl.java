package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.audit.Auditable;
import com.emmanuelgabe.portfolio.dto.CreateExperienceRequest;
import com.emmanuelgabe.portfolio.dto.ExperienceResponse;
import com.emmanuelgabe.portfolio.dto.UpdateExperienceRequest;
import com.emmanuelgabe.portfolio.entity.Experience;
import com.emmanuelgabe.portfolio.entity.ExperienceType;
import com.emmanuelgabe.portfolio.mapper.ExperienceMapper;

import static com.emmanuelgabe.portfolio.util.EntityHelper.findOrThrow;
import com.emmanuelgabe.portfolio.repository.ExperienceRepository;
import com.emmanuelgabe.portfolio.search.event.ExperienceIndexEvent;
import com.emmanuelgabe.portfolio.service.ExperienceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of ExperienceService interface.
 * Handles business logic for experience management.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ExperienceServiceImpl implements ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final ExperienceMapper experienceMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Cacheable(value = "experiences", key = "'all'")
    @Transactional(readOnly = true)
    public List<ExperienceResponse> getAllExperiences() {
        log.debug("[LIST_EXPERIENCES] Fetching all experiences");
        List<Experience> experiences = experienceRepository.findAllByOrderByStartDateDesc();
        log.debug("[LIST_EXPERIENCES] Found {} experiences", experiences.size());
        return experiences.stream()
                .map(experienceMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "experiences", key = "#id")
    @Transactional(readOnly = true)
    public ExperienceResponse getExperienceById(Long id) {
        log.debug("[GET_EXPERIENCE] Fetching experience - id={}", id);
        Experience experience = findOrThrow(experienceRepository.findById(id), "Experience", "id", id);
        return experienceMapper.toResponse(experience);
    }

    @Override
    @CacheEvict(value = "experiences", allEntries = true)
    @Auditable(action = AuditAction.CREATE, entityType = "Experience",
            entityIdExpression = "#result.id", entityNameExpression = "#result.company + ' - ' + #result.role")
    public ExperienceResponse createExperience(CreateExperienceRequest request) {
        log.debug("[CREATE_EXPERIENCE] Creating experience - company={}, role={}",
                request.getCompany(), request.getRole());

        Experience experience = experienceMapper.toEntity(request);

        // Validate dates
        experience.validateDates();

        Experience savedExperience = experienceRepository.save(experience);
        applicationEventPublisher.publishEvent(ExperienceIndexEvent.forIndex(savedExperience));
        log.info("[CREATE_EXPERIENCE] Experience created - id={}, company={}, role={}, type={}, ongoing={}",
                savedExperience.getId(),
                savedExperience.getCompany(),
                savedExperience.getRole(),
                savedExperience.getType(),
                savedExperience.isOngoing());
        return experienceMapper.toResponse(savedExperience);
    }

    @Override
    @CacheEvict(value = "experiences", allEntries = true)
    @Auditable(action = AuditAction.UPDATE, entityType = "Experience",
            entityIdExpression = "#id", entityNameExpression = "#result.company + ' - ' + #result.role")
    public ExperienceResponse updateExperience(Long id, UpdateExperienceRequest request) {
        log.debug("[UPDATE_EXPERIENCE] Updating experience - id={}", id);

        Experience experience = findOrThrow(experienceRepository.findById(id), "Experience", "id", id);
        experienceMapper.updateEntityFromRequest(request, experience);
        experience.validateDates();

        Experience updatedExperience = experienceRepository.save(experience);
        applicationEventPublisher.publishEvent(ExperienceIndexEvent.forIndex(updatedExperience));
        log.info("[UPDATE_EXPERIENCE] Experience updated - id={}, company={}, role={}",
                updatedExperience.getId(),
                updatedExperience.getCompany(),
                updatedExperience.getRole());
        return experienceMapper.toResponse(updatedExperience);
    }

    @Override
    @CacheEvict(value = "experiences", allEntries = true)
    @Auditable(action = AuditAction.DELETE, entityType = "Experience", entityIdExpression = "#id")
    public void deleteExperience(Long id) {
        log.debug("[DELETE_EXPERIENCE] Deleting experience - id={}", id);

        Experience experience = findOrThrow(experienceRepository.findById(id), "Experience", "id", id);

        experienceRepository.deleteById(id);
        applicationEventPublisher.publishEvent(ExperienceIndexEvent.forRemove(experience));
        log.info("[DELETE_EXPERIENCE] Experience deleted - id={}", id);
    }

    @Override
    @Cacheable(value = "experiences", key = "'type:' + #type.name()")
    @Transactional(readOnly = true)
    public List<ExperienceResponse> getExperiencesByType(ExperienceType type) {
        log.debug("[LIST_EXPERIENCES_BY_TYPE] Fetching experiences - type={}", type);
        List<Experience> experiences = experienceRepository.findByTypeOrderByStartDateDesc(type);
        log.debug("[LIST_EXPERIENCES_BY_TYPE] Found {} experiences for type {}", experiences.size(), type);
        return experiences.stream()
                .map(experienceMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "experiences", key = "'ongoing'")
    @Transactional(readOnly = true)
    public List<ExperienceResponse> getOngoingExperiences() {
        log.debug("[LIST_ONGOING_EXPERIENCES] Fetching ongoing experiences");
        List<Experience> experiences = experienceRepository.findByEndDateIsNullOrderByStartDateDesc();
        log.debug("[LIST_ONGOING_EXPERIENCES] Found {} ongoing experiences", experiences.size());
        return experiences.stream()
                .map(experienceMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "experiences", key = "'recent:' + #limit")
    @Transactional(readOnly = true)
    public List<ExperienceResponse> getRecentExperiences(int limit) {
        log.debug("[LIST_RECENT_EXPERIENCES] Fetching recent experiences - limit={}", limit);
        List<Experience> experiences = experienceRepository.findAllByOrderByStartDateDesc();
        log.debug("[LIST_RECENT_EXPERIENCES] Found {} total experiences, returning top {}",
                experiences.size(), limit);
        return experiences.stream()
                .limit(limit)
                .map(experienceMapper::toResponse)
                .toList();
    }
}
