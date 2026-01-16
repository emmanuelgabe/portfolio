package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.config.ImageStorageProperties;
import com.emmanuelgabe.portfolio.exception.FileStorageException;
import com.emmanuelgabe.portfolio.messaging.event.ImageProcessingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service responsible for actual image processing operations.
 * Used by ImageProcessingConsumer for async processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProcessorService {

    private final ImageStorageProperties storageProperties;

    /**
     * Process an image based on the event type.
     * Reads from temp file, processes, saves to final location, deletes temp.
     *
     * @param event the image processing event containing paths and options
     * @throws FileStorageException if processing fails
     */
    public void processImage(ImageProcessingEvent event) {
        log.info("[IMAGE_PROCESSOR] Processing image - eventId={}, type={}",
                event.getEventId(), event.getProcessingType());

        Path tempPath = Paths.get(event.getTempFilePath());

        try {
            BufferedImage originalImage = ImageIO.read(tempPath.toFile());
            if (originalImage == null) {
                throw new FileStorageException("Could not read temp image file");
            }

            switch (event.getProcessingType()) {
                case PROJECT -> processProjectImage(originalImage, event);
                case PROJECT_CAROUSEL -> processCarouselImage(originalImage, event);
                case ARTICLE -> processArticleImage(originalImage, event);
                case PROFILE -> processProfileImage(originalImage, event);
            }

            // Preserve or delete original file based on configuration
            preserveOrDeleteOriginal(tempPath, event);

            log.info("[IMAGE_PROCESSOR] Image processed successfully - eventId={}, type={}",
                    event.getEventId(), event.getProcessingType());

        } catch (IOException e) {
            log.error("[IMAGE_PROCESSOR] Failed to process image - eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            throw new FileStorageException("Failed to process image: " + e.getMessage(), e);
        }
    }

    private void processProjectImage(BufferedImage image, ImageProcessingEvent event) throws IOException {
        Path optimizedPath = Paths.get(event.getOptimizedFilePath());
        Path thumbnailPath = Paths.get(event.getThumbnailFilePath());

        generateOptimizedImage(image, optimizedPath);
        generateThumbnail(image, thumbnailPath);
    }

    private void processCarouselImage(BufferedImage image, ImageProcessingEvent event) throws IOException {
        Path optimizedPath = Paths.get(event.getOptimizedFilePath());
        Path thumbnailPath = Paths.get(event.getThumbnailFilePath());

        generateOptimizedImage(image, optimizedPath);
        generateThumbnail(image, thumbnailPath);
    }

    private void processArticleImage(BufferedImage image, ImageProcessingEvent event) throws IOException {
        Path optimizedPath = Paths.get(event.getOptimizedFilePath());
        Path thumbnailPath = Paths.get(event.getThumbnailFilePath());

        generateOptimizedImage(image, optimizedPath);
        generateThumbnail(image, thumbnailPath);
    }

    private void processProfileImage(BufferedImage image, ImageProcessingEvent event) throws IOException {
        Path optimizedPath = Paths.get(event.getOptimizedFilePath());
        generateProfileImage(image, optimizedPath);
    }

    /**
     * Generate optimized image with max width and WebP conversion.
     */
    private void generateOptimizedImage(BufferedImage originalImage, Path targetPath) throws IOException {
        int originalWidth = originalImage.getWidth();
        int maxWidth = storageProperties.getImageMaxWidth();

        if (originalWidth > maxWidth) {
            Thumbnails.of(originalImage)
                    .width(maxWidth)
                    .outputFormat("webp")
                    .outputQuality(storageProperties.getJpegQuality())
                    .toFile(targetPath.toFile());
        } else {
            Thumbnails.of(originalImage)
                    .scale(1.0)
                    .outputFormat("webp")
                    .outputQuality(storageProperties.getJpegQuality())
                    .toFile(targetPath.toFile());
        }

        log.debug("[IMAGE_PROCESSOR] Optimized image generated - width={}, path={}",
                Math.min(originalWidth, maxWidth), targetPath.getFileName());
    }

    /**
     * Generate 16:9 aspect ratio image with center crop.
     */
    private void generate16x9Image(BufferedImage originalImage, Path targetPath) throws IOException {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double targetRatio = 16.0 / 9.0;
        double originalRatio = (double) originalWidth / originalHeight;

        int cropWidth;
        int cropHeight;
        int x;
        int y;

        if (originalRatio > targetRatio) {
            cropHeight = originalHeight;
            cropWidth = (int) (originalHeight * targetRatio);
            x = (originalWidth - cropWidth) / 2;
            y = 0;
        } else {
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

        log.debug("[IMAGE_PROCESSOR] 16:9 image generated - width={}, path={}",
                Math.min(cropWidth, maxWidth), targetPath.getFileName());
    }

    /**
     * Generate square thumbnail with center crop.
     */
    private void generateThumbnail(BufferedImage originalImage, Path targetPath) throws IOException {
        int size = storageProperties.getThumbnailSize();

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

        log.debug("[IMAGE_PROCESSOR] Thumbnail generated - size={}x{}, path={}",
                size, size, targetPath.getFileName());
    }

    /**
     * Generate optimized square profile image.
     */
    private void generateProfileImage(BufferedImage originalImage, Path targetPath) throws IOException {
        int maxSize = 500;

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int cropSize = Math.min(originalWidth, originalHeight);
        int x = (originalWidth - cropSize) / 2;
        int y = (originalHeight - cropSize) / 2;

        BufferedImage croppedImage = originalImage.getSubimage(x, y, cropSize, cropSize);

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

        log.debug("[IMAGE_PROCESSOR] Profile image generated - size={}x{}, path={}",
                Math.min(cropSize, maxSize), Math.min(cropSize, maxSize), targetPath.getFileName());
    }

    /**
     * Preserve original file for future reprocessing or delete it.
     * When keepOriginals is enabled, renames .tmp to .original for batch reprocessing.
     */
    private void preserveOrDeleteOriginal(Path tempPath, ImageProcessingEvent event) throws IOException {
        if (storageProperties.isKeepOriginals()) {
            Path originalPath = getOriginalPath(tempPath);
            Files.move(tempPath, originalPath);
            log.debug("[IMAGE_PROCESSOR] Original preserved - path={}", originalPath.getFileName());
        } else {
            Files.deleteIfExists(tempPath);
            log.debug("[IMAGE_PROCESSOR] Temp file deleted - path={}", tempPath.getFileName());
        }
    }

    /**
     * Convert temp path to original path (.tmp -> .original).
     */
    private Path getOriginalPath(Path tempPath) {
        String fileName = tempPath.getFileName().toString();
        String originalFileName = fileName.replace(".tmp", ".original");
        return tempPath.getParent().resolve(originalFileName);
    }

    /**
     * Reprocess an existing image from its original file.
     * Used by batch job for bulk reprocessing when quality settings change.
     *
     * @param originalFilePath path to the .original file
     * @param optimizedFilePath path where to save the reprocessed .webp
     * @param thumbnailFilePath path where to save the thumbnail (nullable for profile)
     * @param isCarousel true if the image should be processed as 16:9 carousel
     * @throws FileStorageException if reprocessing fails
     */
    public void reprocessFromOriginal(Path originalFilePath, Path optimizedFilePath,
                                      Path thumbnailFilePath, boolean isCarousel) {
        log.debug("[IMAGE_PROCESSOR] Reprocessing from original - source={}", originalFilePath.getFileName());

        try {
            BufferedImage originalImage = ImageIO.read(originalFilePath.toFile());
            if (originalImage == null) {
                throw new FileStorageException("Could not read original image file: " + originalFilePath);
            }

            generateOptimizedImage(originalImage, optimizedFilePath);

            if (thumbnailFilePath != null) {
                generateThumbnail(originalImage, thumbnailFilePath);
            }

            log.debug("[IMAGE_PROCESSOR] Reprocessing completed - output={}", optimizedFilePath.getFileName());

        } catch (IOException e) {
            log.error("[IMAGE_PROCESSOR] Failed to reprocess image - source={}, error={}",
                    originalFilePath.getFileName(), e.getMessage(), e);
            throw new FileStorageException("Failed to reprocess image: " + e.getMessage(), e);
        }
    }

    /**
     * Check if an original file exists for a given WebP image URL.
     *
     * @param imageUrl the WebP image URL
     * @return true if the .original file exists
     */
    public boolean hasOriginalFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return false;
        }
        Path originalPath = resolveOriginalPathFromUrl(imageUrl);
        return Files.exists(originalPath);
    }

    /**
     * Resolve the original file path from a WebP image URL.
     *
     * @param imageUrl the WebP image URL (e.g., /uploads/projects/project_1_xxx.webp)
     * @return the path to the .original file
     */
    public Path resolveOriginalPathFromUrl(String imageUrl) {
        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        String originalFileName = fileName.replace(".webp", ".original");
        Path uploadPath = Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();
        return uploadPath.resolve(originalFileName);
    }

    /**
     * Resolve the optimized file path from a WebP image URL.
     *
     * @param imageUrl the WebP image URL
     * @return the path to the .webp file
     */
    public Path resolveOptimizedPathFromUrl(String imageUrl) {
        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        Path uploadPath = Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();
        return uploadPath.resolve(fileName);
    }

    /**
     * Resolve the thumbnail file path from a WebP image URL.
     *
     * @param thumbnailUrl the thumbnail URL (e.g., /uploads/projects/project_1_xxx_thumb.webp)
     * @return the path to the thumbnail file, or null if URL is blank
     */
    public Path resolveThumbnailPathFromUrl(String thumbnailUrl) {
        if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
            return null;
        }
        String fileName = thumbnailUrl.substring(thumbnailUrl.lastIndexOf("/") + 1);
        Path uploadPath = Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();
        return uploadPath.resolve(fileName);
    }
}
