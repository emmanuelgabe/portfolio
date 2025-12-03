package com.emmanuelgabe.portfolio.messaging.publisher;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.messaging.event.AuditEvent;
import com.emmanuelgabe.portfolio.messaging.event.ContactEmailEvent;
import com.emmanuelgabe.portfolio.messaging.event.EmailEvent;
import com.emmanuelgabe.portfolio.messaging.event.ImageProcessingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class NoOpEventPublisherTest {

    @InjectMocks
    private NoOpEventPublisher noOpEventPublisher;

    // ========== Publish Email Event Tests ==========

    @Test
    void should_notThrowException_when_publishEmailEventCalled() {
        // Arrange
        EmailEvent event = createTestEmailEvent();

        // Act & Assert
        assertThatCode(() -> noOpEventPublisher.publishEmailEvent(event))
                .doesNotThrowAnyException();
    }

    // ========== Publish Image Event Tests ==========

    @Test
    void should_notThrowException_when_publishImageEventCalledWithProjectEvent() {
        // Arrange
        ImageProcessingEvent event = ImageProcessingEvent.forProject(
                1L, "/tmp/test.tmp", "/uploads/test.webp", "/uploads/test_thumb.webp");

        // Act & Assert
        assertThatCode(() -> noOpEventPublisher.publishImageEvent(event))
                .doesNotThrowAnyException();
    }

    @Test
    void should_notThrowException_when_publishImageEventCalledWithCarouselEvent() {
        // Arrange
        ImageProcessingEvent event = ImageProcessingEvent.forCarousel(
                2L, 10L, 3, "/tmp/carousel.tmp", "/uploads/carousel.webp", "/uploads/carousel_thumb.webp");

        // Act & Assert
        assertThatCode(() -> noOpEventPublisher.publishImageEvent(event))
                .doesNotThrowAnyException();
    }

    @Test
    void should_notThrowException_when_publishImageEventCalledWithArticleEvent() {
        // Arrange
        ImageProcessingEvent event = ImageProcessingEvent.forArticle(
                5L, 15L, "/tmp/article.tmp", "/uploads/article.webp", "/uploads/article_thumb.webp");

        // Act & Assert
        assertThatCode(() -> noOpEventPublisher.publishImageEvent(event))
                .doesNotThrowAnyException();
    }

    @Test
    void should_notThrowException_when_publishImageEventCalledWithProfileEvent() {
        // Arrange
        ImageProcessingEvent event = ImageProcessingEvent.forProfile(
                "/tmp/profile.tmp", "/uploads/profile.webp");

        // Act & Assert
        assertThatCode(() -> noOpEventPublisher.publishImageEvent(event))
                .doesNotThrowAnyException();
    }

    // ========== Publish Audit Event Tests ==========

    @Test
    void should_notThrowException_when_publishAuditEventCalledWithEntityAudit() {
        // Arrange
        AuditEvent event = AuditEvent.forEntityAudit(
                null, AuditAction.CREATE, "Project", 1L, "Test Project",
                null, null, null, true, null);

        // Act & Assert
        assertThatCode(() -> noOpEventPublisher.publishAuditEvent(event))
                .doesNotThrowAnyException();
    }

    @Test
    void should_notThrowException_when_publishAuditEventCalledWithAuthEvent() {
        // Arrange
        AuditEvent event = AuditEvent.forAuthEvent(
                AuditAction.LOGIN, "admin", "192.168.1.1", "Mozilla/5.0", true, null);

        // Act & Assert
        assertThatCode(() -> noOpEventPublisher.publishAuditEvent(event))
                .doesNotThrowAnyException();
    }

    @Test
    void should_notThrowException_when_publishAuditEventCalledWithLogoutEvent() {
        // Arrange
        AuditEvent event = AuditEvent.forAuthEvent(
                AuditAction.LOGOUT, "admin", "192.168.1.1", "Mozilla/5.0", true, null);

        // Act & Assert
        assertThatCode(() -> noOpEventPublisher.publishAuditEvent(event))
                .doesNotThrowAnyException();
    }

    // ========== Is Enabled Tests ==========

    @Test
    void should_returnFalse_when_isEnabledCalled() {
        // Act
        boolean result = noOpEventPublisher.isEnabled();

        // Assert
        assertThat(result).isFalse();
    }

    private EmailEvent createTestEmailEvent() {
        return ContactEmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .recipientEmail("recipient@example.com")
                .senderName("Test Sender")
                .senderEmail("sender@example.com")
                .subject("Test Subject")
                .message("Test message")
                .body("<html>Test body</html>")
                .build();
    }
}
