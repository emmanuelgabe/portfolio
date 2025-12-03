package com.emmanuelgabe.portfolio.messaging.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageProcessingEventTest {

    // ========== Factory Method Tests ==========

    @Test
    void should_createProjectEvent_when_forProjectCalled() {
        // Arrange
        Long projectId = 1L;
        String tempPath = "/tmp/test.tmp";
        String optimizedPath = "/uploads/test.webp";
        String thumbnailPath = "/uploads/test_thumb.webp";

        // Act
        ImageProcessingEvent event = ImageProcessingEvent.forProject(
                projectId, tempPath, optimizedPath, thumbnailPath);

        // Assert
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getProcessingType()).isEqualTo(ImageProcessingEvent.ImageProcessingType.PROJECT);
        assertThat(event.getEntityId()).isEqualTo(projectId);
        assertThat(event.getTempFilePath()).isEqualTo(tempPath);
        assertThat(event.getOptimizedFilePath()).isEqualTo(optimizedPath);
        assertThat(event.getThumbnailFilePath()).isEqualTo(thumbnailPath);
        assertThat(event.getImageIndex()).isEqualTo(0);
    }

    @Test
    void should_createCarouselEvent_when_forCarouselCalled() {
        // Arrange
        Long projectId = 2L;
        Long projectImageId = 10L;
        int imageIndex = 3;
        String tempPath = "/tmp/carousel.tmp";
        String optimizedPath = "/uploads/carousel.webp";
        String thumbnailPath = "/uploads/carousel_thumb.webp";

        // Act
        ImageProcessingEvent event = ImageProcessingEvent.forCarousel(
                projectId, projectImageId, imageIndex, tempPath, optimizedPath, thumbnailPath);

        // Assert
        assertThat(event.getProcessingType()).isEqualTo(ImageProcessingEvent.ImageProcessingType.PROJECT_CAROUSEL);
        assertThat(event.getEntityId()).isEqualTo(projectId);
        assertThat(event.getProjectImageId()).isEqualTo(projectImageId);
        assertThat(event.getImageIndex()).isEqualTo(imageIndex);
    }

    @Test
    void should_createArticleEvent_when_forArticleCalled() {
        // Arrange
        Long articleId = 5L;
        Long articleImageId = 15L;
        String tempPath = "/tmp/article.tmp";
        String optimizedPath = "/uploads/article.webp";
        String thumbnailPath = "/uploads/article_thumb.webp";

        // Act
        ImageProcessingEvent event = ImageProcessingEvent.forArticle(
                articleId, articleImageId, tempPath, optimizedPath, thumbnailPath);

        // Assert
        assertThat(event.getProcessingType()).isEqualTo(ImageProcessingEvent.ImageProcessingType.ARTICLE);
        assertThat(event.getEntityId()).isEqualTo(articleId);
        assertThat(event.getArticleImageId()).isEqualTo(articleImageId);
    }

    @Test
    void should_createProfileEvent_when_forProfileCalled() {
        // Arrange
        String tempPath = "/tmp/profile.tmp";
        String optimizedPath = "/uploads/profile.webp";

        // Act
        ImageProcessingEvent event = ImageProcessingEvent.forProfile(tempPath, optimizedPath);

        // Assert
        assertThat(event.getProcessingType()).isEqualTo(ImageProcessingEvent.ImageProcessingType.PROFILE);
        assertThat(event.getEntityId()).isNull();
        assertThat(event.getThumbnailFilePath()).isNull();
    }

    @Test
    void should_generateUniqueEventId_when_factoryMethodCalled() {
        // Act
        ImageProcessingEvent event1 = ImageProcessingEvent.forProject(
                1L, "/tmp/1.tmp", "/uploads/1.webp", "/uploads/1_thumb.webp");
        ImageProcessingEvent event2 = ImageProcessingEvent.forProject(
                1L, "/tmp/2.tmp", "/uploads/2.webp", "/uploads/2_thumb.webp");

        // Assert
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }

    @Test
    void should_returnCorrectEventType_when_getEventTypeCalled() {
        // Arrange
        ImageProcessingEvent event = ImageProcessingEvent.forProject(
                1L, "/tmp/test.tmp", "/uploads/test.webp", "/uploads/test_thumb.webp");

        // Act
        String eventType = event.getEventType();

        // Assert
        assertThat(eventType).isEqualTo("IMAGE_PROCESSING");
    }

    // ========== Builder Tests ==========

    @Test
    void should_createEvent_when_builderUsed() {
        // Arrange & Act
        ImageProcessingEvent event = ImageProcessingEvent.builder()
                .eventId("custom-id")
                .processingType(ImageProcessingEvent.ImageProcessingType.ARTICLE)
                .entityId(10L)
                .tempFilePath("/tmp/custom.tmp")
                .optimizedFilePath("/uploads/custom.webp")
                .thumbnailFilePath("/uploads/custom_thumb.webp")
                .imageIndex(5)
                .build();

        // Assert
        assertThat(event.getEventId()).isEqualTo("custom-id");
        assertThat(event.getProcessingType()).isEqualTo(ImageProcessingEvent.ImageProcessingType.ARTICLE);
        assertThat(event.getEntityId()).isEqualTo(10L);
        assertThat(event.getImageIndex()).isEqualTo(5);
    }

    // ========== Constants Tests ==========

    @Test
    void should_haveCorrectEventTypeConstant() {
        // Assert
        assertThat(ImageProcessingEvent.EVENT_TYPE).isEqualTo("IMAGE_PROCESSING");
    }
}
