package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CvResponse;
import com.emmanuelgabe.portfolio.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for CV management operations
 */
public interface CvService {

    /**
     * Upload a new CV file for a user
     * Automatically sets the new CV as current and marks previous CV as not current
     * @param file CV file to upload (PDF only)
     * @param user User uploading the CV
     * @return CvResponse containing uploaded CV information
     */
    CvResponse uploadCv(MultipartFile file, User user);

    /**
     * Get the current CV for a user
     * @param userId User ID
     * @return Optional containing current CV if exists
     */
    Optional<CvResponse> getCurrentCv(Long userId);

    /**
     * Get the current CV (public endpoint - any user)
     * Used for public portfolio display
     * @return Optional containing current CV if exists
     */
    Optional<CvResponse> getCurrentCv();

    /**
     * Get the current CV file as a downloadable resource
     * @param userId User ID
     * @return Resource containing the CV file
     */
    Resource downloadCurrentCv(Long userId);

    /**
     * Get the current CV file as a downloadable resource (public endpoint)
     * @return Resource containing the CV file
     */
    Resource downloadCurrentCv();

    /**
     * Set a specific CV as current for a user
     * Marks all other CVs as not current
     * @param cvId CV ID to set as current
     * @param user User owning the CV
     * @return CvResponse of the newly current CV
     */
    CvResponse setCurrentCv(Long cvId, User user);

    /**
     * Get all CVs for a user, ordered by upload date descending
     * @param userId User ID
     * @return List of all CVs for the user
     */
    List<CvResponse> getAllCvs(Long userId);

    /**
     * Delete a CV
     * Cannot delete the current CV unless it's the only one
     * @param cvId CV ID to delete
     * @param user User owning the CV
     */
    void deleteCv(Long cvId, User user);
}
