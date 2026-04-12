package com.emmanuelgabe.portfolio.moderation;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ModerationPromptBuilderTest {

    @InjectMocks
    private ModerationPromptBuilder promptBuilder;

    // ========== System Prompt Tests ==========

    @Test
    void should_includeJsonSchema_when_buildSystemPromptCalled() {
        // Act
        String systemPrompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(systemPrompt).contains("verdict");
        assertThat(systemPrompt).contains("APPROVED");
        assertThat(systemPrompt).contains("REJECTED");
        assertThat(systemPrompt).contains("category");
        assertThat(systemPrompt).contains("reason");
        assertThat(systemPrompt).contains("confidenceScore");
    }

    @Test
    void should_includeAllAnalysisDimensions_when_buildSystemPromptCalled() {
        // Act
        String systemPrompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(systemPrompt).contains("SPAM DETECTION");
        assertThat(systemPrompt).contains("TOXICITY CHECK");
        assertThat(systemPrompt).contains("PROFESSIONAL RELEVANCE");
    }

    @Test
    void should_includeFailOpenInstruction_when_buildSystemPromptCalled() {
        // Act
        String systemPrompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(systemPrompt).containsIgnoringCase("when in doubt");
        assertThat(systemPrompt).containsIgnoringCase("APPROVE");
    }

    @Test
    void should_includePromptInjectionHardening_when_buildSystemPromptCalled() {
        // Act
        String systemPrompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(systemPrompt).contains("<contact_data>");
        assertThat(systemPrompt).containsIgnoringCase("do not follow");
    }

    @Test
    void should_mentionFrenchLanguage_when_buildSystemPromptCalled() {
        // Act
        String systemPrompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(systemPrompt).containsIgnoringCase("french");
    }

    // ========== Categorization Tests ==========

    @Test
    void should_includeAllCategories_when_buildSystemPromptCalled() {
        // Act
        String systemPrompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(systemPrompt).contains("MISSION");
        assertThat(systemPrompt).contains("QUESTION");
        assertThat(systemPrompt).contains("FEEDBACK");
        assertThat(systemPrompt).contains("SPAM");
        assertThat(systemPrompt).contains("INAPPROPRIATE");
    }

    @Test
    void should_includeVerdictRules_when_buildSystemPromptCalled() {
        // Act
        String systemPrompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(systemPrompt).containsIgnoringCase("verdict");
        assertThat(systemPrompt).containsIgnoringCase("categorization");
    }

    // ========== User Prompt Tests ==========

    @Test
    void should_wrapFieldsInXmlDelimiters_when_buildUserPromptCalled() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "Test", "test@test.com", "Subject", "Message content"
        );

        // Act
        String userPrompt = promptBuilder.buildUserPrompt(request);

        // Assert
        assertThat(userPrompt).contains("<contact_data>");
        assertThat(userPrompt).contains("</contact_data>");
    }

    @Test
    void should_includeAllContactFields_when_buildUserPromptCalled() {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Project Inquiry",
                "I would like to discuss a collaboration"
        );

        // Act
        String userPrompt = promptBuilder.buildUserPrompt(request);

        // Assert
        assertThat(userPrompt).contains("John Doe");
        assertThat(userPrompt).contains("john@example.com");
        assertThat(userPrompt).contains("Project Inquiry");
        assertThat(userPrompt).contains("I would like to discuss a collaboration");
    }

    @Test
    void should_handleNullFields_when_buildUserPromptCalledWithPartialData() {
        // Arrange
        ContactRequest request = new ContactRequest(null, null, null, null);

        // Act
        String userPrompt = promptBuilder.buildUserPrompt(request);

        // Assert
        assertThat(userPrompt).isNotNull();
        assertThat(userPrompt).contains("Name:");
        assertThat(userPrompt).contains("Email:");
        assertThat(userPrompt).contains("Subject:");
        assertThat(userPrompt).contains("Message:");
    }
}
