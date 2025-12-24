package com.emmanuelgabe.portfolio.messaging.consumer;

import com.emmanuelgabe.portfolio.messaging.event.ContactEmailEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.service.EmailSenderService;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailConsumerTest {

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private BusinessMetrics metrics;

    @Mock
    private Timer emailTimer;

    @InjectMocks
    private EmailConsumer emailConsumer;

    @BeforeEach
    void setUp() {
        lenient().when(metrics.getEmailSendingTimer()).thenReturn(emailTimer);
    }

    // ========== Handle Contact Email Event Tests ==========

    @Test
    void should_sendEmail_when_handleContactEmailEventCalledWithValidEvent() {
        // Arrange
        ContactEmailEvent event = createTestContactEmailEvent();

        // Act
        emailConsumer.handleContactEmailEvent(event);

        // Assert
        verify(emailSenderService).sendHtmlEmail(
                "sender@example.com",
                "recipient@example.com",
                "[Portfolio Contact] Test Subject",
                "<html>Test body</html>"
        );
    }

    @Test
    void should_recordMetrics_when_emailSentSuccessfully() {
        // Arrange
        ContactEmailEvent event = createTestContactEmailEvent();

        // Act
        emailConsumer.handleContactEmailEvent(event);

        // Assert
        verify(metrics).recordContactSubmission();
        verify(metrics).getEmailSendingTimer();
    }

    @Test
    void should_prefixSubject_when_handleContactEmailEventCalled() {
        // Arrange
        ContactEmailEvent event = ContactEmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .recipientEmail("recipient@example.com")
                .senderName("John")
                .senderEmail("john@example.com")
                .subject("Question about your work")
                .message("Hello")
                .body("<html>Hello</html>")
                .build();

        // Act
        emailConsumer.handleContactEmailEvent(event);

        // Assert
        verify(emailSenderService).sendHtmlEmail(
                "john@example.com",
                "recipient@example.com",
                "[Portfolio Contact] Question about your work",
                "<html>Hello</html>"
        );
    }

    @Test
    void should_recordFailure_when_emailSendingFails() {
        // Arrange
        ContactEmailEvent event = createTestContactEmailEvent();

        doThrow(new RuntimeException("SMTP error"))
                .when(emailSenderService)
                .sendHtmlEmail(any(), any(), any(), any());

        // Act & Assert
        assertThatThrownBy(() -> emailConsumer.handleContactEmailEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SMTP error");

        verify(metrics).recordContactFailure();
        verify(metrics, never()).recordContactSubmission();
    }

    @Test
    void should_rethrowException_when_emailSendingFails() {
        // Arrange
        ContactEmailEvent event = createTestContactEmailEvent();

        RuntimeException expectedException = new RuntimeException("Connection timeout");
        doThrow(expectedException)
                .when(emailSenderService)
                .sendHtmlEmail(any(), any(), any(), any());

        // Act & Assert
        assertThatThrownBy(() -> emailConsumer.handleContactEmailEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Connection timeout");
    }

    @Test
    void should_useEventBody_when_sendingEmail() {
        // Arrange
        String htmlBody = """
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>Contact Message</h1>
                    <p>This is a test message</p>
                </body>
                </html>
                """;

        ContactEmailEvent event = ContactEmailEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .recipientEmail("admin@portfolio.com")
                .senderName("Jane Doe")
                .senderEmail("jane@example.com")
                .subject("Inquiry")
                .message("Raw message")
                .body(htmlBody)
                .build();

        // Act
        emailConsumer.handleContactEmailEvent(event);

        // Assert
        verify(emailSenderService).sendHtmlEmail(
                "jane@example.com",
                "admin@portfolio.com",
                "[Portfolio Contact] Inquiry",
                htmlBody
        );
    }

    private ContactEmailEvent createTestContactEmailEvent() {
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
