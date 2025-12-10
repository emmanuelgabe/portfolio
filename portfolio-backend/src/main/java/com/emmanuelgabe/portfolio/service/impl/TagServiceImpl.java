package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.audit.Auditable;
import com.emmanuelgabe.portfolio.dto.CreateTagRequest;
import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.dto.UpdateTagRequest;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.mapper.TagMapper;

import static com.emmanuelgabe.portfolio.util.EntityHelper.findOrThrow;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import com.emmanuelgabe.portfolio.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of TagService interface
 * Handles business logic for tag management
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Override
    @Cacheable(value = "tags", key = "'all'")
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        log.debug("[LIST_TAGS] Fetching all tags");
        List<Tag> tags = tagRepository.findAll();
        log.debug("[LIST_TAGS] Found {} tags", tags.size());
        return tags.stream()
                .map(tagMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "tags", key = "#id")
    @Transactional(readOnly = true)
    public TagResponse getTagById(Long id) {
        log.debug("[GET_TAG] Fetching tag - id={}", id);
        Tag tag = findOrThrow(tagRepository.findById(id), "Tag", "id", id);
        return tagMapper.toResponse(tag);
    }

    @Override
    @Cacheable(value = "tags", key = "'name:' + #name")
    @Transactional(readOnly = true)
    public TagResponse getTagByName(String name) {
        log.debug("[GET_TAG] Fetching tag - name={}", name);
        Tag tag = findOrThrow(tagRepository.findByName(name), "Tag", "name", name);
        return tagMapper.toResponse(tag);
    }

    @Override
    @CacheEvict(value = "tags", allEntries = true)
    @Auditable(action = AuditAction.CREATE, entityType = "Tag",
            entityIdExpression = "#result.id", entityNameExpression = "#result.name")
    public TagResponse createTag(CreateTagRequest request) {
        log.debug("[CREATE_TAG] Creating tag - name={}", request.getName());

        // Check if tag with same name already exists
        if (tagRepository.existsByName(request.getName())) {
            log.warn("[CREATE_TAG] Tag already exists - name={}", request.getName());
            throw new IllegalStateException("Tag with name '" + request.getName() + "' already exists");
        }

        Tag tag = tagMapper.toEntity(request);
        Tag savedTag = tagRepository.save(tag);

        log.info("[CREATE_TAG] Tag created - id={}, name={}", savedTag.getId(), savedTag.getName());
        return tagMapper.toResponse(savedTag);
    }

    @Override
    @CacheEvict(value = "tags", allEntries = true)
    @Auditable(action = AuditAction.UPDATE, entityType = "Tag",
            entityIdExpression = "#id", entityNameExpression = "#result.name")
    public TagResponse updateTag(Long id, UpdateTagRequest request) {
        log.debug("[UPDATE_TAG] Updating tag - id={}", id);

        Tag tag = findOrThrow(tagRepository.findById(id), "Tag", "id", id);

        // Check if new name conflicts with existing tag
        if (request.getName() != null && !request.getName().equals(tag.getName())) {
            if (tagRepository.existsByName(request.getName())) {
                log.warn("[UPDATE_TAG] Tag name already exists - name={}", request.getName());
                throw new IllegalStateException("Tag with name '" + request.getName() + "' already exists");
            }
        }

        tagMapper.updateEntityFromRequest(request, tag);

        Tag updatedTag = tagRepository.save(tag);
        log.info("[UPDATE_TAG] Tag updated - id={}, name={}", updatedTag.getId(), updatedTag.getName());
        return tagMapper.toResponse(updatedTag);
    }

    @Override
    @CacheEvict(value = "tags", allEntries = true)
    @Auditable(action = AuditAction.DELETE, entityType = "Tag", entityIdExpression = "#id")
    public void deleteTag(Long id) {
        log.debug("[DELETE_TAG] Deleting tag - id={}", id);

        Tag tag = findOrThrow(tagRepository.findById(id), "Tag", "id", id);
        tagRepository.delete(tag);
        log.info("[DELETE_TAG] Tag deleted - id={}, name={}", id, tag.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        log.debug("[CHECK_TAG] Checking if tag exists - name={}", name);
        return tagRepository.existsByName(name);
    }
}
