package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.config.CvStorageProperties;
import com.emmanuelgabe.portfolio.dto.CvResponse;
import com.emmanuelgabe.portfolio.entity.Cv;
import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.exception.FileStorageException;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.CvMapper;
import com.emmanuelgabe.portfolio.repository.CvRepository;
import com.emmanuelgabe.portfolio.service.CvService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of CV service with file storage and version management
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CvServiceImpl implements CvService {

    private final CvRepository cvRepository;
    private final CvMapper cvMapper;
    private final CvStorageProperties cvStorageProperties;
    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(cvStorageProperties.getUploadDir())
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("[INIT] CV upload directory created - path={}", this.fileStorageLocation);
        } catch (IOException ex) {
            log.error("[INIT] Failed to create CV upload directory - path={}, error={}",
                    this.fileStorageLocation, ex.getMessage(), ex);
            throw new FileStorageException("Could not create CV upload directory", ex);
        }
    }

    @Override
    public CvResponse uploadCv(MultipartFile file, User user) {
        log.debug("[UPLOAD_CV] Starting upload - userId={}, originalFileName={}",
                user.getId(), file.getOriginalFilename());

        validateCvFile(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileName = generateFileName(originalFileName);
        String fileUrl = cvStorageProperties.getBasePath() + "/" + fileName;

        log.debug("[UPLOAD_CV] Generated file name - originalFileName={}, newFileName={}",
                originalFileName, fileName);

        try {
            // Security check: prevent path traversal
            if (fileName.contains("..")) {
                log.warn("[UPLOAD_CV] Invalid file name - fileName={}", fileName);
                throw new FileStorageException("Invalid file name: " + fileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("[UPLOAD_CV] File stored successfully - fileName={}, size={}, path={}",
                    fileName, file.getSize(), targetLocation);

            // Set all existing CVs for this user to not current
            List<Cv> existingCvs = cvRepository.findByUserIdAndCurrent(user.getId(), true);
            if (!existingCvs.isEmpty()) {
                existingCvs.forEach(cv -> {
                    cv.setCurrent(false);
                    log.debug("[UPLOAD_CV] Setting CV to not current - cvId={}", cv.getId());
                });
                cvRepository.saveAll(existingCvs);
                cvRepository.flush(); // Force flush to avoid constraint violation
            }

            // Create new CV entity
            Cv cv = new Cv();
            cv.setUser(user);
            cv.setFileName(fileName);
            cv.setOriginalFileName(originalFileName);
            cv.setFileUrl(fileUrl);
            cv.setFileSize(file.getSize());
            cv.setCurrent(true);

            Cv savedCv = cvRepository.save(cv);

            log.info("[UPLOAD_CV] CV uploaded successfully - cvId={}, userId={}, fileName={}, size={}",
                    savedCv.getId(), user.getId(), fileName, file.getSize());

            return cvMapper.toResponse(savedCv);

        } catch (IOException ex) {
            log.error("[UPLOAD_CV] Failed to store file - fileName={}, error={}",
                    fileName, ex.getMessage(), ex);
            throw new FileStorageException("Could not store CV file: " + fileName, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CvResponse> getCurrentCv(Long userId) {
        log.debug("[GET_CURRENT_CV] Fetching current CV - userId={}", userId);

        Optional<Cv> currentCv = cvRepository.findByUserIdAndCurrentTrue(userId);

        if (currentCv.isPresent()) {
            log.info("[GET_CURRENT_CV] Current CV found - userId={}, cvId={}",
                    userId, currentCv.get().getId());
            return currentCv.map(cvMapper::toResponse);
        } else {
            log.debug("[GET_CURRENT_CV] No current CV found - userId={}", userId);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CvResponse> getCurrentCv() {
        log.debug("[GET_CURRENT_CV] Fetching current CV (public)");

        Optional<Cv> currentCv = cvRepository.findFirstByCurrentTrue();

        if (currentCv.isPresent()) {
            log.info("[GET_CURRENT_CV] Current CV found - cvId={}", currentCv.get().getId());
            return currentCv.map(cvMapper::toResponse);
        } else {
            log.debug("[GET_CURRENT_CV] No current CV found");
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadCurrentCv(Long userId) {
        log.debug("[DOWNLOAD_CV] Starting download - userId={}", userId);

        Cv cv = cvRepository.findByUserIdAndCurrentTrue(userId)
                .orElseThrow(() -> {
                    log.warn("[DOWNLOAD_CV] No current CV found - userId={}", userId);
                    return new ResourceNotFoundException("Cv", "userId", userId);
                });

        try {
            Path filePath = this.fileStorageLocation.resolve(cv.getFileName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("[DOWNLOAD_CV] CV download successful - userId={}, cvId={}, fileName={}",
                        userId, cv.getId(), cv.getFileName());
                return resource;
            } else {
                log.error("[DOWNLOAD_CV] File not readable - userId={}, fileName={}, path={}",
                        userId, cv.getFileName(), filePath);
                throw new FileStorageException("Could not read CV file: " + cv.getFileName());
            }
        } catch (MalformedURLException ex) {
            log.error("[DOWNLOAD_CV] Invalid file path - fileName={}, error={}",
                    cv.getFileName(), ex.getMessage(), ex);
            throw new FileStorageException("Invalid file path for CV: " + cv.getFileName(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadCurrentCv() {
        log.debug("[DOWNLOAD_CV] Starting public download");

        Cv cv = cvRepository.findFirstByCurrentTrue()
                .orElseThrow(() -> {
                    log.warn("[DOWNLOAD_CV] No current CV found");
                    return new ResourceNotFoundException("Cv", "current", true);
                });

        try {
            Path filePath = this.fileStorageLocation.resolve(cv.getFileName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("[DOWNLOAD_CV] CV download successful - cvId={}, fileName={}",
                        cv.getId(), cv.getFileName());
                return resource;
            } else {
                log.error("[DOWNLOAD_CV] File not readable - fileName={}, path={}",
                        cv.getFileName(), filePath);
                throw new FileStorageException("Could not read CV file: " + cv.getFileName());
            }
        } catch (MalformedURLException ex) {
            log.error("[DOWNLOAD_CV] Invalid file path - fileName={}, error={}",
                    cv.getFileName(), ex.getMessage(), ex);
            throw new FileStorageException("Invalid file path for CV: " + cv.getFileName(), ex);
        }
    }

    @Override
    public CvResponse setCurrentCv(Long cvId, User user) {
        log.debug("[SET_CURRENT_CV] Setting CV as current - cvId={}, userId={}", cvId, user.getId());

        Cv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> {
                    log.warn("[SET_CURRENT_CV] CV not found - cvId={}", cvId);
                    return new ResourceNotFoundException("Cv", "id", cvId);
                });

        // Verify ownership
        if (!cv.getUser().getId().equals(user.getId())) {
            log.warn("[SET_CURRENT_CV] Unauthorized access - cvId={}, userId={}, cvOwnerId={}",
                    cvId, user.getId(), cv.getUser().getId());
            throw new IllegalArgumentException("CV does not belong to user");
        }

        // Set all existing CVs for this user to not current
        List<Cv> existingCvs = cvRepository.findByUserIdAndCurrent(user.getId(), true);
        if (!existingCvs.isEmpty()) {
            existingCvs.forEach(existingCv -> {
                existingCv.setCurrent(false);
                log.debug("[SET_CURRENT_CV] Setting CV to not current - cvId={}", existingCv.getId());
            });
            cvRepository.saveAll(existingCvs);
            cvRepository.flush(); // Force flush to avoid constraint violation
        }

        // Set this CV as current
        cv.setCurrent(true);
        Cv savedCv = cvRepository.save(cv);

        log.info("[SET_CURRENT_CV] CV set as current successfully - cvId={}, userId={}",
                cvId, user.getId());

        return cvMapper.toResponse(savedCv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CvResponse> getAllCvs(Long userId) {
        log.debug("[LIST_CV] Fetching all CVs - userId={}", userId);

        List<Cv> cvs = cvRepository.findByUserIdOrderByUploadedAtDesc(userId);

        log.info("[LIST_CV] CVs fetched - userId={}, count={}", userId, cvs.size());

        return cvs.stream()
                .map(cvMapper::toResponse)
                .toList();
    }

    @Override
    public void deleteCv(Long cvId, User user) {
        log.debug("[DELETE_CV] Deleting CV - cvId={}, userId={}", cvId, user.getId());

        Cv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> {
                    log.warn("[DELETE_CV] CV not found - cvId={}", cvId);
                    return new ResourceNotFoundException("Cv", "id", cvId);
                });

        // Verify ownership
        if (!cv.getUser().getId().equals(user.getId())) {
            log.warn("[DELETE_CV] Unauthorized access - cvId={}, userId={}, cvOwnerId={}",
                    cvId, user.getId(), cv.getUser().getId());
            throw new IllegalArgumentException("CV does not belong to user");
        }

        // Check if it's the current CV and if there are other CVs
        if (cv.isCurrent()) {
            List<Cv> allUserCvs = cvRepository.findByUserIdOrderByUploadedAtDesc(user.getId());
            if (allUserCvs.size() > 1) {
                log.warn("[DELETE_CV] Cannot delete current CV when other versions exist - cvId={}, userId={}",
                        cvId, user.getId());
                throw new IllegalArgumentException("Cannot delete current CV. Set another CV as current first.");
            }
        }

        // Delete physical file
        try {
            Path filePath = this.fileStorageLocation.resolve(cv.getFileName()).normalize();
            Files.deleteIfExists(filePath);
            log.debug("[DELETE_CV] Physical file deleted - fileName={}, path={}", cv.getFileName(), filePath);
        } catch (IOException ex) {
            log.error("[DELETE_CV] Failed to delete physical file - fileName={}, error={}",
                    cv.getFileName(), ex.getMessage(), ex);
            // Continue with database deletion even if file deletion fails
        }

        // Delete from database
        cvRepository.delete(cv);

        log.info("[DELETE_CV] CV deleted successfully - cvId={}, userId={}, fileName={}",
                cvId, user.getId(), cv.getFileName());
    }

    /**
     * Validate CV file
     * Checks: file not empty, size limit, extension, PDF magic bytes
     */
    private void validateCvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("[VALIDATION] File is null or empty");
            throw new FileStorageException("File is empty");
        }

        // Validate file size
        if (file.getSize() > cvStorageProperties.getMaxFileSize()) {
            log.warn("[VALIDATION] File too large - size={}, maxSize={}",
                    file.getSize(), cvStorageProperties.getMaxFileSize());
            throw new FileStorageException("File size exceeds maximum allowed size of "
                    + (cvStorageProperties.getMaxFileSize() / 1024 / 1024) + "MB");
        }

        // Validate file extension
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(fileName);

        if (!isAllowedExtension(fileExtension)) {
            log.warn("[VALIDATION] Invalid file extension - fileName={}, extension={}",
                    fileName, fileExtension);
            throw new FileStorageException("File type not allowed. Only PDF files are accepted.");
        }

        // Validate PDF magic bytes
        if (!isPdfFile(file)) {
            log.warn("[VALIDATION] File is not a valid PDF - fileName={}", fileName);
            throw new FileStorageException("File content is not a valid PDF. Detected content does not match PDF format.");
        }

        log.debug("[VALIDATION] CV file validation passed - fileName={}, size={}, extension={}",
                fileName, file.getSize(), fileExtension);
    }

    /**
     * Check if file is a valid PDF by reading magic bytes
     * PDF files start with %PDF (0x25 0x50 0x44 0x46)
     */
    private boolean isPdfFile(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length < 4) {
                return false;
            }

            // Check for PDF magic bytes: %PDF
            return fileBytes[0] == 0x25 // %
                   && fileBytes[1] == 0x50 // P
                   && fileBytes[2] == 0x44 // D
                   && fileBytes[3] == 0x46;   // F

        } catch (IOException ex) {
            log.error("[VALIDATION] Failed to read file bytes - error={}", ex.getMessage(), ex);
            throw new FileStorageException("Could not read file content for validation", ex);
        }
    }

    /**
     * Generate unique file name with timestamp and UUID
     * Pattern: cv_YYYYMMDD_HHmmss_UUID.pdf
     */
    private String generateFileName(String originalFileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(originalFileName);
        return "cv_" + timestamp + "_" + uuid + "." + extension;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private boolean isAllowedExtension(String extension) {
        return Arrays.asList(cvStorageProperties.getAllowedExtensions())
                .contains(extension.toLowerCase());
    }
}
