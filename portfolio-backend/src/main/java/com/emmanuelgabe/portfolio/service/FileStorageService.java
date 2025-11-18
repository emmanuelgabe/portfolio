package com.emmanuelgabe.portfolio.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Service for file storage operations
 */
public interface FileStorageService {

    /**
     * Store uploaded file
     * @param file Multipart file to store
     * @return File name of stored file
     */
    String storeFile(MultipartFile file);

    /**
     * Validate file before storage
     * @param file File to validate
     * @throws com.emmanuelgabe.portfolio.exception.FileStorageException if validation fails
     */
    void validateFile(MultipartFile file);

    /**
     * Get file URL for stored file
     * @param fileName Name of the file
     * @return Full URL to access the file
     */
    String getFileUrl(String fileName);
}
