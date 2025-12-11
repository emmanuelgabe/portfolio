package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.ContactResponse;
import com.emmanuelgabe.portfolio.messaging.event.ContactEmailEvent;
import com.emmanuelgabe.portfolio.messaging.publisher.EventPublisher;
import com.emmanuelgabe.portfolio.service.impl.ContactServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private EventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<ContactEmailEvent> eventCaptor;

    @InjectMocks
    private ContactServiceImpl contactService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contactService, "recipientEmail", "recipient@example.com");
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
}
