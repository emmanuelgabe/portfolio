package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.config.ImageStorageProperties;
import com.emmanuelgabe.portfolio.dto.ImageUploadResponse;
import com.emmanuelgabe.portfolio.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Service for handling image upload, optimization, and thumbnail generation
 * Supports WebP conversion for optimal file sizes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final ImageStorageProperties storageProperties;

    private Path uploadPath;

    /**
     * Initialize upload directory on service startup
     */
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
     * Upload and optimize project image
     * Generates both full-size optimized image and thumbnail
     *
     * @param projectId Project ID
     * @param file Image file to upload
     * @return ImageUploadResponse containing URLs and metadata
     * @throws FileStorageException if upload fails
     */
    public ImageUploadResponse uploadProjectImage(Long projectId, MultipartFile file) {
        log.info("[UPLOAD_IMAGE] Starting upload - projectId={}, originalFileName={}, size={}",
                projectId, file.getOriginalFilename(), file.getSize());

        // Validate image file
        validateImageFile(file);

        try {
            // Delete old images if they exist
            deleteProjectImage(projectId);

            // Generate filenames
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String baseFileName = "project_" + projectId + "_" + timestamp;
            String optimizedFileName = baseFileName + ".webp";
            String thumbnailFileName = baseFileName + "_thumb.webp";

            Path optimizedPath = uploadPath.resolve(optimizedFileName);
            Path thumbnailPath = uploadPath.resolve(thumbnailFileName);

            // Read original image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (originalImage == null) {
                throw new FileStorageException("Could not read image file. File may be corrupted.");
            }

            // Generate optimized image (max width 1200px, WebP format)
            generateOptimizedImage(originalImage, optimizedPath);

            // Generate thumbnail (300x300px square crop, WebP format)
            generateThumbnail(originalImage, thumbnailPath);

            // Get file size
            long fileSize = Files.size(optimizedPath);

            // Build URLs
            String imageUrl = storageProperties.getBasePath() + "/" + optimizedFileName;
            String thumbnailUrl = storageProperties.getBasePath() + "/" + thumbnailFileName;

            log.info("[UPLOAD_IMAGE] Images uploaded successfully - projectId={}, imageSize={}, thumbnailSize={}",
                    projectId, fileSize, Files.size(thumbnailPath));

            return new ImageUploadResponse(imageUrl, thumbnailUrl, fileSize, LocalDateTime.now());

        } catch (IOException e) {
            log.error("[UPLOAD_IMAGE] Failed to upload image - projectId={}", projectId, e);
            throw new FileStorageException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    /**
     * Upload and optimize article image
     * Generates both full-size optimized image and thumbnail
     *
     * @param articleId Article ID
     * @param file Image file to upload
     * @return ImageUploadResponse containing URLs and metadata
     * @throws FileStorageException if upload fails
     */
    public ImageUploadResponse uploadArticleImage(Long articleId, MultipartFile file) {
        log.info("[UPLOAD_IMAGE] Starting upload - articleId={}, originalFileName={}, size={}",
                articleId, file.getOriginalFilename(), file.getSize());

        // Validate image file
        validateImageFile(file);

        try {
            // Generate filenames
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String baseFileName = "article_" + articleId + "_" + timestamp;
            String optimizedFileName = baseFileName + ".webp";
            String thumbnailFileName = baseFileName + "_thumb.webp";

            Path optimizedPath = uploadPath.resolve(optimizedFileName);
            Path thumbnailPath = uploadPath.resolve(thumbnailFileName);

            // Read original image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (originalImage == null) {
                throw new FileStorageException("Could not read image file. File may be corrupted.");
            }

            // Generate optimized image (max width 1200px, WebP format)
            generateOptimizedImage(originalImage, optimizedPath);

            // Generate thumbnail (300x300px square crop, WebP format)
            generateThumbnail(originalImage, thumbnailPath);

            // Get file size
            long fileSize = Files.size(optimizedPath);

            // Build URLs
            String imageUrl = storageProperties.getBasePath() + "/" + optimizedFileName;
            String thumbnailUrl = storageProperties.getBasePath() + "/" + thumbnailFileName;

            log.info("[UPLOAD_IMAGE] Images uploaded successfully - articleId={}, imageSize={}, thumbnailSize={}",
                    articleId, fileSize, Files.size(thumbnailPath));

            return new ImageUploadResponse(imageUrl, thumbnailUrl, fileSize, LocalDateTime.now());

        } catch (IOException e) {
            log.error("[UPLOAD_IMAGE] Failed to upload image - articleId={}", articleId, e);
            throw new FileStorageException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    /**
     * Upload and optimize project carousel image with 16:9 aspect ratio.
     * Generates both 16:9 cropped image and square thumbnail.
     *
     * @param projectId Project ID
     * @param imageIndex Index of this image in the project gallery
     * @param file Image file to upload
     * @return ImageUploadResponse containing URLs and metadata
     * @throws FileStorageException if upload fails
     */
    public ImageUploadResponse uploadProjectCarouselImage(Long projectId, int imageIndex, MultipartFile file) {
        log.info("[UPLOAD_IMAGE] Starting carousel image upload - projectId={}, index={}, fileName={}",
                projectId, imageIndex, file.getOriginalFilename());

        validateImageFile(file);

        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));
            String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
            String baseFileName = "project_" + projectId + "_img" + imageIndex + "_" + timestamp + "_" + uniqueId;
            String optimizedFileName = baseFileName + ".webp";
            String thumbnailFileName = baseFileName + "_thumb.webp";

            Path optimizedPath = uploadPath.resolve(optimizedFileName);
            Path thumbnailPath = uploadPath.resolve(thumbnailFileName);

            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (originalImage == null) {
                throw new FileStorageException("Could not read image file. File may be corrupted.");
            }

            // Generate 16:9 aspect ratio image for carousel
            generate16x9Image(originalImage, optimizedPath);

            // Generate square thumbnail for project cards
            generateThumbnail(originalImage, thumbnailPath);

            long fileSize = Files.size(optimizedPath);

            String imageUrl = storageProperties.getBasePath() + "/" + optimizedFileName;
            String thumbnailUrl = storageProperties.getBasePath() + "/" + thumbnailFileName;

            log.info("[UPLOAD_IMAGE] Carousel image uploaded - projectId={}, index={}, size={}",
                    projectId, imageIndex, fileSize);

            return new ImageUploadResponse(imageUrl, thumbnailUrl, fileSize, LocalDateTime.now());

        } catch (IOException e) {
            log.error("[UPLOAD_IMAGE] Failed to upload carousel image - projectId={}", projectId, e);
            throw new FileStorageException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a specific project image by its URL.
     * Deletes both the main image and its thumbnail.
     *
     * @param imageUrl URL of the image to delete
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
     * Delete article image and its associated thumbnail.
     * Handles both main image URL and thumbnail URL correctly.
     *
     * @param imageUrl URL of the image to delete (can be main image or thumbnail)
     */
    public void deleteArticleImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            // Extract filename from URL
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

            // Determine if this is a thumbnail or main image
            boolean isThumbnail = fileName.contains("_thumb.webp");

            if (isThumbnail) {
                // If thumbnail URL passed, derive main image name and delete both
                String mainFileName = fileName.replace("_thumb.webp", ".webp");
                deleteFileIfExists(mainFileName);
                deleteFileIfExists(fileName);
            } else {
                // If main image URL passed, delete main image and its thumbnail
                deleteFileIfExists(fileName);
                String thumbnailFileName = fileName.replace(".webp", "_thumb.webp");
                deleteFileIfExists(thumbnailFileName);
            }

        } catch (IOException e) {
            log.warn("[DELETE_IMAGE] Failed to delete article image - imageUrl={}", imageUrl, e);
        }
    }

    /**
     * Helper method to delete a file if it exists.
     */
    private void deleteFileIfExists(String fileName) throws IOException {
        Path filePath = uploadPath.resolve(fileName);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.debug("[DELETE_IMAGE] Deleted file - fileName={}", fileName);
        }
    }

    /**
     * Delete project images (both optimized and thumbnail)
     *
     * @param projectId Project ID
     */
    public void deleteProjectImage(Long projectId) {
        log.debug("[DELETE_IMAGE] Deleting images - projectId={}", projectId);

        try {
            // Find and delete all files matching pattern project_{id}_*
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
     * Validate image file
     * Checks: null/empty, size, extension, MIME type, magic bytes, path traversal
     *
     * @param file File to validate
     * @throws FileStorageException if validation fails
     */
    private void validateImageFile(MultipartFile file) {
        // Check null or empty
        if (file == null || file.isEmpty()) {
            log.warn("[VALIDATION] File is null or empty");
            throw new FileStorageException("File is empty. Please select a valid image file.");
        }

        // Security check: prevent path traversal
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains("..")) {
            log.warn("[VALIDATION] Path traversal detected - fileName={}", originalFilename);
            throw new FileStorageException("Invalid file name: path traversal detected");
        }

        // Check file size
        if (file.getSize() > storageProperties.getMaxFileSize()) {
            log.warn("[VALIDATION] File too large - size={}, maxSize={}",
                    file.getSize(), storageProperties.getMaxFileSize());
            throw new FileStorageException(String.format(
                    "File size exceeds maximum allowed size of %d MB",
                    storageProperties.getMaxFileSize() / (1024 * 1024)
            ));
        }

        // Check extension
        if (originalFilename == null || !originalFilename.contains(".")) {
            log.warn("[VALIDATION] Invalid filename - fileName={}", originalFilename);
            throw new FileStorageException("Invalid file name");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!Arrays.asList(storageProperties.getAllowedExtensions()).contains(extension)) {
            log.warn("[VALIDATION] Invalid file extension - fileName={}, extension={}", originalFilename, extension);
            throw new FileStorageException(
                    "File type not allowed. Only images (JPEG, PNG, WebP) are accepted."
            );
        }

        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(storageProperties.getAllowedMimeTypes()).contains(contentType)) {
            log.warn("[VALIDATION] Invalid MIME type - contentType={}", contentType);
            throw new FileStorageException("File type not allowed. Only images (JPEG, PNG, WebP) are accepted.");
        }

        // Validate magic bytes
        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length < 4) {
                throw new FileStorageException("File is too small to be a valid image");
            }

            boolean isValidImage = isJpeg(fileBytes) || isPng(fileBytes) || isWebP(fileBytes);
            if (!isValidImage) {
                log.warn("[VALIDATION] Invalid image magic bytes");
                throw new FileStorageException("File is not a valid image. Only JPEG, PNG, and WebP are accepted.");
            }

        } catch (IOException e) {
            log.error("[VALIDATION] Failed to read file bytes", e);
            throw new FileStorageException("Failed to validate image file", e);
        }

        log.debug("[VALIDATION] Image file validation passed - fileName={}, size={}, extension={}",
                originalFilename, file.getSize(), extension);
    }

    /**
     * Generate optimized image with max width and WebP conversion
     *
     * @param originalImage Original image
     * @param targetPath Target file path
     * @throws IOException if optimization fails
     */
    private void generateOptimizedImage(BufferedImage originalImage, Path targetPath) throws IOException {
        int originalWidth = originalImage.getWidth();
        int maxWidth = storageProperties.getImageMaxWidth();

        if (originalWidth > maxWidth) {
            // Resize maintaining aspect ratio
            Thumbnails.of(originalImage)
                    .width(maxWidth)
                    .outputFormat("webp")
                    .outputQuality(storageProperties.getJpegQuality())
                    .toFile(targetPath.toFile());
        } else {
            // No resizing needed, just convert to WebP
            Thumbnails.of(originalImage)
                    .scale(1.0)
                    .outputFormat("webp")
                    .outputQuality(storageProperties.getJpegQuality())
                    .toFile(targetPath.toFile());
        }

        log.debug("[OPTIMIZE] Optimized image generated - width={}, path={}",
                Math.min(originalWidth, maxWidth), targetPath.getFileName());
    }

    /**
     * Generate 16:9 aspect ratio image with center crop.
     * Used for carousel images to ensure consistent display.
     *
     * @param originalImage Original image
     * @param targetPath Target file path
     * @throws IOException if optimization fails
     */
    private void generate16x9Image(BufferedImage originalImage, Path targetPath) throws IOException {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate 16:9 crop dimensions
        double targetRatio = 16.0 / 9.0;
        double originalRatio = (double) originalWidth / originalHeight;

        int cropWidth;
        int cropHeight;
        int x;
        int y;

        if (originalRatio > targetRatio) {
            // Image is wider than 16:9 - crop width
            cropHeight = originalHeight;
            cropWidth = (int) (originalHeight * targetRatio);
            x = (originalWidth - cropWidth) / 2;
            y = 0;
        } else {
            // Image is taller than 16:9 - crop height
            cropWidth = originalWidth;
            cropHeight = (int) (originalWidth / targetRatio);
            x = 0;
            y = (originalHeight - cropHeight) / 2;
        }

        BufferedImage croppedImage = originalImage.getSubimage(x, y, cropWidth, cropHeight);

        int maxWidth = storageProperties.getImageMaxWidth();
        if (cropWidth > maxWidth) {
            Thumbnails.of(croppedImage)
                    .width(maxWidth)
                    .outputFormat("webp")
                    .outputQuality(storageProperties.getJpegQuality())
                    .toFile(targetPath.toFile());
        } else {
            Thumbnails.of(croppedImage)
                    .scale(1.0)
                    .outputFormat("webp")
                    .outputQuality(storageProperties.getJpegQuality())
                    .toFile(targetPath.toFile());
        }

        log.debug("[OPTIMIZE] 16:9 image generated - width={}, path={}",
                Math.min(cropWidth, maxWidth), targetPath.getFileName());
    }

    /**
     * Generate square thumbnail with center crop
     *
     * @param originalImage Original image
     * @param targetPath Target file path
     * @throws IOException if thumbnail generation fails
     */
    private void generateThumbnail(BufferedImage originalImage, Path targetPath) throws IOException {
        int size = storageProperties.getThumbnailSize();

        // Crop to square from center first, then resize
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int cropSize = Math.min(originalWidth, originalHeight);
        int x = (originalWidth - cropSize) / 2;
        int y = (originalHeight - cropSize) / 2;

        BufferedImage croppedImage = originalImage.getSubimage(x, y, cropSize, cropSize);

        Thumbnails.of(croppedImage)
                .size(size, size)
                .outputFormat("webp")
                .outputQuality(storageProperties.getThumbnailQuality())
                .toFile(targetPath.toFile());

        log.debug("[THUMBNAIL] Thumbnail generated - size={}x{}, path={}",
                size, size, targetPath.getFileName());
    }

    /**
     * Check if file is JPEG (magic bytes: 0xFF 0xD8 0xFF)
     */
    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && bytes[0] == (byte) 0xFF
                && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF;
    }

    /**
     * Check if file is PNG (magic bytes: 0x89 0x50 0x4E 0x47)
     */
    private boolean isPng(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == (byte) 0x89
                && bytes[1] == (byte) 0x50
                && bytes[2] == (byte) 0x4E
                && bytes[3] == (byte) 0x47;
    }

    /**
     * Check if file is WebP (magic bytes: 0x52 0x49 0x46 0x46 ... 0x57 0x45 0x42 0x50)
     */
    private boolean isWebP(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == (byte) 0x52 // 'R'
                && bytes[1] == (byte) 0x49 // 'I'
                && bytes[2] == (byte) 0x46 // 'F'
                && bytes[3] == (byte) 0x46 // 'F'
                && bytes[8] == (byte) 0x57 // 'W'
                && bytes[9] == (byte) 0x45 // 'E'
                && bytes[10] == (byte) 0x42 // 'B'
                && bytes[11] == (byte) 0x50;  // 'P'
    }

    /**
     * Upload and optimize profile image for site configuration.
     * Generates optimized image (no thumbnail needed for profile).
     *
     * @param file Image file to upload
     * @return ImageUploadResponse containing URL and metadata
     * @throws FileStorageException if upload fails
     */
    public ImageUploadResponse uploadProfileImage(MultipartFile file) {
        log.info("[UPLOAD_IMAGE] Starting profile image upload - fileName={}, size={}",
                file.getOriginalFilename(), file.getSize());

        validateImageFile(file);

        try {
            // Delete existing profile images
            deleteExistingProfileImages();

            // Generate filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "profile_" + timestamp + ".webp";
            Path targetPath = uploadPath.resolve(fileName);

            // Read original image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (originalImage == null) {
                throw new FileStorageException("Could not read image file. File may be corrupted.");
            }

            // Generate optimized square image (max 500px for profile)
            generateProfileImage(originalImage, targetPath);

            long fileSize = Files.size(targetPath);
            String imageUrl = storageProperties.getBasePath() + "/" + fileName;

            log.info("[UPLOAD_IMAGE] Profile image uploaded - fileName={}, size={}", fileName, fileSize);

            return new ImageUploadResponse(imageUrl, null, fileSize, LocalDateTime.now());

        } catch (IOException e) {
            log.error("[UPLOAD_IMAGE] Failed to upload profile image", e);
            throw new FileStorageException("Failed to upload profile image: " + e.getMessage(), e);
        }
    }

    /**
     * Delete profile image by filename.
     *
     * @param fileName Profile image filename to delete
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

    /**
     * Delete all existing profile images.
     */
    private void deleteExistingProfileImages() {
        try {
            Files.list(uploadPath)
                    .filter(path -> path.getFileName().toString().startsWith("profile_"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.debug("[DELETE_IMAGE] Deleted existing profile image - fileName={}",
                                    path.getFileName());
                        } catch (IOException e) {
                            log.warn("[DELETE_IMAGE] Failed to delete profile image - fileName={}",
                                    path.getFileName(), e);
                        }
                    });
        } catch (IOException e) {
            log.warn("[DELETE_IMAGE] Failed to list profile images for deletion", e);
        }
    }

    /**
     * Generate optimized square profile image.
     *
     * @param originalImage Original image
     * @param targetPath Target file path
     * @throws IOException if optimization fails
     */
    private void generateProfileImage(BufferedImage originalImage, Path targetPath) throws IOException {
        int maxSize = 500; // Max profile image size

        // Crop to square from center
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int cropSize = Math.min(originalWidth, originalHeight);
        int x = (originalWidth - cropSize) / 2;
        int y = (originalHeight - cropSize) / 2;

        BufferedImage croppedImage = originalImage.getSubimage(x, y, cropSize, cropSize);

        // Resize if needed and convert to WebP
        if (cropSize > maxSize) {
            Thumbnails.of(croppedImage)
                    .size(maxSize, maxSize)
                    .outputFormat("webp")
                    .outputQuality(storageProperties.getJpegQuality())
                    .toFile(targetPath.toFile());
        } else {
            Thumbnails.of(croppedImage)
                    .scale(1.0)
                    .outputFormat("webp")
                    .outputQuality(storageProperties.getJpegQuality())
                    .toFile(targetPath.toFile());
        }

        log.debug("[OPTIMIZE] Profile image generated - size={}x{}, path={}",
                Math.min(cropSize, maxSize), Math.min(cropSize, maxSize), targetPath.getFileName());
    }
}
