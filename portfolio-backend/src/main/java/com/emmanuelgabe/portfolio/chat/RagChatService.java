package com.emmanuelgabe.portfolio.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * RAG-powered chat service: retrieves relevant portfolio documents from the
 * vector store, augments the prompt with that context, and delegates generation
 * to the underlying chat model.
 *
 * <p>Conversation memory is preserved per {@code conversationId} via
 * {@link ConversationHistoryStore}. Only the raw user message and assistant
 * response are persisted in history -- the retrieved RAG context is specific
 * to each turn and never stored.</p>
 *
 * <p>Follows the fail-open contract: any vector-store or chat-model failure
 * returns a polite fallback message without propagating the exception.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RagChatService implements ChatService {

    private static final String FALLBACK_MESSAGE =
            "D\u00e9sol\u00e9, je ne peux pas r\u00e9pondre en ce moment. Merci de r\u00e9essayer "
            + "ou d'utiliser le formulaire de contact.";

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ConversationHistoryStore historyStore;
    private final ChatPromptBuilder promptBuilder;

    @Value("${ai.chat.rag.max-documents:5}")
    private int maxDocuments;

    @Value("${ai.chat.rag.similarity-threshold:0.3}")
    private double similarityThreshold;

    @Override
    public String chat(String conversationId, String userMessage) {
        log.debug("[CHAT] Processing message - conversationId={}, length={}",
                conversationId, userMessage != null ? userMessage.length() : 0);

        try {
            List<Message> history = historyStore.history(conversationId);
            List<Document> relevantDocs = searchRelevantDocuments(userMessage, conversationId);
            String augmentedPrompt = promptBuilder.buildUserPrompt(userMessage, relevantDocs);

            String reply = buildPromptSpec(history, augmentedPrompt)
                    .call()
                    .content();

            if (reply == null || reply.isBlank()) {
                log.warn("[CHAT] Empty model reply - conversationId={}", conversationId);
                return FALLBACK_MESSAGE;
            }

            persistTurn(conversationId, userMessage, reply);
            log.info("[CHAT] Reply generated - conversationId={}, replyLength={}, docs={}",
                    conversationId, reply.length(), relevantDocs.size());
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
            List<Document> relevantDocs = searchRelevantDocuments(userMessage, conversationId);
            String augmentedPrompt = promptBuilder.buildUserPrompt(userMessage, relevantDocs);

            StringBuilder fullResponse = new StringBuilder();
            return buildPromptSpec(history, augmentedPrompt)
                    .stream()
                    .content()
                    .doOnNext(fullResponse::append)
                    .doOnComplete(() -> persistStreamingTurn(
                            conversationId, userMessage, fullResponse, relevantDocs.size()))
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

    private ChatClient.ChatClientRequestSpec buildPromptSpec(List<Message> history, String augmentedPrompt) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .system(promptBuilder.buildSystemPrompt());
        if (!history.isEmpty()) {
            spec = spec.messages(history);
        }
        return spec.user(augmentedPrompt);
    }

    private List<Document> searchRelevantDocuments(String query, String conversationId) {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(maxDocuments)
                            .similarityThreshold(similarityThreshold)
                            .build()
            );
            log.debug("[CHAT] Retrieved documents - conversationId={}, count={}",
                    conversationId, docs.size());
            return docs;
        } catch (Exception e) {
            log.warn("[CHAT] Vector search failed, continuing without context - conversationId={}, error={}",
                    conversationId, e.getMessage());
            return List.of();
        }
    }

    private void persistTurn(String conversationId, String userMessage, String reply) {
        historyStore.append(conversationId, new UserMessage(userMessage));
        historyStore.append(conversationId, new AssistantMessage(reply));
    }

    private void persistStreamingTurn(String conversationId, String userMessage,
                                      StringBuilder fullResponse, int documentCount) {
        String reply = fullResponse.toString();
        if (reply.isBlank()) {
            log.warn("[CHAT] Empty streamed reply - conversationId={}", conversationId);
            return;
        }
        persistTurn(conversationId, userMessage, reply);
        log.info("[CHAT] Streamed reply completed - conversationId={}, replyLength={}, docs={}",
                conversationId, reply.length(), documentCount);
    }
}
