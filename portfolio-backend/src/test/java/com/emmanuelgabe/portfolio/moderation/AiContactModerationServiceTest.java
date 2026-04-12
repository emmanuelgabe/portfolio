package com.emmanuelgabe.portfolio.moderation;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiContactModerationServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ModerationPromptBuilder promptBuilder;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private BusinessMetrics metrics;

    @InjectMocks
    private AiContactModerationService aiContactModerationService;

    private ContactRequest validRequest;
    private Timer moderationTimer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiContactModerationService, "timeoutSeconds", 5);

        // Use a real Timer from SimpleMeterRegistry: sample.stop(timer) needs an actual
        // timer instance, which is hard to mock correctly through Mockito's proxy
        MeterRegistry registry = new SimpleMeterRegistry();
        moderationTimer = Timer.builder("test.moderation.duration").register(registry);
        when(metrics.getModerationTimer()).thenReturn(moderationTimer);

        validRequest = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Project Inquiry",
                "I would like to discuss a collaboration opportunity"
        );

        when(promptBuilder.buildSystemPrompt()).thenReturn("system prompt");
        when(promptBuilder.buildUserPrompt(any(ContactRequest.class))).thenReturn("user prompt");
    }

    // ========== Happy Path Tests ==========

    @Test
    void should_returnApproved_when_aiReturnsApprovedVerdict() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "APPROVED", "QUESTION", "Legitimate inquiry", 0.95);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.isApproved()).isTrue();
        assertThat(result.verdict()).isEqualTo(ModerationVerdict.APPROVED);
    }

    @Test
    void should_returnRejected_when_aiReturnsRejectedForSpam() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "REJECTED", "SPAM", "SEO spam detected", 0.92);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.isApproved()).isFalse();
        assertThat(result.verdict()).isEqualTo(ModerationVerdict.REJECTED);
        assertThat(result.reason()).isEqualTo("SEO spam detected");
        assertThat(result.confidenceScore()).isEqualTo(0.92);
    }

    // ========== Category Tests ==========

    @Test
    void should_parseMissionCategory_when_aiReturnsMission() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "APPROVED", "MISSION", "Freelance request", 0.9);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.category()).isEqualTo(ContactCategory.MISSION);
    }

    @Test
    void should_parseSpamCategory_when_aiReturnsSpam() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "REJECTED", "SPAM", "Marketing spam", 0.95);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.category()).isEqualTo(ContactCategory.SPAM);
    }

    @Test
    void should_parseInappropriateCategory_when_aiReturnsInappropriate() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "REJECTED", "INAPPROPRIATE", "Hate speech detected", 0.98);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.category()).isEqualTo(ContactCategory.INAPPROPRIATE);
    }

    @Test
    void should_returnNullCategory_when_aiReturnsInvalidCategory() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "APPROVED", "UNKNOWN_CATEGORY", "Some message", 0.8);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.isApproved()).isTrue();
        assertThat(result.category()).isNull();
    }

    @Test
    void should_returnNullCategory_when_aiReturnsNullCategory() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "APPROVED", null, "Looks fine", 0.85);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.isApproved()).isTrue();
        assertThat(result.category()).isNull();
    }

    @Test
    void should_parseCategoryCaseInsensitive_when_aiReturnsLowercaseCategory() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "APPROVED", "feedback", "User feedback", 0.88);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.category()).isEqualTo(ContactCategory.FEEDBACK);
    }

    // ========== Fail-Open Tests ==========

    @Test
    void should_returnApproved_when_aiCallThrowsException() {
        // Arrange
        when(chatClient.prompt()).thenThrow(new RuntimeException("API unavailable"));

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.isApproved()).isTrue();
        assertThat(result.confidenceScore()).isEqualTo(1.0);
        assertThat(result.category()).isNull();
    }

    @Test
    void should_returnApproved_when_aiReturnsNullResponse() {
        // Arrange
        mockChatClientChain(null);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.isApproved()).isTrue();
        assertThat(result.category()).isNull();
    }

    @Test
    void should_returnApproved_when_aiReturnsInvalidVerdict() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "MAYBE", "QUESTION", "Unclear", 0.5);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.isApproved()).isTrue();
    }

    @Test
    void should_returnApproved_when_aiReturnsNullVerdict() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                null, "QUESTION", "No verdict", 0.5);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.isApproved()).isTrue();
    }

    // ========== Confidence Score Tests ==========

    @Test
    void should_preserveAiConfidenceScore_when_aiReturnsApproved() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "APPROVED", "QUESTION", "Looks legitimate", 0.75);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.isApproved()).isTrue();
        assertThat(result.confidenceScore()).isEqualTo(0.75);
        assertThat(result.reason()).isEqualTo("Looks legitimate");
    }

    @Test
    void should_clampConfidenceScore_when_aiReturnsValueAboveOne() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "REJECTED", "SPAM", "Spam", 1.5);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.confidenceScore()).isEqualTo(1.0);
    }

    @Test
    void should_clampConfidenceScore_when_aiReturnsNegativeValue() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "REJECTED", "SPAM", "Spam", -0.5);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.confidenceScore()).isEqualTo(0.0);
    }

    // ========== Metrics Tests ==========

    @Test
    void should_recordCategoryMetric_when_categoryPresent() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "APPROVED", "MISSION", "Job offer", 0.9);
        mockChatClientChain(aiResponse);

        // Act
        aiContactModerationService.moderate(validRequest);

        // Assert
        verify(metrics).recordModerationApproval();
        verify(metrics).recordModerationCategory("MISSION");
    }

    @Test
    void should_recordFailureMetric_when_aiCallFails() {
        // Arrange
        when(chatClient.prompt()).thenThrow(new RuntimeException("Timeout"));

        // Act
        aiContactModerationService.moderate(validRequest);

        // Assert
        verify(metrics).recordModerationFailure();
    }

    // ========== Interaction Tests ==========

    @Test
    void should_useModerationPromptBuilder_when_moderateCalled() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "APPROVED", "QUESTION", "OK", 0.9);
        mockChatClientChain(aiResponse);

        // Act
        aiContactModerationService.moderate(validRequest);

        // Assert
        verify(promptBuilder).buildSystemPrompt();
        verify(promptBuilder).buildUserPrompt(validRequest);
    }

    @Test
    void should_handleCaseInsensitiveVerdict_when_aiReturnsLowercase() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(
                "approved", "QUESTION", "Looks good", 0.8);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.isApproved()).isTrue();
    }

    // ========== Helper Methods ==========

    private void mockChatClientChain(AiModerationResponse aiResponse) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(eq(AiModerationResponse.class))).thenReturn(aiResponse);
    }
}
