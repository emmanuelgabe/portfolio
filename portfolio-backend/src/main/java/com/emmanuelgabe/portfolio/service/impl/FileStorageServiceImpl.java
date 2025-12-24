package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.config.FileStorageProperties;
import com.emmanuelgabe.portfolio.exception.FileStorageException;
import com.emmanuelgabe.portfolio.exception.FileValidationException;
import com.emmanuelgabe.portfolio.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

/**
 * Implementation of file storage service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final FileStorageProperties fileStorageProperties;
    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("[INIT] Upload directory created - path={}", this.fileStorageLocation);
        } catch (IOException ex) {
            log.error("[INIT] Failed to create upload directory - path={}, error={}",
                    this.fileStorageLocation, ex.getMessage(), ex);
            throw new FileStorageException("Could not create upload directory", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        validateFile(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String fileName = UUID.randomUUID() + "." + fileExtension;

        log.debug("[CREATE_FILE] Storing file - originalName={}, newName={}, size={}",
                originalFileName, fileName, file.getSize());

        try {
            // Security check: prevent path traversal
            if (fileName.contains("..")) {
                log.warn("[VALIDATION] Invalid file name - fileName={}", fileName);
                throw new FileValidationException("Invalid file name: " + fileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("[CREATE_FILE] File stored successfully - fileName={}, size={}, path={}",
                    fileName, file.getSize(), targetLocation);
            return fileName;

        } catch (IOException ex) {
            log.error("[CREATE_FILE] Failed to store file - fileName={}, error={}",
                    fileName, ex.getMessage(), ex);
            throw new FileStorageException("Could not store file: " + fileName, ex);
        }
    }

    @Override
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("[VALIDATION] File is null or empty");
            throw new FileValidationException("File is empty");
        }

        // Validate file size
        if (file.getSize() > fileStorageProperties.getMaxFileSize()) {
            log.warn("[VALIDATION] File too large - size={}, maxSize={}",
                    file.getSize(), fileStorageProperties.getMaxFileSize());
            throw new FileValidationException("File size exceeds maximum allowed size of "
                    + (fileStorageProperties.getMaxFileSize() / 1024 / 1024) + "MB");
        }

        // Validate file extension
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(fileName);

        if (!isAllowedExtension(fileExtension)) {
            log.warn("[VALIDATION] Invalid file extension - fileName={}, extension={}",
                    fileName, fileExtension);
            throw new FileValidationException("File type not allowed. Allowed types: "
                    + Arrays.toString(fileStorageProperties.getAllowedExtensions()));
        }

        // Validate MIME type by reading file content
        String detectedMimeType = detectMimeType(file);
        if (!isAllowedMimeType(detectedMimeType)) {
            log.warn("[VALIDATION] Invalid MIME type - fileName={}, declaredType={}, detectedType={}",
                    fileName, file.getContentType(), detectedMimeType);
            throw new FileValidationException("File content type not allowed. Detected type: " + detectedMimeType);
        }

        log.debug("[VALIDATION] File validation passed - fileName={}, size={}, extension={}, mimeType={}",
                fileName, file.getSize(), fileExtension, detectedMimeType);
    }

    @Override
    public String getFileUrl(String fileName) {
        return fileStorageProperties.getBasePath() + "/" + fileName;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private boolean isAllowedExtension(String extension) {
        return Arrays.asList(fileStorageProperties.getAllowedExtensions())
                .contains(extension.toLowerCase());
    }

    private boolean isAllowedMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return Arrays.asList(fileStorageProperties.getAllowedMimeTypes())
                .contains(mimeType.toLowerCase());
    }

    private String detectMimeType(MultipartFile file) {
        try {
            // Read magic bytes to detect actual file type
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length < 12) {
                log.warn("[VALIDATION] File too small to detect type - size={}", fileBytes.length);
                return "application/octet-stream";
            }

            // Check magic bytes for common image formats
            if (fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8 && fileBytes[2] == (byte) 0xFF) {
                return "image/jpeg";
            }
            if (fileBytes[0] == (byte) 0x89 && fileBytes[1] == 0x50 && fileBytes[2] == 0x4E && fileBytes[3] == 0x47) {
                return "image/png";
            }
            if (fileBytes[0] == 0x47 && fileBytes[1] == 0x49 && fileBytes[2] == 0x46) {
                return "image/gif";
            }
            if (fileBytes[8] == 0x57 && fileBytes[9] == 0x45 && fileBytes[10] == 0x42 && fileBytes[11] == 0x50) {
                return "image/webp";
            }

            log.warn("[VALIDATION] Unknown file type - first bytes: {}",
                    Arrays.toString(Arrays.copyOf(fileBytes, Math.min(12, fileBytes.length))));
            return "application/octet-stream";

        } catch (IOException ex) {
            log.error("[VALIDATION] Failed to read file bytes - error={}", ex.getMessage(), ex);
            throw new FileStorageException("Could not read file content for validation", ex);
        }
    }
}
