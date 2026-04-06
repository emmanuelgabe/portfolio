package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.ContactResponse;
import com.emmanuelgabe.portfolio.messaging.event.AutoReplyEvent;
import com.emmanuelgabe.portfolio.messaging.event.ContactEmailEvent;
import com.emmanuelgabe.portfolio.messaging.publisher.EventPublisher;
import com.emmanuelgabe.portfolio.moderation.ContactModerationService;
import com.emmanuelgabe.portfolio.moderation.ModerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContactServiceImpl}.
 * Verifies the contact flow orchestration: moderation gate, email event publication,
 * and auto-reply event publication with fail-open behavior.
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    private static final String RECIPIENT_EMAIL = "owner@portfolio.com";

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ContactModerationService moderationService;

    @InjectMocks
    private ContactServiceImpl contactService;

    private ContactRequest validRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contactService, "recipientEmail", RECIPIENT_EMAIL);
        ReflectionTestUtils.setField(contactService, "autoReplyEnabled", true);

        validRequest = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Project Inquiry",
                "I would like to discuss a collaboration opportunity"
        );
    }

    // ========== Approved Flow Tests ==========

    @Test
    void should_publishEmailAndAutoReplyEvents_when_messageApprovedAndAutoReplyEnabled() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());

        // Act
        ContactResponse response = contactService.sendContactEmail(validRequest);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        verify(eventPublisher).publishEmailEvent(any(ContactEmailEvent.class));
        verify(eventPublisher).publishAutoReplyEvent(any(AutoReplyEvent.class));
    }

    @Test
    void should_publishOnlyEmailEvent_when_autoReplyDisabled() {
        // Arrange
        ReflectionTestUtils.setField(contactService, "autoReplyEnabled", false);
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());

        // Act
        ContactResponse response = contactService.sendContactEmail(validRequest);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        verify(eventPublisher).publishEmailEvent(any(ContactEmailEvent.class));
        verify(eventPublisher, never()).publishAutoReplyEvent(any(AutoReplyEvent.class));
    }

    @Test
    void should_useConfiguredRecipientEmail_when_buildingEmailEvent() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());
        ArgumentCaptor<ContactEmailEvent> eventCaptor = ArgumentCaptor.forClass(ContactEmailEvent.class);

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert
        verify(eventPublisher).publishEmailEvent(eventCaptor.capture());
        ContactEmailEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getRecipientEmail()).isEqualTo(RECIPIENT_EMAIL);
        assertThat(capturedEvent.getSenderEmail()).isEqualTo(validRequest.getEmail());
        assertThat(capturedEvent.getSenderName()).isEqualTo(validRequest.getName());
        assertThat(capturedEvent.getSubject()).isEqualTo(validRequest.getSubject());
    }

    @Test
    void should_useConfiguredRecipientEmail_when_buildingAutoReplyEvent() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());
        ArgumentCaptor<AutoReplyEvent> eventCaptor = ArgumentCaptor.forClass(AutoReplyEvent.class);

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert
        verify(eventPublisher).publishAutoReplyEvent(eventCaptor.capture());
        AutoReplyEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getRecipientEmail()).isEqualTo(RECIPIENT_EMAIL);
        assertThat(capturedEvent.getSenderEmail()).isEqualTo(validRequest.getEmail());
        assertThat(capturedEvent.getSenderName()).isEqualTo(validRequest.getName());
        assertThat(capturedEvent.getSubject()).isEqualTo(validRequest.getSubject());
        assertThat(capturedEvent.getMessage()).isEqualTo(validRequest.getMessage());
    }

    @Test
    void should_escapeHtmlSpecialCharacters_when_buildingEmailBody() {
        // Arrange
        ContactRequest maliciousRequest = new ContactRequest(
                "<script>alert('xss')</script>",
                "attacker@example.com",
                "Subject with \"quotes\" & <tags>",
                "Message with <b>html</b> and 'quotes' & ampersand"
        );
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());
        ArgumentCaptor<ContactEmailEvent> eventCaptor = ArgumentCaptor.forClass(ContactEmailEvent.class);

        // Act
        contactService.sendContactEmail(maliciousRequest);

        // Assert
        verify(eventPublisher).publishEmailEvent(eventCaptor.capture());
        String body = eventCaptor.getValue().getBody();
        assertThat(body).doesNotContain("<script>");
        assertThat(body).contains("&lt;script&gt;");
        assertThat(body).contains("&amp;");
        assertThat(body).contains("&quot;");
        assertThat(body).contains("&#x27;");
    }

    // ========== Rejected Flow Tests ==========

    @Test
    void should_notPublishAnyEvent_when_messageRejected() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.rejected("Spam detected", 0.95));

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert
        verify(eventPublisher, never()).publishEmailEvent(any(ContactEmailEvent.class));
        verify(eventPublisher, never()).publishAutoReplyEvent(any(AutoReplyEvent.class));
    }

    @Test
    void should_returnGenericSuccess_when_messageRejected() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.rejected("Spam detected", 0.95));

        // Act
        ContactResponse response = contactService.sendContactEmail(validRequest);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).doesNotContainIgnoringCase("spam");
        assertThat(response.getMessage()).doesNotContainIgnoringCase("rejected");
        assertThat(response.getMessage()).doesNotContainIgnoringCase("blocked");
    }

    // ========== Fail-Open Tests ==========

    @Test
    void should_returnSuccess_when_autoReplyPublicationFails() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(eventPublisher).publishAutoReplyEvent(any(AutoReplyEvent.class));

        // Act
        ContactResponse response = contactService.sendContactEmail(validRequest);

        // Assert
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void should_stillPublishEmailEvent_when_autoReplyPublicationFails() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(eventPublisher).publishAutoReplyEvent(any(AutoReplyEvent.class));

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert
        verify(eventPublisher).publishEmailEvent(any(ContactEmailEvent.class));
    }

    // ========== Moderation Interaction Tests ==========

    @Test
    void should_callModerationService_when_sendContactEmailCalled() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert
        verify(moderationService).moderate(validRequest);
    }

    @Test
    void should_callModerationBeforeEmailPublication_when_sendContactEmailCalled() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert -- verify ordering via InOrder
        var inOrder = org.mockito.Mockito.inOrder(moderationService, eventPublisher);
        inOrder.verify(moderationService).moderate(any(ContactRequest.class));
        inOrder.verify(eventPublisher).publishEmailEvent(any(ContactEmailEvent.class));
    }
}
