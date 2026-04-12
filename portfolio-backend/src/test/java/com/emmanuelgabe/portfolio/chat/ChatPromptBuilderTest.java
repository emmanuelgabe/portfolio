package com.emmanuelgabe.portfolio.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPromptBuilderTest {

    private ChatPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new ChatPromptBuilder();
    }

    // ========== System Prompt ==========

    @Test
    void should_returnSystemPrompt_when_buildSystemPromptCalled() {
        // Act
        String prompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(prompt).isNotBlank();
        assertThat(prompt).containsIgnoringCase("Emmanuel Gabe");
        assertThat(prompt).containsIgnoringCase("portfolio");
    }

    @Test
    void should_containLanguageDetectionRule_when_buildSystemPromptCalled() {
        // Act
        String prompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(prompt).containsIgnoringCase("French");
        assertThat(prompt).containsIgnoringCase("English");
    }

    @Test
    void should_containPromptInjectionDefense_when_buildSystemPromptCalled() {
        // Act
        String prompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(prompt).containsIgnoringCase("visitor_message");
        assertThat(prompt).containsIgnoringCase("ignore previous instructions");
    }

    @Test
    void should_restrictScopeToPortfolio_when_buildSystemPromptCalled() {
        // Act
        String prompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(prompt).containsIgnoringCase("portfolio");
        assertThat(prompt).containsIgnoringCase("decline");
    }

    @Test
    void should_containToolUsageGuidelines_when_buildSystemPromptCalled() {
        // Act
        String prompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(prompt).containsIgnoringCase("searchProjects");
        assertThat(prompt).containsIgnoringCase("listArticles");
        assertThat(prompt).containsIgnoringCase("listSkills");
        assertThat(prompt).containsIgnoringCase("prepareContact");
        assertThat(prompt).containsIgnoringCase("searchPortfolio");
    }

    @Test
    void should_documentProjectListingTools_when_buildSystemPromptCalled() {
        // Act
        String prompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(prompt).containsIgnoringCase("listProjects");
        assertThat(prompt).containsIgnoringCase("getFeaturedProjects");
        assertThat(prompt).containsIgnoringCase("fallback");
    }

    // ========== Wrap User Message ==========

    @Test
    void should_wrapMessageInVisitorTags_when_wrapUserMessageCalled() {
        // Arrange
        String message = "What are your projects?";

        // Act
        String wrapped = promptBuilder.wrapUserMessage(message);

        // Assert
        assertThat(wrapped).contains("<visitor_message>");
        assertThat(wrapped).contains("</visitor_message>");
        assertThat(wrapped).contains(message);
    }

    @Test
    void should_handleNullMessage_when_wrapUserMessageCalled() {
        // Act
        String wrapped = promptBuilder.wrapUserMessage(null);

        // Assert
        assertThat(wrapped).contains("<visitor_message>");
        assertThat(wrapped).contains("</visitor_message>");
    }

    @Test
    void should_preserveMessageContent_when_wrapUserMessageCalled() {
        // Arrange
        String message = "Tell me about your Docker projects";

        // Act
        String wrapped = promptBuilder.wrapUserMessage(message);

        // Assert
        assertThat(wrapped).isEqualTo(
                "<visitor_message>\nTell me about your Docker projects\n</visitor_message>");
    }
}
