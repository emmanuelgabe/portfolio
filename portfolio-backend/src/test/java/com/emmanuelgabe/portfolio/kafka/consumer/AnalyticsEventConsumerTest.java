package com.emmanuelgabe.portfolio.kafka.consumer;

import com.emmanuelgabe.portfolio.kafka.event.AnalyticsEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for AnalyticsEventConsumer.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsEventConsumerTest {

    @Mock
    private BusinessMetrics metrics;

    private AnalyticsEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AnalyticsEventConsumer(metrics);
    }

    // ========== handleAnalyticsEvent Tests ==========

    @Test
    void should_recordProjectView_when_projectViewEventReceived() {
        // Arrange
        AnalyticsEvent event = AnalyticsEvent.projectView(
                1L, "192.168.1.100", "Mozilla/5.0", "session123");

        // Act
        consumer.handleAnalyticsEvent(event);

        // Assert
        verify(metrics).recordProjectView();
        verify(metrics).recordKafkaEventConsumed("analytics_events", "PROJECT_VIEW");
    }

    @Test
    void should_recordArticleView_when_articleViewEventReceived() {
        // Arrange
        AnalyticsEvent event = AnalyticsEvent.articleView(
                5L, "my-article", "10.0.0.1", "Chrome", "session456");

        // Act
        consumer.handleAnalyticsEvent(event);

        // Assert
        verify(metrics).recordArticleView();
        verify(metrics).recordKafkaEventConsumed("analytics_events", "ARTICLE_VIEW");
    }

    @Test
    void should_recordContactSubmission_when_contactSubmitEventReceived() {
        // Arrange
        AnalyticsEvent event = AnalyticsEvent.contactSubmit("172.16.0.1", "session789");

        // Act
        consumer.handleAnalyticsEvent(event);

        // Assert
        verify(metrics).recordContactSubmission();
        verify(metrics).recordKafkaEventConsumed("analytics_events", "CONTACT_SUBMIT");
    }

    @Test
    void should_handlePageView_when_pageViewEventReceived() {
        // Arrange
        AnalyticsEvent event = AnalyticsEvent.pageView(
                "/about", "https://google.com", "Firefox", "127.0.0.1", "session-abc");

        // Act
        consumer.handleAnalyticsEvent(event);

        // Assert
        verify(metrics).recordKafkaEventConsumed("analytics_events", "PAGE_VIEW");
    }

    @Test
    void should_recordFailureMetric_when_exceptionOccurs() {
        // Arrange
        AnalyticsEvent event = AnalyticsEvent.projectView(
                1L, "192.168.1.1", "UA", "session");

        doThrow(new RuntimeException("Metrics error"))
                .when(metrics).recordProjectView();

        // Act & Assert
        assertThatThrownBy(() -> consumer.handleAnalyticsEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Metrics error");
        verify(metrics).recordKafkaConsumeFailure("analytics_events", "PROJECT_VIEW");
    }
}
