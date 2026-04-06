package com.emmanuelgabe.portfolio.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

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

        // Assert -- explicit defense against prompt injection in visitor_message tags
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

    // ========== User Prompt ==========

    @Test
    void should_wrapMessageInVisitorTags_when_buildUserPromptCalled() {
        // Arrange
        String message = "What are your projects?";

        // Act
        String prompt = promptBuilder.buildUserPrompt(message, List.of());

        // Assert
        assertThat(prompt).contains("<visitor_message>");
        assertThat(prompt).contains("</visitor_message>");
        assertThat(prompt).contains(message);
    }

    @Test
    void should_includeEmptyContextMarker_when_noDocumentsProvided() {
        // Act
        String prompt = promptBuilder.buildUserPrompt("Tell me about your work", List.of());

        // Assert
        assertThat(prompt).contains("<portfolio_context>");
        assertThat(prompt).contains("</portfolio_context>");
        assertThat(prompt).containsIgnoringCase("No specific portfolio content");
    }

    @Test
    void should_includeEmptyContextMarker_when_documentsNull() {
        // Act
        String prompt = promptBuilder.buildUserPrompt("Question?", null);

        // Assert
        assertThat(prompt).containsIgnoringCase("No specific portfolio content");
    }

    @Test
    void should_formatDocuments_when_documentsProvided() {
        // Arrange
        Document doc1 = new Document("doc-1", "Built a microservices platform",
                Map.of("type", "project", "title", "Portfolio Backend"));
        Document doc2 = new Document("doc-2", "Article about Spring AI integration",
                Map.of("type", "article", "title", "Spring AI Guide"));

        // Act
        String prompt = promptBuilder.buildUserPrompt("What about your work?", List.of(doc1, doc2));

        // Assert
        assertThat(prompt).contains("--- Document 1 ---");
        assertThat(prompt).contains("--- Document 2 ---");
        assertThat(prompt).contains("Portfolio Backend");
        assertThat(prompt).contains("Spring AI Guide");
        assertThat(prompt).contains("project");
        assertThat(prompt).contains("article");
    }

    @Test
    void should_truncateLongDocumentContent_when_contentExceedsLimit() {
        // Arrange
        String longContent = "x".repeat(1000);
        Document doc = new Document("doc-1", longContent,
                Map.of("type", "article", "title", "Long Article"));

        // Act
        String prompt = promptBuilder.buildUserPrompt("Question?", List.of(doc));

        // Assert
        assertThat(prompt).contains("...");
        assertThat(prompt).doesNotContain("x".repeat(1000));
    }

    @Test
    void should_handleNullMessage_when_buildUserPromptCalled() {
        // Act
        String prompt = promptBuilder.buildUserPrompt(null, List.of());

        // Assert -- no NPE, wrapping tags still present
        assertThat(prompt).contains("<visitor_message>");
        assertThat(prompt).contains("</visitor_message>");
    }

    @Test
    void should_handleDocumentWithNullMetadata_when_formatting() {
        // Arrange
        Document doc = new Document("Text content");

        // Act
        String prompt = promptBuilder.buildUserPrompt("Question?", List.of(doc));

        // Assert
        assertThat(prompt).contains("--- Document 1 ---");
        assertThat(prompt).contains("Text content");
    }

    @Test
    void should_handleDocumentWithMissingMetadataKeys_when_formatting() {
        // Arrange
        Document doc = new Document("doc-1", "Content", Map.of("other", "value"));

        // Act
        String prompt = promptBuilder.buildUserPrompt("Question?", List.of(doc));

        // Assert -- missing type/title render as empty but no NPE
        assertThat(prompt).contains("--- Document 1 ---");
        assertThat(prompt).contains("Content");
    }
}
