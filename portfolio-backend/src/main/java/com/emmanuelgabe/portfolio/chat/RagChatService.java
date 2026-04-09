package com.emmanuelgabe.portfolio.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

import java.util.List;

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

    private final ChatClient chatClient;
    private final ConversationHistoryStore historyStore;
    private final ChatPromptBuilder promptBuilder;
    private final Object[] tools;

    /**
     * @param chatClient    the Spring AI chat client configured with the model
     * @param historyStore  in-memory conversation history with sliding window and TTL
     * @param promptBuilder builds the system prompt and wraps visitor messages
     * @param tools         tool instances with {@code @Tool}-annotated methods
     */
    public RagChatService(ChatClient chatClient, ConversationHistoryStore historyStore,
                          ChatPromptBuilder promptBuilder, Object[] tools) {
        this.chatClient = chatClient;
        this.historyStore = historyStore;
        this.promptBuilder = promptBuilder;
        this.tools = tools;
    }

    @Override
    public String chat(String conversationId, String userMessage) {
        log.debug("[CHAT] Processing message - conversationId={}, length={}",
                conversationId, userMessage != null ? userMessage.length() : 0);

        try {
            List<Message> history = historyStore.history(conversationId);
            String wrappedMessage = promptBuilder.wrapUserMessage(userMessage);

            String reply = buildPromptSpec(history, wrappedMessage)
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
    public Flux<String> streamChat(String conversationId, String userMessage) {
        log.debug("[CHAT] Streaming message - conversationId={}, length={}",
                conversationId, userMessage != null ? userMessage.length() : 0);

        try {
            List<Message> history = historyStore.history(conversationId);
            String wrappedMessage = promptBuilder.wrapUserMessage(userMessage);

            StringBuilder fullResponse = new StringBuilder();
            return buildPromptSpec(history, wrappedMessage)
                    .stream()
                    .content()
                    .doOnNext(fullResponse::append)
                    .doOnComplete(() -> persistStreamingTurn(
                            conversationId, userMessage, fullResponse))
                    .onErrorResume(error -> {
                        log.error("[CHAT] Stream generation failed - conversationId={}, error={}",
                                conversationId, error.getMessage(), error);
                        return Flux.just(FALLBACK_MESSAGE);
                    });

        } catch (Exception e) {
            log.error("[CHAT] Stream setup failed - conversationId={}, error={}",
                    conversationId, e.getMessage(), e);
            return Flux.just(FALLBACK_MESSAGE);
        }
    }

    private ChatClient.ChatClientRequestSpec buildPromptSpec(
            List<Message> history, String wrappedMessage) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .system(promptBuilder.buildSystemPrompt())
                .tools(tools);
        if (!history.isEmpty()) {
            spec = spec.messages(history);
        }
        return spec.user(wrappedMessage);
    }

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
