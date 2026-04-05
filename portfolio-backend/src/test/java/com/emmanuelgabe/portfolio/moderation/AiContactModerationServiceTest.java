package com.emmanuelgabe.portfolio.moderation;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
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

    @InjectMocks
    private AiContactModerationService aiContactModerationService;

    private ContactRequest validRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiContactModerationService, "timeoutSeconds", 5);

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
        AiModerationResponse aiResponse = new AiModerationResponse("APPROVED", "Legitimate inquiry", 0.95);
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
        AiModerationResponse aiResponse = new AiModerationResponse("REJECTED", "SEO spam detected", 0.92);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.isApproved()).isFalse();
        assertThat(result.verdict()).isEqualTo(ModerationVerdict.REJECTED);
        assertThat(result.reason()).isEqualTo("SEO spam detected");
        assertThat(result.confidenceScore()).isEqualTo(0.92);
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
    }

    @Test
    void should_returnApproved_when_aiReturnsNullResponse() {
        // Arrange
        mockChatClientChain(null);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.isApproved()).isTrue();
    }

    @Test
    void should_returnApproved_when_aiReturnsInvalidVerdict() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse("MAYBE", "Unclear", 0.5);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.isApproved()).isTrue();
    }

    @Test
    void should_returnApproved_when_aiReturnsNullVerdict() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse(null, "No verdict", 0.5);
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
        AiModerationResponse aiResponse = new AiModerationResponse("APPROVED", "Looks legitimate", 0.75);
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
        AiModerationResponse aiResponse = new AiModerationResponse("REJECTED", "Spam", 1.5);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.confidenceScore()).isEqualTo(1.0);
    }

    @Test
    void should_clampConfidenceScore_when_aiReturnsNegativeValue() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse("REJECTED", "Spam", -0.5);
        mockChatClientChain(aiResponse);

        // Act
        ModerationResult result = aiContactModerationService.moderate(validRequest);

        // Assert
        assertThat(result.confidenceScore()).isEqualTo(0.0);
    }

    // ========== Interaction Tests ==========

    @Test
    void should_useModerationPromptBuilder_when_moderateCalled() {
        // Arrange
        AiModerationResponse aiResponse = new AiModerationResponse("APPROVED", "OK", 0.9);
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
        AiModerationResponse aiResponse = new AiModerationResponse("approved", "Looks good", 0.8);
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
