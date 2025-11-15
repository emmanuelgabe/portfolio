package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.dto.CreateTagRequest;
import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.dto.UpdateTagRequest;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.TagMapper;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import com.emmanuelgabe.portfolio.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        log.debug("[LIST_TAGS] Fetching all tags");
        List<Tag> tags = tagRepository.findAll();
        log.debug("[LIST_TAGS] Found {} tags", tags.size());
        return tags.stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse getTagById(Long id) {
        log.debug("[GET_TAG] Fetching tag - id={}", id);
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[GET_TAG] Tag not found - id={}", id);
                    return new ResourceNotFoundException("Tag", "id", id);
                });
        return tagMapper.toResponse(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse getTagByName(String name) {
        log.debug("[GET_TAG] Fetching tag - name={}", name);
        Tag tag = tagRepository.findByName(name)
                .orElseThrow(() -> {
                    log.warn("[GET_TAG] Tag not found - name={}", name);
                    return new ResourceNotFoundException("Tag", "name", name);
                });
        return tagMapper.toResponse(tag);
    }

    @Override
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
    public TagResponse updateTag(Long id, UpdateTagRequest request) {
        log.debug("[UPDATE_TAG] Updating tag - id={}", id);

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[UPDATE_TAG] Tag not found - id={}", id);
                    return new ResourceNotFoundException("Tag", "id", id);
                });

        // Check if new name conflicts with existing tag
        if (request.getName() != null && !request.getName().equals(tag.getName())) {
            if (tagRepository.existsByName(request.getName())) {
                log.warn("[UPDATE_TAG] Tag name already exists - name={}", request.getName());
                throw new IllegalStateException("Tag with name '" + request.getName() + "' already exists");
            }
        }

        // Update only provided fields using MapStruct
        tagMapper.updateEntityFromRequest(request, tag);

        Tag updatedTag = tagRepository.save(tag);
        log.info("[UPDATE_TAG] Tag updated - id={}, name={}", updatedTag.getId(), updatedTag.getName());
        return tagMapper.toResponse(updatedTag);
    }

    @Override
    public void deleteTag(Long id) {
        log.debug("[DELETE_TAG] Deleting tag - id={}", id);

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[DELETE_TAG] Tag not found - id={}", id);
                    return new ResourceNotFoundException("Tag", "id", id);
                });

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
