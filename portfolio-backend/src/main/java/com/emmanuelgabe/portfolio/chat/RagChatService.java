package com.emmanuelgabe.portfolio.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agentic chat service: uses Spring AI function calling to let the LLM
 * decide which portfolio tools to invoke based on the visitor's question.
 *
 * <p>The agent has access to tools for searching projects, articles, skills,
 * experiences, site configuration, CV, and a semantic portfolio search.
 * Spring AI handles the tool-call loop automatically: when the model requests
 * a tool call, Spring AI executes it, feeds the result back, and the model
 * continues reasoning until it produces a final text response.</p>
 *
 * <p>The streaming endpoint emits typed Server-Sent Events so the frontend
 * can display tool call indicators and structured actions (e.g., contact
 * form pre-fill) alongside the progressive text response.</p>
 *
 * <p>Conversation memory is preserved per {@code conversationId} via
 * {@link ConversationHistoryStore}. Only the raw user message and assistant
 * response are stored -- tool call details are not persisted.</p>
 *
 * <p>Follows the fail-open contract: any chat-model or tool failure returns
 * a polite fallback message without propagating the exception.</p>
 */
@Slf4j
public class RagChatService implements ChatService {

    private static final String FALLBACK_MESSAGE =
            "D\u00e9sol\u00e9, je ne peux pas r\u00e9pondre en ce moment. Merci de r\u00e9essayer "
            + "ou d'utiliser le formulaire de contact.";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ChatClient chatClient;
    private final ConversationHistoryStore historyStore;
    private final ChatPromptBuilder promptBuilder;
    private final ChatToolFactory toolFactory;
    private final VectorStore vectorStore;

    /**
     * @param chatClient    the Spring AI chat client configured with the model
     * @param historyStore  in-memory conversation history with sliding window and TTL
     * @param promptBuilder builds the system prompt and wraps visitor messages
     * @param toolFactory   factory for creating tool instances per request
     * @param vectorStore   the Elasticsearch vector store for semantic search
     */
    public RagChatService(ChatClient chatClient, ConversationHistoryStore historyStore,
                          ChatPromptBuilder promptBuilder, ChatToolFactory toolFactory,
                          VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.historyStore = historyStore;
        this.promptBuilder = promptBuilder;
        this.toolFactory = toolFactory;
        this.vectorStore = vectorStore;
    }

    @Override
    public String chat(String conversationId, String userMessage) {
        log.debug("[CHAT] Processing message - conversationId={}, length={}",
                conversationId, userMessage != null ? userMessage.length() : 0);

        try {
            List<Message> history = historyStore.history(conversationId);
            String wrappedMessage = promptBuilder.wrapUserMessage(userMessage);
            Object[] tools = toolFactory.createTools(vectorStore);

            String reply = buildPromptSpec(history, wrappedMessage, tools)
                    .call()
                    .content();

            if (reply == null || reply.isBlank()) {
                log.warn("[CHAT] Empty model reply - conversationId={}", conversationId);
                return FALLBACK_MESSAGE;
            }

            persistTurn(conversationId, userMessage, reply);
            log.info("[CHAT] Reply generated - conversationId={}, replyLength={}",
                    conversationId, reply.length());
            return reply;

        } catch (Exception e) {
            log.error("[CHAT] Generation failed - conversationId={}, error={}",
                    conversationId, e.getMessage(), e);
            return FALLBACK_MESSAGE;
        }
    }

    @Override
    public Flux<ServerSentEvent<String>> streamChat(String conversationId, String userMessage) {
        log.debug("[CHAT] Streaming message - conversationId={}, length={}",
                conversationId, userMessage != null ? userMessage.length() : 0);

        try {
            List<Message> history = historyStore.history(conversationId);
            String wrappedMessage = promptBuilder.wrapUserMessage(userMessage);
            ToolCallTracker tracker = new ToolCallTracker();
            Object[] tools = toolFactory.createTools(vectorStore, tracker);

            StringBuilder fullResponse = new StringBuilder();
            AtomicBoolean firstToken = new AtomicBoolean(true);
            // Tracks how many tool calls have already been reported to the client.
            // Tool calls that occur after the first token (mid-stream) are flushed
            // from buildEndEvents so the frontend can render their indicators too.
            AtomicInteger reportedToolCount = new AtomicInteger(0);

            return buildPromptSpec(history, wrappedMessage, tools)
                    .stream()
                    .content()
                    .concatMap(token -> {
                        fullResponse.append(token);
                        if (firstToken.compareAndSet(true, false)) {
                            return buildFirstTokenEvents(tracker, token, reportedToolCount);
                        }
                        return Flux.just(tokenEvent(token));
                    })
                    .concatWith(Flux.defer(
                            () -> buildEndEvents(conversationId, userMessage,
                                    fullResponse, tracker, reportedToolCount)))
                    .onErrorResume(error -> {
                        log.error("[CHAT] Stream generation failed - conversationId={}, error={}",
                                conversationId, error.getMessage(), error);
                        return Flux.just(tokenEvent(FALLBACK_MESSAGE), doneEvent());
                    });

        } catch (Exception e) {
            log.error("[CHAT] Stream setup failed - conversationId={}, error={}",
                    conversationId, e.getMessage(), e);
            return Flux.just(tokenEvent(FALLBACK_MESSAGE), doneEvent());
        }
    }

    private ChatClient.ChatClientRequestSpec buildPromptSpec(
            List<Message> history, String wrappedMessage, Object[] tools) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .system(promptBuilder.buildSystemPrompt())
                .tools(tools);
        if (!history.isEmpty()) {
            spec = spec.messages(history);
        }
        return spec.user(wrappedMessage);
    }

    // ========== SSE event builders ==========

    /**
     * Before the first text token, emit tool_calls metadata (if any tools ran),
     * then the first token itself. Updates {@code reportedToolCount} so that
     * {@link #buildEndEvents} can detect tool calls that happen mid-stream.
     */
    private Flux<ServerSentEvent<String>> buildFirstTokenEvents(
            ToolCallTracker tracker, String token, AtomicInteger reportedToolCount) {
        List<String> toolCalls = tracker.getToolCalls();
        if (toolCalls.isEmpty()) {
            return Flux.just(tokenEvent(token));
        }

        reportedToolCount.set(toolCalls.size());
        List<ServerSentEvent<String>> events = new ArrayList<>();
        events.add(ServerSentEvent.<String>builder()
                .event("tool_calls")
                .data(toJson(toolCalls))
                .build());
        events.add(tokenEvent(token));
        return Flux.fromIterable(events);
    }

    /**
     * After all tokens: flush any tool calls that were recorded after the first
     * token, emit contact_form data if prepareContact was used, then the done
     * event. Also persists the conversation turn.
     *
     * <p>Re-emits the full tool list (not a delta) so the frontend can
     * overwrite its indicator set without stateful merging.</p>
     */
    private Flux<ServerSentEvent<String>> buildEndEvents(
            String conversationId, String userMessage,
            StringBuilder fullResponse, ToolCallTracker tracker,
            AtomicInteger reportedToolCount) {
        persistStreamingTurn(conversationId, userMessage, fullResponse);

        List<ServerSentEvent<String>> events = new ArrayList<>();
        List<String> toolCalls = tracker.getToolCalls();
        if (toolCalls.size() > reportedToolCount.get()) {
            events.add(ServerSentEvent.<String>builder()
                    .event("tool_calls")
                    .data(toJson(toolCalls))
                    .build());
            reportedToolCount.set(toolCalls.size());
        }
        tracker.getContactFormData().ifPresent(data ->
                events.add(ServerSentEvent.<String>builder()
                        .event("contact_form")
                        .data(toJson(data))
                        .build())
        );
        events.add(doneEvent());
        return Flux.fromIterable(events);
    }

    private static ServerSentEvent<String> tokenEvent(String token) {
        return ServerSentEvent.<String>builder()
                .event("token")
                .data(token)
                .build();
    }

    private static ServerSentEvent<String> doneEvent() {
        return ServerSentEvent.<String>builder()
                .event("done")
                .data("")
                .build();
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("[CHAT] JSON serialization failed", e);
            return "{}";
        }
    }

    // ========== Conversation persistence ==========

    private void persistTurn(String conversationId, String userMessage, String reply) {
        historyStore.append(conversationId, new UserMessage(userMessage));
        historyStore.append(conversationId, new AssistantMessage(reply));
    }

    private void persistStreamingTurn(String conversationId, String userMessage,
                                      StringBuilder fullResponse) {
        String reply = fullResponse.toString();
        if (reply.isBlank()) {
            log.warn("[CHAT] Empty streamed reply - conversationId={}", conversationId);
            return;
        }
        persistTurn(conversationId, userMessage, reply);
        log.info("[CHAT] Streamed reply completed - conversationId={}, replyLength={}",
                conversationId, reply.length());
    }
}
