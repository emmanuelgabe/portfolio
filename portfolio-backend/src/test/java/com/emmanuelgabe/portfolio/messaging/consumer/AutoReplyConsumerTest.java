package com.emmanuelgabe.portfolio.messaging.consumer;

import com.emmanuelgabe.portfolio.autoreply.AutoReplyService;
import com.emmanuelgabe.portfolio.messaging.event.AutoReplyEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.service.EmailSenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoReplyConsumerTest {

    @Mock
    private AutoReplyService autoReplyService;

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private BusinessMetrics metrics;

    @InjectMocks
    private AutoReplyConsumer autoReplyConsumer;

    private AutoReplyEvent validEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(autoReplyConsumer, "portfolioEmail", "owner@portfolio.com");

        validEvent = AutoReplyEvent.of(
                "John Doe", "john@example.com", "Project Inquiry",
                "I would like to discuss a collaboration", "owner@portfolio.com");
    }

    // ========== Happy Path Tests ==========

    @Test
    void should_sendBothEmails_when_replyGenerated() {
        // Arrange
        when(autoReplyService.generateReply(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Thank you for your message, John."));

        // Act
        autoReplyConsumer.handleAutoReplyEvent(validEvent);

        // Assert
        verify(emailSenderService, times(2)).sendHtmlEmail(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void should_sendAutoReplyToSender_when_replyGenerated() {
        // Arrange
        when(autoReplyService.generateReply(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Thank you for your message."));

        // Act
        autoReplyConsumer.handleAutoReplyEvent(validEvent);

        // Assert
        verify(emailSenderService).sendHtmlEmail(
                eq("owner@portfolio.com"),
                eq("john@example.com"),
                eq("Re: Project Inquiry"),
                anyString()
        );
    }

    @Test
    void should_sendEnrichedNotificationToOwner_when_replyGenerated() {
        // Arrange
        when(autoReplyService.generateReply(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Thank you for your message."));

        // Act
        autoReplyConsumer.handleAutoReplyEvent(validEvent);

        // Assert
        verify(emailSenderService).sendHtmlEmail(
                eq("owner@portfolio.com"),
                eq("owner@portfolio.com"),
                eq("[Portfolio Contact + Auto-Reply] Project Inquiry"),
                anyString()
        );
    }

    @Test
    void should_recordMetric_when_replyGeneratedSuccessfully() {
        // Arrange
        when(autoReplyService.generateReply(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Reply text"));

        // Act
        autoReplyConsumer.handleAutoReplyEvent(validEvent);

        // Assert
        verify(metrics).recordAutoReplyGenerated();
        verify(metrics, never()).recordAutoReplyFailure();
    }

    // ========== No Reply Generated Tests ==========

    @Test
    void should_skipEmails_when_noReplyGenerated() {
        // Arrange
        when(autoReplyService.generateReply(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act
        autoReplyConsumer.handleAutoReplyEvent(validEvent);

        // Assert
        verify(emailSenderService, never()).sendHtmlEmail(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void should_notRecordMetrics_when_noReplyGenerated() {
        // Arrange
        when(autoReplyService.generateReply(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act
        autoReplyConsumer.handleAutoReplyEvent(validEvent);

        // Assert
        verify(metrics, never()).recordAutoReplyGenerated();
        verify(metrics, never()).recordAutoReplyFailure();
    }

    // ========== Fail-Open Tests ==========

    @Test
    void should_notRethrow_when_replyGenerationThrowsException() {
        // Arrange
        when(autoReplyService.generateReply(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("AI service error"));

        // Act -- should not throw
        autoReplyConsumer.handleAutoReplyEvent(validEvent);

        // Assert
        verify(metrics).recordAutoReplyFailure();
    }

    @Test
    void should_notRethrow_when_senderEmailThrowsException() {
        // Arrange
        when(autoReplyService.generateReply(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Reply text"));
        doThrow(new RuntimeException("SMTP error"))
                .when(emailSenderService).sendHtmlEmail(
                        anyString(), anyString(), anyString(), anyString());

        // Act -- should not throw
        autoReplyConsumer.handleAutoReplyEvent(validEvent);

        // Assert -- sender email failed, so generated metric is not recorded
        verify(metrics, never()).recordAutoReplyGenerated();
    }

    @Test
    void should_stillSendOwnerNotification_when_senderEmailFails() {
        // Arrange
        when(autoReplyService.generateReply(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Reply text"));
        // First call (sender) fails, second call (owner) succeeds
        doThrow(new RuntimeException("SMTP error"))
                .doNothing()
                .when(emailSenderService).sendHtmlEmail(
                        anyString(), anyString(), anyString(), anyString());

        // Act
        autoReplyConsumer.handleAutoReplyEvent(validEvent);

        // Assert -- both calls attempted despite first failure
        verify(emailSenderService, times(2)).sendHtmlEmail(
                anyString(), anyString(), anyString(), anyString());
    }
}
