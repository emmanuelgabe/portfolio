package com.emmanuelgabe.portfolio.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagChatServiceTest {

    private static final String CONVERSATION_ID = "conv-abcd1234";
    private static final String USER_MESSAGE = "Tell me about your Spring projects";

    @Mock
    private ChatClient chatClient;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ConversationHistoryStore historyStore;

    @Mock
    private ChatPromptBuilder promptBuilder;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ChatClient.StreamResponseSpec streamResponseSpec;

    @InjectMocks
    private RagChatService ragChatService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ragChatService, "maxDocuments", 5);
        ReflectionTestUtils.setField(ragChatService, "similarityThreshold", 0.3);

        lenient().when(promptBuilder.buildSystemPrompt()).thenReturn("system prompt");
        lenient().when(promptBuilder.buildUserPrompt(anyString(), anyList())).thenReturn("user prompt");
    }

    // ========== chat: Happy Path ==========

    @Test
    void should_returnReply_when_chatCalledWithValidInput() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockVectorSearch(List.of());
        mockCallChain(List.of(), "Here are my Spring projects.");

        // Act
        String reply = ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert
        assertThat(reply).isEqualTo("Here are my Spring projects.");
    }

    @Test
    void should_includeRetrievedDocuments_when_promptBuilt() {
        // Arrange
        Document doc = new Document("doc-1", "Spring Boot microservice",
                Map.of("type", "project", "title", "Portfolio Backend"));
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockVectorSearch(List.of(doc));
        mockCallChain(List.of(), "Reply based on context.");

        // Act
        ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert
        ArgumentCaptor<List<Document>> docsCaptor = ArgumentCaptor.forClass(List.class);
        verify(promptBuilder).buildUserPrompt(anyString(), docsCaptor.capture());
        assertThat(docsCaptor.getValue()).hasSize(1);
        assertThat(docsCaptor.getValue().get(0).getId()).isEqualTo("doc-1");
    }

    @Test
    void should_includeConversationHistory_when_historyNotEmpty() {
        // Arrange
        List<Message> history = List.of(
                new UserMessage("Previous question"),
                new AssistantMessage("Previous answer")
        );
        when(historyStore.history(CONVERSATION_ID)).thenReturn(history);
        mockVectorSearch(List.of());
        mockCallChain(history, "Follow-up reply.");

        // Act
        String reply = ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert
        assertThat(reply).isEqualTo("Follow-up reply.");
        verify(requestSpec).messages(history);
    }

    @Test
    void should_skipMessagesCall_when_historyEmpty() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockVectorSearch(List.of());
        mockCallChain(List.of(), "Fresh reply.");

        // Act
        ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert
        verify(requestSpec, never()).messages(anyList());
    }

    @Test
    void should_persistUserAndAssistantMessages_when_replyGenerated() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockVectorSearch(List.of());
        mockCallChain(List.of(), "Generated reply.");

        // Act
        ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(historyStore, org.mockito.Mockito.times(2))
                .append(anyString(), messageCaptor.capture());
        List<Message> captured = messageCaptor.getAllValues();
        assertThat(captured.get(0)).isInstanceOf(UserMessage.class);
        assertThat(captured.get(0).getText()).isEqualTo(USER_MESSAGE);
        assertThat(captured.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(captured.get(1).getText()).isEqualTo("Generated reply.");
    }

    // ========== chat: Vector Search Failure ==========

    @Test
    void should_continueWithoutContext_when_vectorSearchThrowsException() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Elasticsearch down"));
        mockCallChain(List.of(), "Reply without docs.");

        // Act
        String reply = ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert -- the service degrades gracefully: no RAG context but still calls the model
        assertThat(reply).isEqualTo("Reply without docs.");
    }

    @Test
    void should_buildPromptWithEmptyDocs_when_vectorSearchFails() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Vector store offline"));
        mockCallChain(List.of(), "Reply.");

        // Act
        ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert
        ArgumentCaptor<List<Document>> docsCaptor = ArgumentCaptor.forClass(List.class);
        verify(promptBuilder).buildUserPrompt(anyString(), docsCaptor.capture());
        assertThat(docsCaptor.getValue()).isEmpty();
    }

    // ========== chat: Fail-Open ==========

    @Test
    void should_returnFallback_when_chatClientThrowsException() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockVectorSearch(List.of());
        when(chatClient.prompt()).thenThrow(new RuntimeException("Gemini API unavailable"));

        // Act
        String reply = ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert
        assertThat(reply).containsIgnoringCase("contact");
        verify(historyStore, never()).append(anyString(), any(Message.class));
    }

    @Test
    void should_returnFallback_when_modelReturnsNull() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockVectorSearch(List.of());
        mockCallChain(List.of(), null);

        // Act
        String reply = ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert
        assertThat(reply).containsIgnoringCase("contact");
        verify(historyStore, never()).append(anyString(), any(Message.class));
    }

    @Test
    void should_returnFallback_when_modelReturnsBlank() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockVectorSearch(List.of());
        mockCallChain(List.of(), "   ");

        // Act
        String reply = ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert
        assertThat(reply).containsIgnoringCase("contact");
        verify(historyStore, never()).append(anyString(), any(Message.class));
    }

    @Test
    void should_returnFallback_when_historyStoreThrowsException() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID))
                .thenThrow(new RuntimeException("Memory error"));

        // Act
        String reply = ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert -- fail-open contract: any downstream failure yields fallback, not exception
        assertThat(reply).containsIgnoringCase("contact");
    }

    // ========== streamChat: Happy Path ==========

    @Test
    void should_streamChunks_when_streamChatCalled() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockVectorSearch(List.of());
        mockStreamChain(List.of(), Flux.just("Hello ", "world", "!"));

        // Act & Assert
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNext("Hello ")
                .expectNext("world")
                .expectNext("!")
                .verifyComplete();
    }

    @Test
    void should_persistFullResponse_when_streamCompletes() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockVectorSearch(List.of());
        mockStreamChain(List.of(), Flux.just("Hello ", "there", "."));

        // Act
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNextCount(3)
                .verifyComplete();

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(historyStore, org.mockito.Mockito.times(2))
                .append(anyString(), messageCaptor.capture());
        List<Message> captured = messageCaptor.getAllValues();
        assertThat(captured.get(0)).isInstanceOf(UserMessage.class);
        assertThat(captured.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(captured.get(1).getText()).isEqualTo("Hello there.");
    }

    @Test
    void should_includeConversationHistoryInStream_when_historyNotEmpty() {
        // Arrange
        List<Message> history = List.of(new UserMessage("Previous"));
        when(historyStore.history(CONVERSATION_ID)).thenReturn(history);
        mockVectorSearch(List.of());
        mockStreamChain(history, Flux.just("Reply"));

        // Act
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNext("Reply")
                .verifyComplete();

        // Assert
        verify(requestSpec).messages(history);
    }

    // ========== streamChat: Fail-Open ==========

    @Test
    void should_emitFallback_when_streamErrors() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockVectorSearch(List.of());
        mockStreamChain(List.of(), Flux.error(new RuntimeException("Stream failure")));

        // Act & Assert
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNextMatches(msg -> msg.toLowerCase().contains("contact"))
                .verifyComplete();
    }

    @Test
    void should_emitFallback_when_streamSetupThrowsException() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockVectorSearch(List.of());
        when(chatClient.prompt()).thenThrow(new RuntimeException("Setup failed"));

        // Act & Assert
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNextMatches(msg -> msg.toLowerCase().contains("contact"))
                .verifyComplete();
    }

    @Test
    void should_emitFallback_when_historyLookupThrowsOnStream() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID))
                .thenThrow(new RuntimeException("Memory error"));

        // Act & Assert
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNextMatches(msg -> msg.toLowerCase().contains("contact"))
                .verifyComplete();
    }

    @Test
    void should_notPersistHistory_when_streamEmitsBlankContent() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockVectorSearch(List.of());
        mockStreamChain(List.of(), Flux.just("", "   "));

        // Act
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNextCount(2)
                .verifyComplete();

        // Assert -- blank accumulated response means nothing worth persisting
        verify(historyStore, never()).append(anyString(), any(Message.class));
    }

    // ========== Helper Methods ==========

    private void mockVectorSearch(List<Document> documents) {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
    }

    private void mockCallChain(List<Message> history, String responseContent) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        if (!history.isEmpty()) {
            when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        }
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(responseContent);
    }

    private void mockStreamChain(List<Message> history, Flux<String> contentFlux) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        if (!history.isEmpty()) {
            when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        }
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(contentFlux);
    }
}
