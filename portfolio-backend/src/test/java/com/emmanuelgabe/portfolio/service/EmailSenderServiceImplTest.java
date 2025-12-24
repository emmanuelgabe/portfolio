package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.service.impl.EmailSenderServiceImpl;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailSenderServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailSenderServiceImpl emailSenderService;

    // ========== Send HTML Email Tests ==========

    @Test
    void should_sendEmail_when_sendHtmlEmailCalledWithValidParams() {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailSenderService.sendHtmlEmail(
                "from@example.com",
                "to@example.com",
                "Test Subject",
                "<html>Test body</html>"
        );

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void should_throwEmailException_when_mailSenderThrowsMessagingException() {
        // Arrange
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> emailSenderService.sendHtmlEmail(
                "from@example.com",
                "to@example.com",
                "Test Subject",
                "<html>Body</html>"
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection failed");
    }

    @Test
    void should_throwEmailException_when_sendThrowsException() {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender)
                .send(any(MimeMessage.class));

        // Act & Assert
        assertThatThrownBy(() -> emailSenderService.sendHtmlEmail(
                "from@example.com",
                "to@example.com",
                "Test Subject",
                "<html>Body</html>"
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SMTP error");
    }

    @Test
    void should_createMimeMessage_when_sendHtmlEmailCalled() {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailSenderService.sendHtmlEmail(
                "sender@test.com",
                "recipient@test.com",
                "Subject",
                "<p>HTML Content</p>"
        );

        // Assert
        verify(mailSender).createMimeMessage();
    }

    @Test
    void should_sendMimeMessage_when_sendHtmlEmailCalled() {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailSenderService.sendHtmlEmail(
                "sender@test.com",
                "recipient@test.com",
                "Subject",
                "<p>HTML Content</p>"
        );

        // Assert
        verify(mailSender).send(any(MimeMessage.class));
    }
}
