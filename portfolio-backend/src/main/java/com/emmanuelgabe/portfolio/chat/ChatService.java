package com.emmanuelgabe.portfolio.chat;

import reactor.core.publisher.Flux;

/**
 * Service for conversational chat with retrieval-augmented generation (RAG).
 * Implementations retrieve portfolio context from the vector store, augment
 * the prompt, and delegate generation to the underlying chat model.
 *
 * <p>Conversation state is tracked per {@code conversationId} so that
 * multi-turn exchanges maintain coherent memory. The caller is responsible
 * for generating and preserving the conversation identifier across requests.</p>
 *
 * <p>Follows the fail-open contract: when the AI backend is unavailable or
 * disabled, implementations return a polite fallback message rather than
 * propagating errors to the HTTP layer.</p>
 */
public interface ChatService {

    /**
     * Generate a synchronous RAG-powered reply.
     *
     * @param conversationId the conversation identifier, used to maintain history
     * @param userMessage    the raw visitor message
     * @return the assistant reply text, or a fallback if generation failed
     */
    String chat(String conversationId, String userMessage);

    /**
     * Generate a streaming RAG-powered reply. Emits response chunks as the
     * model produces them. The stream completes when generation finishes
     * and errors are surfaced as a single fallback message.
     *
     * @param conversationId the conversation identifier, used to maintain history
     * @param userMessage    the raw visitor message
     * @return a flux of response chunks
     */
    Flux<String> streamChat(String conversationId, String userMessage);
}
