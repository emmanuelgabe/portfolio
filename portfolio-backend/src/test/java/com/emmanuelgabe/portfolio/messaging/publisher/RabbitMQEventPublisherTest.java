package com.emmanuelgabe.portfolio.messaging.publisher;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.messaging.config.RabbitMQProperties;
import com.emmanuelgabe.portfolio.messaging.event.AuditEvent;
import com.emmanuelgabe.portfolio.messaging.event.ContactEmailEvent;
import com.emmanuelgabe.portfolio.messaging.event.EmailEvent;
import com.emmanuelgabe.portfolio.messaging.event.ImageProcessingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitMQEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitMQProperties properties;

    @Mock
    private RabbitMQProperties.Exchanges exchanges;

    @Mock
    private RabbitMQProperties.RoutingKeys routingKeys;

    private RabbitMQEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = new RabbitMQEventPublisher(rabbitTemplate, properties);

        lenient().when(properties.getExchanges()).thenReturn(exchanges);
        lenient().when(properties.getRoutingKeys()).thenReturn(routingKeys);
        lenient().when(exchanges.getEmail()).thenReturn("portfolio.email.exchange");
        lenient().when(routingKeys.getEmail()).thenReturn("email.send");
    }

    // ========== Publish Email Event Tests ==========

    @Test
    void should_publishEvent_when_publishEmailEventCalledWithValidEvent() {
        // Arrange
        EmailEvent event = createTestEmailEvent();

        // Act
        eventPublisher.publishEmailEvent(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                "portfolio.email.exchange",
                "email.send",
                event
        );
    }

    @Test
    void should_useCorrectExchangeAndRoutingKey_when_publishEmailEventCalled() {
        // Arrange
        when(exchanges.getEmail()).thenReturn("custom.exchange");
        when(routingKeys.getEmail()).thenReturn("custom.routing.key");

        EmailEvent event = createTestEmailEvent();

        // Act
        eventPublisher.publishEmailEvent(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                "custom.exchange",
                "custom.routing.key",
                event
        );
    }

    @Test
    void should_throwAmqpException_when_rabbitTemplateThrowsException() {
        // Arrange
        EmailEvent event = createTestEmailEvent();

        doThrow(new AmqpException("Connection failed"))
                .when(rabbitTemplate)
                .convertAndSend("portfolio.email.exchange", "email.send", event);

        // Act & Assert
        assertThatThrownBy(() -> eventPublisher.publishEmailEvent(event))
                .isInstanceOf(AmqpException.class)
                .hasMessageContaining("Connection failed");
    }

    // ========== Is Enabled Tests ==========

    @Test
    void should_returnTrue_when_isEnabledCalledAndPropertyIsTrue() {
        // Arrange
        when(properties.isEnabled()).thenReturn(true);

        // Act
        boolean result = eventPublisher.isEnabled();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void should_returnFalse_when_isEnabledCalledAndPropertyIsFalse() {
        // Arrange
        when(properties.isEnabled()).thenReturn(false);

        // Act
        boolean result = eventPublisher.isEnabled();

        // Assert
        assertThat(result).isFalse();
    }

    // ========== Publish Image Event Tests ==========

    @Test
    void should_publishEvent_when_publishImageEventCalledWithValidEvent() {
        // Arrange
        lenient().when(exchanges.getImage()).thenReturn("portfolio.image.exchange");
        lenient().when(routingKeys.getImage()).thenReturn("image.process");

        ImageProcessingEvent event = ImageProcessingEvent.forProject(
                1L, "/tmp/test.tmp", "/uploads/test.webp", "/uploads/test_thumb.webp");

        // Act
        eventPublisher.publishImageEvent(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                "portfolio.image.exchange",
                "image.process",
                event
        );
    }

    @Test
    void should_useCorrectExchangeAndRoutingKey_when_publishImageEventCalled() {
        // Arrange
        when(exchanges.getImage()).thenReturn("custom.image.exchange");
        when(routingKeys.getImage()).thenReturn("custom.image.routing.key");

        ImageProcessingEvent event = ImageProcessingEvent.forArticle(
                5L, 15L, "/tmp/article.tmp", "/uploads/article.webp", "/uploads/article_thumb.webp");

        // Act
        eventPublisher.publishImageEvent(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                "custom.image.exchange",
                "custom.image.routing.key",
                event
        );
    }

    @Test
    void should_throwAmqpException_when_publishImageEventAndRabbitTemplateThrowsException() {
        // Arrange
        lenient().when(exchanges.getImage()).thenReturn("portfolio.image.exchange");
        lenient().when(routingKeys.getImage()).thenReturn("image.process");

        ImageProcessingEvent event = ImageProcessingEvent.forProject(
                1L, "/tmp/test.tmp", "/uploads/test.webp", "/uploads/test_thumb.webp");

        doThrow(new AmqpException("Connection failed"))
                .when(rabbitTemplate)
                .convertAndSend("portfolio.image.exchange", "image.process", event);

        // Act & Assert
        assertThatThrownBy(() -> eventPublisher.publishImageEvent(event))
                .isInstanceOf(AmqpException.class)
                .hasMessageContaining("Connection failed");
    }

    @Test
    void should_publishCarouselEvent_when_publishImageEventCalledWithCarouselEvent() {
        // Arrange
        lenient().when(exchanges.getImage()).thenReturn("portfolio.image.exchange");
        lenient().when(routingKeys.getImage()).thenReturn("image.process");

        ImageProcessingEvent event = ImageProcessingEvent.forCarousel(
                2L, 10L, 3, "/tmp/carousel.tmp", "/uploads/carousel.webp", "/uploads/carousel_thumb.webp");

        // Act
        eventPublisher.publishImageEvent(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                "portfolio.image.exchange",
                "image.process",
                event
        );
    }

    @Test
    void should_publishProfileEvent_when_publishImageEventCalledWithProfileEvent() {
        // Arrange
        lenient().when(exchanges.getImage()).thenReturn("portfolio.image.exchange");
        lenient().when(routingKeys.getImage()).thenReturn("image.process");

        ImageProcessingEvent event = ImageProcessingEvent.forProfile(
                "/tmp/profile.tmp", "/uploads/profile.webp");

        // Act
        eventPublisher.publishImageEvent(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                "portfolio.image.exchange",
                "image.process",
                event
        );
    }

    // ========== Publish Audit Event Tests ==========

    @Test
    void should_publishEvent_when_publishAuditEventCalledWithEntityAudit() {
        // Arrange
        lenient().when(exchanges.getAudit()).thenReturn("portfolio.audit.exchange");
        lenient().when(routingKeys.getAudit()).thenReturn("audit.log");

        AuditEvent event = AuditEvent.forEntityAudit(
                null, AuditAction.CREATE, "Project", 1L, "Test Project",
                null, null, null, true, null);

        // Act
        eventPublisher.publishAuditEvent(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                "portfolio.audit.exchange",
                "audit.log",
                event
        );
    }

    @Test
    void should_useCorrectExchangeAndRoutingKey_when_publishAuditEventCalled() {
        // Arrange
        when(exchanges.getAudit()).thenReturn("custom.audit.exchange");
        when(routingKeys.getAudit()).thenReturn("custom.audit.routing.key");

        AuditEvent event = AuditEvent.forAuthEvent(
                AuditAction.LOGIN, "admin", "192.168.1.1", "Mozilla/5.0", true, null);

        // Act
        eventPublisher.publishAuditEvent(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                "custom.audit.exchange",
                "custom.audit.routing.key",
                event
        );
    }

    @Test
    void should_throwAmqpException_when_publishAuditEventAndRabbitTemplateThrowsException() {
        // Arrange
        lenient().when(exchanges.getAudit()).thenReturn("portfolio.audit.exchange");
        lenient().when(routingKeys.getAudit()).thenReturn("audit.log");

        AuditEvent event = AuditEvent.forEntityAudit(
                null, AuditAction.DELETE, "Skill", 3L, "Java",
                null, null, null, true, null);

        doThrow(new AmqpException("Connection failed"))
                .when(rabbitTemplate)
                .convertAndSend("portfolio.audit.exchange", "audit.log", event);

        // Act & Assert
        assertThatThrownBy(() -> eventPublisher.publishAuditEvent(event))
                .isInstanceOf(AmqpException.class)
                .hasMessageContaining("Connection failed");
    }

    @Test
    void should_publishAuthEvent_when_publishAuditEventCalledWithAuthEvent() {
        // Arrange
        lenient().when(exchanges.getAudit()).thenReturn("portfolio.audit.exchange");
        lenient().when(routingKeys.getAudit()).thenReturn("audit.log");

        AuditEvent event = AuditEvent.forAuthEvent(
                AuditAction.LOGOUT, "admin", "192.168.1.1", "Mozilla/5.0", true, null);

        // Act
        eventPublisher.publishAuditEvent(event);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                "portfolio.audit.exchange",
                "audit.log",
                event
        );
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
