package com.emmanuelgabe.portfolio.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagChatServiceTest {

    private static final String CONVERSATION_ID = "conv-abcd1234";
    private static final String USER_MESSAGE = "Tell me about your Spring projects";

    @Mock
    private ChatClient chatClient;

    @Mock
    private ConversationHistoryStore historyStore;

    @Mock
    private ChatPromptBuilder promptBuilder;

    @Mock
    private ChatToolFactory toolFactory;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ChatClient.StreamResponseSpec streamResponseSpec;

    private RagChatService ragChatService;

    @BeforeEach
    void setUp() {
        ragChatService = new RagChatService(
                chatClient, historyStore, promptBuilder, toolFactory, vectorStore);

        lenient().when(promptBuilder.buildSystemPrompt()).thenReturn("system prompt");
        lenient().when(promptBuilder.wrapUserMessage(anyString()))
                .thenReturn("<visitor_message>msg</visitor_message>");
        lenient().when(toolFactory.createTools(any(VectorStore.class)))
                .thenReturn(new Object[0]);
        lenient().when(toolFactory.createTools(any(VectorStore.class), any(ToolCallTracker.class)))
                .thenReturn(new Object[0]);
    }

    // ========== chat: Happy Path ==========

    @Test
    void should_returnReply_when_chatCalledWithValidInput() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockCallChain(List.of(), "Here are my Spring projects.");

        // Act
        String reply = ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert
        assertThat(reply).isEqualTo("Here are my Spring projects.");
    }

    @Test
    void should_includeConversationHistory_when_historyNotEmpty() {
        // Arrange
        List<Message> history = List.of(
                new UserMessage("Previous question"),
                new AssistantMessage("Previous answer")
        );
        when(historyStore.history(CONVERSATION_ID)).thenReturn(history);
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
        mockCallChain(List.of(), "Generated reply.");

        // Act
        ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(historyStore, times(2)).append(anyString(), messageCaptor.capture());
        List<Message> captured = messageCaptor.getAllValues();
        assertThat(captured.get(0)).isInstanceOf(UserMessage.class);
        assertThat(captured.get(0).getText()).isEqualTo(USER_MESSAGE);
        assertThat(captured.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(captured.get(1).getText()).isEqualTo("Generated reply.");
    }

    @Test
    void should_registerTools_when_buildingPromptSpec() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockCallChain(List.of(), "Reply with tools.");

        // Act
        ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert
        verify(requestSpec).tools(any(Object[].class));
    }

    @Test
    void should_wrapUserMessage_when_buildingPrompt() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockCallChain(List.of(), "Reply.");

        // Act
        ragChatService.chat(CONVERSATION_ID, USER_MESSAGE);

        // Assert
        verify(promptBuilder).wrapUserMessage(USER_MESSAGE);
    }

    // ========== chat: Fail-Open ==========

    @Test
    void should_returnFallback_when_chatClientThrowsException() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
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

        // Assert
        assertThat(reply).containsIgnoringCase("contact");
    }

    // ========== streamChat: Happy Path ==========

    @Test
    void should_streamTokenEvents_when_streamChatCalled() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockStreamChain(List.of(), Flux.just("Hello ", "world", "!"));

        // Act & Assert
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNextMatches(sse -> isToken(sse, "Hello "))
                .expectNextMatches(sse -> isToken(sse, "world"))
                .expectNextMatches(sse -> isToken(sse, "!"))
                .expectNextMatches(this::isDone)
                .verifyComplete();
    }

    @Test
    void should_persistFullResponse_when_streamCompletes() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockStreamChain(List.of(), Flux.just("Hello ", "there", "."));

        // Act
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNextCount(4)
                .verifyComplete();

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(historyStore, times(2)).append(anyString(), messageCaptor.capture());
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
        mockStreamChain(history, Flux.just("Reply"));

        // Act
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNextMatches(sse -> isToken(sse, "Reply"))
                .expectNextMatches(this::isDone)
                .verifyComplete();

        // Assert
        verify(requestSpec).messages(history);
    }

    // ========== streamChat: Fail-Open ==========

    @Test
    void should_emitFallback_when_streamErrors() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockStreamChain(List.of(), Flux.error(new RuntimeException("Stream failure")));

        // Act & Assert
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNextMatches(sse -> "token".equals(sse.event())
                        && sse.data() != null
                        && sse.data().toLowerCase().contains("contact"))
                .expectNextMatches(this::isDone)
                .verifyComplete();
    }

    @Test
    void should_emitFallback_when_streamSetupThrowsException() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        when(chatClient.prompt()).thenThrow(new RuntimeException("Setup failed"));

        // Act & Assert
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNextMatches(sse -> "token".equals(sse.event())
                        && sse.data() != null
                        && sse.data().toLowerCase().contains("contact"))
                .expectNextMatches(this::isDone)
                .verifyComplete();
    }

    @Test
    void should_emitFallback_when_historyLookupThrowsOnStream() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID))
                .thenThrow(new RuntimeException("Memory error"));

        // Act & Assert
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNextMatches(sse -> "token".equals(sse.event())
                        && sse.data() != null
                        && sse.data().toLowerCase().contains("contact"))
                .expectNextMatches(this::isDone)
                .verifyComplete();
    }

    @Test
    void should_notPersistHistory_when_streamEmitsBlankContent() {
        // Arrange
        when(historyStore.history(CONVERSATION_ID)).thenReturn(List.of());
        mockStreamChain(List.of(), Flux.just("", "   "));

        // Act
        StepVerifier.create(ragChatService.streamChat(CONVERSATION_ID, USER_MESSAGE))
                .expectNextCount(3)
                .verifyComplete();

        // Assert
        verify(historyStore, never()).append(anyString(), any(Message.class));
    }

    // ========== Helper Methods ==========

    private boolean isToken(ServerSentEvent<String> sse, String expected) {
        return "token".equals(sse.event()) && expected.equals(sse.data());
    }

    private boolean isDone(ServerSentEvent<String> sse) {
        return "done".equals(sse.event());
    }

    private void mockCallChain(List<Message> history, String responseContent) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
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
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        if (!history.isEmpty()) {
            when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        }
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(contentFlux);
    }
}
