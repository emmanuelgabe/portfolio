package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.ContactResponse;
import com.emmanuelgabe.portfolio.service.impl.ContactServiceImpl;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private ContactServiceImpl contactService;

    @Test
    void should_sendEmail_when_validRequest() {
        // Arrange
        ReflectionTestUtils.setField(contactService, "recipientEmail", "recipient@example.com");

        ContactRequest request = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Test Subject",
                "This is a test message"
        );

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        ContactResponse response = contactService.sendContactEmail(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Message sent successfully");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void should_throwEmailException_when_mailSenderFails() {
        // Arrange
        ReflectionTestUtils.setField(contactService, "recipientEmail", "recipient@example.com");

        ContactRequest request = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Test Subject",
                "This is a test message"
        );

        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail server error"));

        // Act & Assert
        assertThatThrownBy(() -> contactService.sendContactEmail(request))
                .isInstanceOf(RuntimeException.class);

        verify(mailSender).createMimeMessage();
    }

    @Test
    void should_escapeHtmlInEmailContent_when_requestContainsHtmlTags() {
        // Arrange
        ReflectionTestUtils.setField(contactService, "recipientEmail", "recipient@example.com");

        ContactRequest request = new ContactRequest(
                "John <script>alert('XSS')</script>",
                "john@example.com",
                "Test <b>Subject</b>",
                "Message with <img src=x onerror=alert('XSS')>"
        );

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        ContactResponse response = contactService.sendContactEmail(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();

        verify(mailSender).send(any(MimeMessage.class));
    }
}
