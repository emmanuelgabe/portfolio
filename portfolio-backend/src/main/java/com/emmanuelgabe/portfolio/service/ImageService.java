package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.config.ImageStorageProperties;
import com.emmanuelgabe.portfolio.dto.ImageUploadResponse;
import com.emmanuelgabe.portfolio.dto.PreparedImageInfo;
import com.emmanuelgabe.portfolio.exception.FileStorageException;
import com.emmanuelgabe.portfolio.exception.FileValidationException;
import com.emmanuelgabe.portfolio.messaging.event.ImageProcessingEvent;
import com.emmanuelgabe.portfolio.messaging.publisher.EventPublisher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

/**
 * Service for handling image upload with async processing.
 * Validates and saves images, then publishes events for background WebP conversion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final ImageStorageProperties storageProperties;
    private final EventPublisher eventPublisher;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            log.info("[INIT] Image upload directory created - path={}", uploadPath);
        } catch (IOException e) {
            log.error("[INIT] Failed to create upload directory - path={}", storageProperties.getUploadDir(), e);
            throw new FileStorageException("Could not create upload directory", e);
        }
    }

    /**
     * Upload project image with async processing.
     */
    public ImageUploadResponse uploadProjectImage(Long projectId, MultipartFile file) {
        log.info("[UPLOAD_IMAGE] Starting upload - projectId={}, fileName={}, size={}",
                projectId, file.getOriginalFilename(), file.getSize());

        validateImageFile(file);

        try {
            deleteProjectImage(projectId);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String baseFileName = "project_" + projectId + "_" + timestamp;

            return processImageAsync(file, baseFileName,
                    ImageProcessingEvent.forProject(projectId,
                            uploadPath.resolve(baseFileName + ".tmp").toString(),
                            uploadPath.resolve(baseFileName + ".webp").toString(),
                            uploadPath.resolve(baseFileName + "_thumb.webp").toString()));

        } catch (IOException e) {
            log.error("[UPLOAD_IMAGE] Failed to upload image - projectId={}", projectId, e);
            throw new FileStorageException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    /**
     * Prepare a carousel image for async processing without publishing the event.
     * Saves the temp file and returns information needed to create the processing event.
     * The caller should save the entity first, then publish the event with the entity ID.
     */
    public PreparedImageInfo prepareCarouselImage(Long projectId, int imageIndex, MultipartFile file) {
        log.info("[PREPARE_IMAGE] Preparing carousel image - projectId={}, index={}, fileName={}",
                projectId, imageIndex, file.getOriginalFilename());

        validateImageFile(file);

        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String baseFileName = "project_" + projectId + "_img" + imageIndex + "_" + timestamp + "_" + uniqueId;

            Path tempPath = uploadPath.resolve(baseFileName + ".tmp");
            Files.write(tempPath, file.getBytes());

            String imageUrl = storageProperties.getBasePath() + "/" + baseFileName + ".webp";
            String thumbnailUrl = storageProperties.getBasePath() + "/" + baseFileName + "_thumb.webp";

            log.info("[PREPARE_IMAGE] Image prepared - projectId={}, tempFile={}", projectId, tempPath);

            return PreparedImageInfo.builder()
                    .imageUrl(imageUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .tempFilePath(tempPath.toString())
                    .optimizedFilePath(uploadPath.resolve(baseFileName + ".webp").toString())
                    .thumbnailFilePath(uploadPath.resolve(baseFileName + "_thumb.webp").toString())
                    .fileSize(file.getSize())
                    .build();

        } catch (IOException e) {
            log.error("[PREPARE_IMAGE] Failed to prepare carousel image - projectId={}", projectId, e);
            throw new FileStorageException("Failed to prepare image: " + e.getMessage(), e);
        }
    }

    /**
     * Prepare an article image for async processing without publishing the event.
     * Saves the temp file and returns information needed to create the processing event.
     * The caller should save the entity first, then publish the event with the entity ID.
     */
    public PreparedImageInfo prepareArticleImage(Long articleId, MultipartFile file) {
        log.info("[PREPARE_IMAGE] Preparing article image - articleId={}, fileName={}",
                articleId, file.getOriginalFilename());

        validateImageFile(file);

        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String baseFileName = "article_" + articleId + "_" + timestamp + "_" + uniqueId;

            Path tempPath = uploadPath.resolve(baseFileName + ".tmp");
            Files.write(tempPath, file.getBytes());

            String imageUrl = storageProperties.getBasePath() + "/" + baseFileName + ".webp";
            String thumbnailUrl = storageProperties.getBasePath() + "/" + baseFileName + "_thumb.webp";

            log.info("[PREPARE_IMAGE] Article image prepared - articleId={}, tempFile={}", articleId, tempPath);

            return PreparedImageInfo.builder()
                    .imageUrl(imageUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .tempFilePath(tempPath.toString())
                    .optimizedFilePath(uploadPath.resolve(baseFileName + ".webp").toString())
                    .thumbnailFilePath(uploadPath.resolve(baseFileName + "_thumb.webp").toString())
                    .fileSize(file.getSize())
                    .build();

        } catch (IOException e) {
            log.error("[PREPARE_IMAGE] Failed to prepare article image - articleId={}", articleId, e);
            throw new FileStorageException("Failed to prepare image: " + e.getMessage(), e);
        }
    }

    /**
     * Upload profile image with async processing.
     */
    public ImageUploadResponse uploadProfileImage(MultipartFile file) {
        log.info("[UPLOAD_IMAGE] Starting profile upload - fileName={}, size={}",
                file.getOriginalFilename(), file.getSize());

        validateImageFile(file);

        try {
            deleteExistingProfileImages();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String baseFileName = "profile_" + timestamp;

            return processImageAsync(file, baseFileName,
                    ImageProcessingEvent.forProfile(
                            uploadPath.resolve(baseFileName + ".tmp").toString(),
                            uploadPath.resolve(baseFileName + ".webp").toString()));

        } catch (IOException e) {
            log.error("[UPLOAD_IMAGE] Failed to upload profile image", e);
            throw new FileStorageException("Failed to upload profile image: " + e.getMessage(), e);
        }
    }

    /**
     * Common async processing: save temp file, publish event, return URLs.
     */
    private ImageUploadResponse processImageAsync(MultipartFile file, String baseFileName,
                                                   ImageProcessingEvent event) throws IOException {
        // Save original bytes to temp file
        Path tempPath = uploadPath.resolve(baseFileName + ".tmp");
        Files.write(tempPath, file.getBytes());

        // Publish event for async processing
        eventPublisher.publishImageEvent(event);

        // Build URLs (files will be created by consumer)
        String imageUrl = storageProperties.getBasePath() + "/" + baseFileName + ".webp";
        String thumbnailUrl = event.getThumbnailFilePath() != null
                ? storageProperties.getBasePath() + "/" + baseFileName + "_thumb.webp"
                : null;

        log.info("[UPLOAD_IMAGE] Image queued for processing - eventId={}, imageUrl={}",
                event.getEventId(), imageUrl);

        return new ImageUploadResponse(imageUrl, thumbnailUrl, file.getSize(), LocalDateTime.now());
    }

    /**
     * Delete a specific project image by its URL.
     */
    public void deleteProjectImageByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            boolean isThumbnail = fileName.contains("_thumb.webp");

            if (isThumbnail) {
                String mainFileName = fileName.replace("_thumb.webp", ".webp");
                deleteFileIfExists(mainFileName);
                deleteFileIfExists(fileName);
            } else {
                deleteFileIfExists(fileName);
                String thumbnailFileName = fileName.replace(".webp", "_thumb.webp");
                deleteFileIfExists(thumbnailFileName);
            }

            log.debug("[DELETE_IMAGE] Project image deleted - imageUrl={}", imageUrl);

        } catch (IOException e) {
            log.warn("[DELETE_IMAGE] Failed to delete project image - imageUrl={}", imageUrl, e);
        }
    }

    /**
     * Delete article image and its thumbnail.
     */
    public void deleteArticleImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            boolean isThumbnail = fileName.contains("_thumb.webp");

            if (isThumbnail) {
                String mainFileName = fileName.replace("_thumb.webp", ".webp");
                deleteFileIfExists(mainFileName);
                deleteFileIfExists(fileName);
            } else {
                deleteFileIfExists(fileName);
                String thumbnailFileName = fileName.replace(".webp", "_thumb.webp");
                deleteFileIfExists(thumbnailFileName);
            }

        } catch (IOException e) {
            log.warn("[DELETE_IMAGE] Failed to delete article image - imageUrl={}", imageUrl, e);
        }
    }

    /**
     * Delete project images by project ID.
     */
    public void deleteProjectImage(Long projectId) {
        log.debug("[DELETE_IMAGE] Deleting images - projectId={}", projectId);

        try {
            Files.list(uploadPath)
                    .filter(path -> path.getFileName().toString().startsWith("project_" + projectId + "_"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.debug("[DELETE_IMAGE] Deleted file - fileName={}", path.getFileName());
                        } catch (IOException e) {
                            log.warn("[DELETE_IMAGE] Failed to delete file - fileName={}", path.getFileName(), e);
                        }
                    });

        } catch (IOException e) {
            log.warn("[DELETE_IMAGE] Failed to list files for deletion - projectId={}", projectId, e);
        }
    }

    /**
     * Delete profile image by filename.
     */
    public void deleteProfileImage(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }

        try {
            deleteFileIfExists(fileName);
            log.debug("[DELETE_IMAGE] Profile image deleted - fileName={}", fileName);
        } catch (IOException e) {
            log.warn("[DELETE_IMAGE] Failed to delete profile image - fileName={}", fileName, e);
        }
    }

    private void deleteExistingProfileImages() {
        try {
            Files.list(uploadPath)
                    .filter(path -> path.getFileName().toString().startsWith("profile_"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.debug("[DELETE_IMAGE] Deleted profile image - fileName={}", path.getFileName());
                        } catch (IOException e) {
                            log.warn("[DELETE_IMAGE] Failed to delete profile image - fileName={}",
                                    path.getFileName(), e);
                        }
                    });
        } catch (IOException e) {
            log.warn("[DELETE_IMAGE] Failed to list profile images for deletion", e);
        }
    }

    private void deleteFileIfExists(String fileName) throws IOException {
        Path filePath = uploadPath.resolve(fileName).normalize();
        if (!filePath.startsWith(uploadPath)) {
            log.warn("[SECURITY] Path traversal attempt detected - fileName={}", fileName);
            return;
        }
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.debug("[DELETE_IMAGE] Deleted file - fileName={}", fileName);
        }
    }

    /**
     * Validate image file for security and format.
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("[VALIDATION] File is null or empty");
            throw new FileValidationException("File is empty. Please select a valid image file.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains("..")) {
            log.warn("[VALIDATION] Path traversal detected - fileName={}", originalFilename);
            throw new FileValidationException("Invalid file name: path traversal detected");
        }

        if (file.getSize() > storageProperties.getMaxFileSize()) {
            log.warn("[VALIDATION] File too large - size={}, maxSize={}",
                    file.getSize(), storageProperties.getMaxFileSize());
            throw new FileValidationException(String.format(
                    "File size exceeds maximum allowed size of %d MB",
                    storageProperties.getMaxFileSize() / (1024 * 1024)
            ));
        }

        if (originalFilename == null || !originalFilename.contains(".")) {
            log.warn("[VALIDATION] Invalid filename - fileName={}", originalFilename);
            throw new FileValidationException("Invalid file name");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!Arrays.asList(storageProperties.getAllowedExtensions()).contains(extension)) {
            log.warn("[VALIDATION] Invalid extension - fileName={}, extension={}", originalFilename, extension);
            throw new FileValidationException("File type not allowed. Only images (JPEG, PNG, WebP) are accepted.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(storageProperties.getAllowedMimeTypes()).contains(contentType)) {
            log.warn("[VALIDATION] Invalid MIME type - contentType={}", contentType);
            throw new FileValidationException("File type not allowed. Only images (JPEG, PNG, WebP) are accepted.");
        }

        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length < 4) {
                throw new FileValidationException("File is too small to be a valid image");
            }

            boolean isValidImage = isJpeg(fileBytes) || isPng(fileBytes) || isWebP(fileBytes);
            if (!isValidImage) {
                log.warn("[VALIDATION] Invalid image magic bytes");
                throw new FileValidationException("File is not a valid image. Only JPEG, PNG, and WebP are accepted.");
            }

        } catch (IOException e) {
            log.error("[VALIDATION] Failed to read file bytes", e);
            throw new FileStorageException("Failed to validate image file", e);
        }

        log.debug("[VALIDATION] Image validation passed - fileName={}, size={}, extension={}",
                originalFilename, file.getSize(), extension);
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && bytes[0] == (byte) 0xFF
                && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == (byte) 0x89
                && bytes[1] == (byte) 0x50
                && bytes[2] == (byte) 0x4E
                && bytes[3] == (byte) 0x47;
    }

    private boolean isWebP(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == (byte) 0x52
                && bytes[1] == (byte) 0x49
                && bytes[2] == (byte) 0x46
                && bytes[3] == (byte) 0x46
                && bytes[8] == (byte) 0x57
                && bytes[9] == (byte) 0x45
                && bytes[10] == (byte) 0x42
                && bytes[11] == (byte) 0x50;
    }
}
