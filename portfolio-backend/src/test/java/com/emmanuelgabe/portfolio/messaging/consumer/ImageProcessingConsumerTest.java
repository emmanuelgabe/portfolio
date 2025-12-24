package com.emmanuelgabe.portfolio.messaging.consumer;

import com.emmanuelgabe.portfolio.messaging.event.ImageProcessingEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.repository.ArticleImageRepository;
import com.emmanuelgabe.portfolio.repository.ProjectImageRepository;
import com.emmanuelgabe.portfolio.service.ImageProcessorService;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ImageProcessingConsumerTest {

    @Mock
    private ImageProcessorService imageProcessorService;

    @Mock
    private ProjectImageRepository projectImageRepository;

    @Mock
    private ArticleImageRepository articleImageRepository;

    @Mock
    private BusinessMetrics metrics;

    @Mock
    private Timer imageProcessingTimer;

    @InjectMocks
    private ImageProcessingConsumer imageProcessingConsumer;

    @BeforeEach
    void setUp() {
        lenient().when(metrics.getImageProcessingTimer()).thenReturn(imageProcessingTimer);
    }

    // ========== Handle Image Processing Event Tests ==========

    @Test
    void should_processImage_when_handleImageProcessingEventCalledWithValidEvent() {
        // Arrange
        ImageProcessingEvent event = ImageProcessingEvent.forProject(
                1L, "/tmp/test.tmp", "/uploads/test.webp", "/uploads/test_thumb.webp");

        // Act
        imageProcessingConsumer.handleImageProcessingEvent(event);

        // Assert
        verify(imageProcessorService).processImage(event);
    }

    @Test
    void should_recordMetrics_when_imageProcessedSuccessfully() {
        // Arrange
        ImageProcessingEvent event = ImageProcessingEvent.forProject(
                1L, "/tmp/test.tmp", "/uploads/test.webp", "/uploads/test_thumb.webp");

        // Act
        imageProcessingConsumer.handleImageProcessingEvent(event);

        // Assert
        verify(metrics).recordImageProcessed();
        verify(metrics).getImageProcessingTimer();
    }

    @Test
    void should_recordFailure_when_imageProcessingFails() {
        // Arrange
        ImageProcessingEvent event = ImageProcessingEvent.forProject(
                1L, "/tmp/test.tmp", "/uploads/test.webp", "/uploads/test_thumb.webp");

        doThrow(new RuntimeException("Processing error"))
                .when(imageProcessorService)
                .processImage(event);

        // Act & Assert
        assertThatThrownBy(() -> imageProcessingConsumer.handleImageProcessingEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Processing error");

        verify(metrics).recordImageProcessingFailure();
        verify(metrics, never()).recordImageProcessed();
    }

    @Test
    void should_rethrowException_when_imageProcessingFails() {
        // Arrange
        ImageProcessingEvent event = ImageProcessingEvent.forArticle(
                5L, 15L, "/tmp/article.tmp", "/uploads/article.webp", "/uploads/article_thumb.webp");

        RuntimeException expectedException = new RuntimeException("IO error");
        doThrow(expectedException)
                .when(imageProcessorService)
                .processImage(event);

        // Act & Assert
        assertThatThrownBy(() -> imageProcessingConsumer.handleImageProcessingEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("IO error");
    }

    @Test
    void should_processCarouselImage_when_carouselEventReceived() {
        // Arrange
        ImageProcessingEvent event = ImageProcessingEvent.forCarousel(
                2L, 10L, 3, "/tmp/carousel.tmp", "/uploads/carousel.webp", "/uploads/carousel_thumb.webp");

        // Act
        imageProcessingConsumer.handleImageProcessingEvent(event);

        // Assert
        verify(imageProcessorService).processImage(event);
        verify(metrics).recordImageProcessed();
    }

    @Test
    void should_processProfileImage_when_profileEventReceived() {
        // Arrange
        ImageProcessingEvent event = ImageProcessingEvent.forProfile(
                "/tmp/profile.tmp", "/uploads/profile.webp");

        // Act
        imageProcessingConsumer.handleImageProcessingEvent(event);

        // Assert
        verify(imageProcessorService).processImage(event);
        verify(metrics).recordImageProcessed();
    }
}
