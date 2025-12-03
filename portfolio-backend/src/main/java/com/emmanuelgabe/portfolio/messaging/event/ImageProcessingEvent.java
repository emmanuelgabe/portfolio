package com.emmanuelgabe.portfolio.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Event for asynchronous image processing.
 * Contains all information needed to process an image in the background.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageProcessingEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String EVENT_TYPE = "IMAGE_PROCESSING";

    private String eventId;
    private Instant createdAt;
    private ImageProcessingType processingType;
    private Long entityId;
    private Long projectImageId;
    private Long articleImageId;
    private String tempFilePath;
    private String optimizedFilePath;
    private String thumbnailFilePath;
    private int imageIndex;

    /**
     * Image processing type determines the processing strategy.
     */
    public enum ImageProcessingType {
        PROJECT,
        PROJECT_CAROUSEL,
        ARTICLE,
        PROFILE
    }

    public String getEventType() {
        return EVENT_TYPE;
    }

    /**
     * Factory method to create a project image processing event.
     */
    public static ImageProcessingEvent forProject(Long projectId, String tempFilePath,
                                                   String optimizedFilePath, String thumbnailFilePath) {
        return ImageProcessingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .processingType(ImageProcessingType.PROJECT)
                .entityId(projectId)
                .tempFilePath(tempFilePath)
                .optimizedFilePath(optimizedFilePath)
                .thumbnailFilePath(thumbnailFilePath)
                .imageIndex(0)
                .build();
    }

    /**
     * Factory method to create a carousel image processing event.
     */
    public static ImageProcessingEvent forCarousel(Long projectId, Long projectImageId, int imageIndex,
                                                    String tempFilePath, String optimizedFilePath,
                                                    String thumbnailFilePath) {
        return ImageProcessingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .processingType(ImageProcessingType.PROJECT_CAROUSEL)
                .entityId(projectId)
                .projectImageId(projectImageId)
                .tempFilePath(tempFilePath)
                .optimizedFilePath(optimizedFilePath)
                .thumbnailFilePath(thumbnailFilePath)
                .imageIndex(imageIndex)
                .build();
    }

    /**
     * Factory method to create an article image processing event.
     */
    public static ImageProcessingEvent forArticle(Long articleId, Long articleImageId, String tempFilePath,
                                                   String optimizedFilePath, String thumbnailFilePath) {
        return ImageProcessingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .processingType(ImageProcessingType.ARTICLE)
                .entityId(articleId)
                .articleImageId(articleImageId)
                .tempFilePath(tempFilePath)
                .optimizedFilePath(optimizedFilePath)
                .thumbnailFilePath(thumbnailFilePath)
                .imageIndex(0)
                .build();
    }

    /**
     * Factory method to create a profile image processing event.
     */
    public static ImageProcessingEvent forProfile(String tempFilePath, String optimizedFilePath) {
        return ImageProcessingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .processingType(ImageProcessingType.PROFILE)
                .entityId(null)
                .tempFilePath(tempFilePath)
                .optimizedFilePath(optimizedFilePath)
                .thumbnailFilePath(null)
                .imageIndex(0)
                .build();
    }
}
