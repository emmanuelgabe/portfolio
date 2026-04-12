package com.emmanuelgabe.portfolio.autoreply;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAutoReplyServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private AutoReplyPromptBuilder promptBuilder;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @InjectMocks
    private AiAutoReplyService aiAutoReplyService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiAutoReplyService, "timeoutSeconds", 5);
        ReflectionTestUtils.setField(aiAutoReplyService, "maxDocuments", 5);

        when(promptBuilder.buildSystemPrompt()).thenReturn("system prompt");
        when(promptBuilder.buildUserPrompt(anyString(), anyString(), any())).thenReturn("user prompt");
    }

    // ========== Happy Path Tests ==========

    @Test
    void should_returnReply_when_generateReplyCalledWithValidInput() {
        // Arrange
        mockVectorStoreSearch(List.of());
        mockChatClientChain("Thank you for your message, John.");

        // Act
        Optional<String> result = aiAutoReplyService.generateReply(
                "John", "john@example.com", "Inquiry", "Tell me about your projects");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("Thank you for your message, John.");
    }

    @Test
    void should_returnReply_when_vectorStoreReturnsDocuments() {
        // Arrange
        Document doc = new Document("doc-1", "Spring Boot project",
                Map.of("type", "project", "title", "Portfolio"));
        mockVectorStoreSearch(List.of(doc));
        mockChatClientChain("I see you are interested in my Spring Boot project.");

        // Act
        Optional<String> result = aiAutoReplyService.generateReply(
                "Jane", "jane@example.com", "Projects", "What frameworks do you use?");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).contains("Spring Boot");
    }

    // ========== Fail-Open Tests ==========

    @Test
    void should_returnEmpty_when_chatClientThrowsException() {
        // Arrange
        mockVectorStoreSearch(List.of());
        when(chatClient.prompt()).thenThrow(new RuntimeException("API unavailable"));

        // Act
        Optional<String> result = aiAutoReplyService.generateReply(
                "John", "john@example.com", "Hello", "Hi there");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_aiReturnsNullContent() {
        // Arrange
        mockVectorStoreSearch(List.of());
        mockChatClientChain(null);

        // Act
        Optional<String> result = aiAutoReplyService.generateReply(
                "John", "john@example.com", "Hello", "Hi there");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmpty_when_aiReturnsBlankContent() {
        // Arrange
        mockVectorStoreSearch(List.of());
        mockChatClientChain("   ");

        // Act
        Optional<String> result = aiAutoReplyService.generateReply(
                "John", "john@example.com", "Hello", "Hi there");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_proceedWithoutContext_when_vectorStoreThrowsException() {
        // Arrange
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Elasticsearch down"));
        mockChatClientChain("Thank you for reaching out.");

        // Act
        Optional<String> result = aiAutoReplyService.generateReply(
                "John", "john@example.com", "Hello", "Hi there");

        // Assert
        assertThat(result).isPresent();
    }

    // ========== Interaction Tests ==========

    @Test
    void should_callVectorStore_when_generateReplyCalled() {
        // Arrange
        mockVectorStoreSearch(List.of());
        mockChatClientChain("Reply text");

        // Act
        aiAutoReplyService.generateReply("John", "john@example.com", "Subject", "Message");

        // Assert
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void should_callPromptBuilder_when_generateReplyCalled() {
        // Arrange
        mockVectorStoreSearch(List.of());
        mockChatClientChain("Reply text");

        // Act
        aiAutoReplyService.generateReply("John", "john@example.com", "Subject", "Message");

        // Assert
        verify(promptBuilder).buildSystemPrompt();
        verify(promptBuilder).buildUserPrompt(anyString(), anyString(), any());
    }

    // ========== Helper Methods ==========

    private void mockVectorStoreSearch(List<Document> documents) {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
    }

    private void mockChatClientChain(String responseContent) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(responseContent);
    }
}
