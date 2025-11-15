package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CreateTagRequest;
import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.dto.UpdateTagRequest;

import java.util.List;

/**
 * Service interface for managing tags.
 * Provides business logic for tag operations.
 */
public interface TagService {

    /**
     * Get all tags
     * @return List of all tags
     */
    List<TagResponse> getAllTags();

    /**
     * Get a tag by ID
     * @param id Tag ID
     * @return TagResponse
     * @throws com.emmanuelgabe.portfolio.exception.ResourceNotFoundException if tag not found
     */
    TagResponse getTagById(Long id);

    /**
     * Get a tag by name
     * @param name Tag name
     * @return TagResponse
     * @throws com.emmanuelgabe.portfolio.exception.ResourceNotFoundException if tag not found
     */
    TagResponse getTagByName(String name);

    /**
     * Create a new tag
     * @param request CreateTagRequest with tag data
     * @return Created TagResponse
     * @throws IllegalStateException if tag with same name already exists
     */
    TagResponse createTag(CreateTagRequest request);

    /**
     * Update an existing tag
     * @param id Tag ID
     * @param request UpdateTagRequest with updated data
     * @return Updated TagResponse
     * @throws com.emmanuelgabe.portfolio.exception.ResourceNotFoundException if tag not found
     */
    TagResponse updateTag(Long id, UpdateTagRequest request);

    /**
     * Delete a tag
     * @param id Tag ID
     * @throws com.emmanuelgabe.portfolio.exception.ResourceNotFoundException if tag not found
     */
    void deleteTag(Long id);

    /**
     * Check if a tag exists by name
     * @param name Tag name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);
}
