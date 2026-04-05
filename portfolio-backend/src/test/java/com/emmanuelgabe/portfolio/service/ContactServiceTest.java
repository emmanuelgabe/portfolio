package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.ContactResponse;
import com.emmanuelgabe.portfolio.messaging.event.AutoReplyEvent;
import com.emmanuelgabe.portfolio.messaging.event.ContactEmailEvent;
import com.emmanuelgabe.portfolio.messaging.publisher.EventPublisher;
import com.emmanuelgabe.portfolio.moderation.ContactModerationService;
import com.emmanuelgabe.portfolio.moderation.ModerationResult;
import com.emmanuelgabe.portfolio.service.impl.ContactServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ContactModerationService moderationService;

    @Captor
    private ArgumentCaptor<ContactEmailEvent> eventCaptor;

    @InjectMocks
    private ContactServiceImpl contactService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contactService, "recipientEmail", "recipient@example.com");
        ReflectionTestUtils.setField(contactService, "autoReplyEnabled", true);
        lenient().when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());
    }

    // ========== Send Contact Email Tests ==========

    @Test
    void should_publishEmailEvent_when_sendContactEmailCalledWithValidRequest() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Test Subject",
                "This is a test message"
        );

        // Act
        ContactResponse response = contactService.sendContactEmail(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Message sent successfully");

        verify(eventPublisher).publishEmailEvent(eventCaptor.capture());

        ContactEmailEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getRecipientEmail()).isEqualTo("recipient@example.com");
        assertThat(capturedEvent.getSenderName()).isEqualTo("John Doe");
        assertThat(capturedEvent.getSenderEmail()).isEqualTo("john@example.com");
        assertThat(capturedEvent.getSubject()).isEqualTo("Test Subject");
        assertThat(capturedEvent.getMessage()).isEqualTo("This is a test message");
    }

    @Test
    void should_generateUniqueEventId_when_sendContactEmailCalled() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "Jane Doe",
                "jane@example.com",
                "Subject",
                "Message"
        );

        // Act
        contactService.sendContactEmail(request);

        // Assert
        verify(eventPublisher).publishEmailEvent(eventCaptor.capture());

        ContactEmailEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventId()).isNotNull();
        assertThat(capturedEvent.getEventId()).isNotEmpty();
        assertThat(capturedEvent.getCreatedAt()).isNotNull();
    }

    @Test
    void should_escapeHtmlInEmailContent_when_requestContainsHtmlTags() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John <script>alert('XSS')</script>",
                "john@example.com",
                "Test <b>Subject</b>",
                "Message with <img src=x onerror=alert('XSS')>"
        );

        // Act
        ContactResponse response = contactService.sendContactEmail(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();

        verify(eventPublisher).publishEmailEvent(eventCaptor.capture());

        ContactEmailEvent capturedEvent = eventCaptor.getValue();
        String body = capturedEvent.getBody();

        assertThat(body).doesNotContain("<script>");
        assertThat(body).doesNotContain("<b>");
        assertThat(body).doesNotContain("<img");
        assertThat(body).contains("&lt;script&gt;");
        assertThat(body).contains("&lt;b&gt;");
    }

    @Test
    void should_convertNewlinesToBreaks_when_messageContainsNewlines() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Subject",
                "Line 1\nLine 2\nLine 3"
        );

        // Act
        contactService.sendContactEmail(request);

        // Assert
        verify(eventPublisher).publishEmailEvent(eventCaptor.capture());

        ContactEmailEvent capturedEvent = eventCaptor.getValue();
        String body = capturedEvent.getBody();

        assertThat(body).contains("Line 1<br>Line 2<br>Line 3");
    }

    @Test
    void should_setCorrectEventType_when_sendContactEmailCalled() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Subject",
                "Message"
        );

        // Act
        contactService.sendContactEmail(request);

        // Assert
        verify(eventPublisher).publishEmailEvent(eventCaptor.capture());

        ContactEmailEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("CONTACT_EMAIL");
    }

    // ========== Moderation Tests ==========

    @Test
    void should_notPublishEmailEvent_when_moderationRejectsMessage() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "Spam Bot",
                "spam@bot.com",
                "BUY NOW",
                "Click here for free money and prizes"
        );
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.rejected("SEO spam detected", 0.95));

        // Act
        contactService.sendContactEmail(request);

        // Assert
        verify(eventPublisher, never()).publishEmailEvent(any());
    }

    @Test
    void should_returnSuccess_when_moderationRejectsMessage() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "Spam Bot",
                "spam@bot.com",
                "BUY NOW",
                "Click here for free money and prizes"
        );
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.rejected("Spam detected", 0.9));

        // Act
        ContactResponse response = contactService.sendContactEmail(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Message sent successfully");
    }

    @Test
    void should_callModerationBeforePublishing_when_sendContactEmailCalled() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Subject",
                "A legitimate test message"
        );

        // Act
        contactService.sendContactEmail(request);

        // Assert
        InOrder inOrder = inOrder(moderationService, eventPublisher);
        inOrder.verify(moderationService).moderate(request);
        inOrder.verify(eventPublisher).publishEmailEvent(any(ContactEmailEvent.class));
    }

    // ========== Auto-Reply Tests ==========

    @Test
    void should_publishAutoReplyEvent_when_moderationApproves() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe", "john@example.com", "Subject", "Message");

        // Act
        contactService.sendContactEmail(request);

        // Assert
        verify(eventPublisher).publishAutoReplyEvent(argThat(event ->
                event.getSenderName().equals("John Doe")
                        && event.getSenderEmail().equals("john@example.com")
                        && event.getSubject().equals("Subject")
                        && event.getMessage().equals("Message")
                        && event.getRecipientEmail().equals("recipient@example.com")
        ));
    }

    @Test
    void should_notPublishAutoReplyEvent_when_moderationRejects() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "Spam Bot", "spam@bot.com", "BUY NOW", "Spam content");
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.rejected("Spam", 0.95));

        // Act
        contactService.sendContactEmail(request);

        // Assert
        verify(eventPublisher, never()).publishAutoReplyEvent(any());
    }

    @Test
    void should_publishEmailBeforeAutoReply_when_sendContactEmailCalled() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe", "john@example.com", "Subject", "Message");

        // Act
        contactService.sendContactEmail(request);

        // Assert
        InOrder inOrder = inOrder(eventPublisher);
        inOrder.verify(eventPublisher).publishEmailEvent(any(ContactEmailEvent.class));
        inOrder.verify(eventPublisher).publishAutoReplyEvent(any(AutoReplyEvent.class));
    }

    @Test
    void should_returnSuccess_when_autoReplyEventPublishingFails() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe", "john@example.com", "Subject", "Message");
        doThrow(new RuntimeException("RabbitMQ down"))
                .when(eventPublisher).publishAutoReplyEvent(any());

        // Act
        ContactResponse response = contactService.sendContactEmail(request);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Message sent successfully");
        verify(eventPublisher).publishEmailEvent(any(ContactEmailEvent.class));
    }

    @Test
    void should_notPublishAutoReplyEvent_when_autoReplyDisabled() {
        // Arrange
        ReflectionTestUtils.setField(contactService, "autoReplyEnabled", false);
        ContactRequest request = new ContactRequest(
                "John Doe", "john@example.com", "Subject", "Message");

        // Act
        contactService.sendContactEmail(request);

        // Assert
        verify(eventPublisher).publishEmailEvent(any(ContactEmailEvent.class));
        verify(eventPublisher, never()).publishAutoReplyEvent(any());
    }
}
