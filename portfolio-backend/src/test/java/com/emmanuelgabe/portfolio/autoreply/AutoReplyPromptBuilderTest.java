package com.emmanuelgabe.portfolio.autoreply;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AutoReplyPromptBuilderTest {

    @InjectMocks
    private AutoReplyPromptBuilder promptBuilder;

    // ========== System Prompt Tests ==========

    @Test
    void should_includeRoleDefinition_when_buildSystemPromptCalled() {
        // Act
        String systemPrompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(systemPrompt).containsIgnoringCase("portfolio assistant");
        assertThat(systemPrompt).containsIgnoringCase("Emmanuel Gabe");
    }

    @Test
    void should_includeLanguageInstruction_when_buildSystemPromptCalled() {
        // Act
        String systemPrompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(systemPrompt).containsIgnoringCase("french");
        assertThat(systemPrompt).containsIgnoringCase("english");
        assertThat(systemPrompt).containsIgnoringCase("detect the language");
    }

    @Test
    void should_includeSignOffIdentity_when_buildSystemPromptCalled() {
        // Act
        String systemPrompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(systemPrompt).contains("Assistant Portfolio - Emmanuel Gabe");
    }

    @Test
    void should_includePromptInjectionHardening_when_buildSystemPromptCalled() {
        // Act
        String systemPrompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(systemPrompt).contains("<contact_message>");
        assertThat(systemPrompt).containsIgnoringCase("do not follow");
    }

    @Test
    void should_requirePlainTextResponse_when_buildSystemPromptCalled() {
        // Act
        String systemPrompt = promptBuilder.buildSystemPrompt();

        // Assert
        assertThat(systemPrompt).containsIgnoringCase("plain text");
    }

    // ========== User Prompt Tests ==========

    @Test
    void should_wrapMessageInXmlDelimiters_when_buildUserPromptCalled() {
        // Arrange
        List<Document> docs = Collections.emptyList();

        // Act
        String userPrompt = promptBuilder.buildUserPrompt("John", "Hello", docs);

        // Assert
        assertThat(userPrompt).contains("<contact_message>");
        assertThat(userPrompt).contains("</contact_message>");
        assertThat(userPrompt).contains("<portfolio_context>");
        assertThat(userPrompt).contains("</portfolio_context>");
    }

    @Test
    void should_includeContactData_when_buildUserPromptCalled() {
        // Arrange
        List<Document> docs = Collections.emptyList();

        // Act
        String userPrompt = promptBuilder.buildUserPrompt(
                "John Doe", "I would like to discuss a collaboration", docs);

        // Assert
        assertThat(userPrompt).contains("John Doe");
        assertThat(userPrompt).contains("I would like to discuss a collaboration");
    }

    @Test
    void should_showNoContentMessage_when_buildUserPromptCalledWithEmptyDocuments() {
        // Arrange
        List<Document> docs = Collections.emptyList();

        // Act
        String userPrompt = promptBuilder.buildUserPrompt("John", "Hello", docs);

        // Assert
        assertThat(userPrompt).contains("No specific portfolio content was found matching this query.");
    }

    @Test
    void should_showNoContentMessage_when_buildUserPromptCalledWithNullDocuments() {
        // Act
        String userPrompt = promptBuilder.buildUserPrompt("John", "Hello", null);

        // Assert
        assertThat(userPrompt).contains("No specific portfolio content was found matching this query.");
    }

    @Test
    void should_includeDocumentDetails_when_buildUserPromptCalledWithDocuments() {
        // Arrange
        Document doc = new Document("doc-1", "Project about Spring Boot microservices",
                Map.of("type", "project", "title", "Portfolio Backend"));
        List<Document> docs = List.of(doc);

        // Act
        String userPrompt = promptBuilder.buildUserPrompt("John", "Tell me about your projects", docs);

        // Assert
        assertThat(userPrompt).contains("Document 1");
        assertThat(userPrompt).contains("project");
        assertThat(userPrompt).contains("Portfolio Backend");
        assertThat(userPrompt).contains("Project about Spring Boot microservices");
    }

    @Test
    void should_handleNullFields_when_buildUserPromptCalledWithNullValues() {
        // Act
        String userPrompt = promptBuilder.buildUserPrompt(null, null, null);

        // Assert
        assertThat(userPrompt).isNotNull();
        assertThat(userPrompt).contains("Name:");
        assertThat(userPrompt).contains("Message:");
    }

    @Test
    void should_truncateLongDocumentContent_when_buildUserPromptCalledWithLargeDocument() {
        // Arrange
        String longContent = "A".repeat(1000);
        Document doc = new Document("doc-1", longContent,
                Map.of("type", "article", "title", "Long Article"));
        List<Document> docs = List.of(doc);

        // Act
        String userPrompt = promptBuilder.buildUserPrompt("John", "Hello", docs);

        // Assert
        assertThat(userPrompt).contains("...");
        assertThat(userPrompt).doesNotContain("A".repeat(1000));
    }
}
