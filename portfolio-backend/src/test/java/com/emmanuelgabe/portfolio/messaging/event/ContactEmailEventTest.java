package com.emmanuelgabe.portfolio.messaging.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactEmailEventTest {

    // ========== Factory Method Tests ==========

    @Test
    void should_createEvent_when_ofCalledWithValidParams() {
        // Arrange
        String recipientEmail = "recipient@example.com";
        String senderName = "John Doe";
        String senderEmail = "john@example.com";
        String subject = "Test Subject";
        String message = "Test message";
        String body = "<html>Test body</html>";

        // Act
        ContactEmailEvent event = ContactEmailEvent.of(
                recipientEmail,
                senderName,
                senderEmail,
                subject,
                message,
                body
        );

        // Assert
        assertThat(event.getRecipientEmail()).isEqualTo(recipientEmail);
        assertThat(event.getSenderName()).isEqualTo(senderName);
        assertThat(event.getSenderEmail()).isEqualTo(senderEmail);
        assertThat(event.getSubject()).isEqualTo(subject);
        assertThat(event.getMessage()).isEqualTo(message);
        assertThat(event.getBody()).isEqualTo(body);
    }

    @Test
    void should_generateUniqueEventId_when_ofCalled() {
        // Arrange & Act
        ContactEmailEvent event1 = ContactEmailEvent.of(
                "recipient@example.com",
                "John",
                "john@example.com",
                "Subject",
                "Message",
                "Body"
        );

        ContactEmailEvent event2 = ContactEmailEvent.of(
                "recipient@example.com",
                "John",
                "john@example.com",
                "Subject",
                "Message",
                "Body"
        );

        // Assert
        assertThat(event1.getEventId()).isNotNull();
        assertThat(event1.getEventId()).isNotEmpty();
        assertThat(event2.getEventId()).isNotNull();
        assertThat(event2.getEventId()).isNotEmpty();
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }

    @Test
    void should_setCreatedAt_when_ofCalled() {
        // Arrange & Act
        ContactEmailEvent event = ContactEmailEvent.of(
                "recipient@example.com",
                "John",
                "john@example.com",
                "Subject",
                "Message",
                "Body"
        );

        // Assert
        assertThat(event.getCreatedAt()).isNotNull();
    }

    @Test
    void should_returnCorrectEventType_when_getEventTypeCalled() {
        // Arrange
        ContactEmailEvent event = ContactEmailEvent.of(
                "recipient@example.com",
                "John",
                "john@example.com",
                "Subject",
                "Message",
                "Body"
        );

        // Act
        String eventType = event.getEventType();

        // Assert
        assertThat(eventType).isEqualTo("CONTACT_EMAIL");
    }

    // ========== Builder Tests ==========

    @Test
    void should_createEvent_when_builderUsed() {
        // Arrange & Act
        ContactEmailEvent event = ContactEmailEvent.builder()
                .eventId("custom-event-id")
                .recipientEmail("recipient@example.com")
                .senderName("Jane")
                .senderEmail("jane@example.com")
                .subject("Custom Subject")
                .message("Custom message")
                .body("<html>Custom body</html>")
                .build();

        // Assert
        assertThat(event.getEventId()).isEqualTo("custom-event-id");
        assertThat(event.getRecipientEmail()).isEqualTo("recipient@example.com");
        assertThat(event.getSenderName()).isEqualTo("Jane");
        assertThat(event.getSenderEmail()).isEqualTo("jane@example.com");
        assertThat(event.getSubject()).isEqualTo("Custom Subject");
        assertThat(event.getMessage()).isEqualTo("Custom message");
        assertThat(event.getBody()).isEqualTo("<html>Custom body</html>");
    }

    // ========== Constant Tests ==========

    @Test
    void should_haveCorrectEventTypeConstant() {
        // Assert
        assertThat(ContactEmailEvent.EVENT_TYPE).isEqualTo("CONTACT_EMAIL");
    }
}
