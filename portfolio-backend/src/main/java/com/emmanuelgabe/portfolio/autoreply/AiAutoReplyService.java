package com.emmanuelgabe.portfolio.autoreply;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * AI-powered auto-reply service using RAG (Retrieval-Augmented Generation).
 * Queries the vector store for relevant portfolio content, then generates
 * a contextual reply via Google Gemini.
 *
 * <p>Follows the fail-open contract: any failure (timeout, API error, empty result)
 * returns {@link Optional#empty()} to avoid blocking the contact flow.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class AiAutoReplyService implements AutoReplyService {

    private final ExecutorService aiExecutor = Executors.newFixedThreadPool(2);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final AutoReplyPromptBuilder promptBuilder;

    @PreDestroy
    void shutdown() {
        aiExecutor.shutdownNow();
    }

    @Value("${ai.autoreply.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${ai.autoreply.max-documents:5}")
    private int maxDocuments;

    @Override
    public Optional<String> generateReply(String senderName, String senderEmail,
                                          String subject, String message) {
        try {
            log.debug("[AUTO_REPLY] Starting reply generation - email={}", senderEmail);

            List<Document> relevantDocs = searchRelevantDocuments(subject, message, senderEmail);

            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(senderName, message, relevantDocs);

            String reply = CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                            .system(systemPrompt)
                            .user(userPrompt)
                            .call()
                            .content(),
                    aiExecutor
            ).get(timeoutSeconds, TimeUnit.SECONDS);

            if (reply == null || reply.isBlank()) {
                log.warn("[AUTO_REPLY] AI returned empty reply - email={}", senderEmail);
                return Optional.empty();
            }

            log.info("[AUTO_REPLY] Reply generated - email={}, length={}", senderEmail, reply.length());
            return Optional.of(reply);

        } catch (Exception e) {
            log.error("[AUTO_REPLY] Reply generation failed - email={}, error={}",
                    senderEmail, e.getMessage());
            return Optional.empty();
        }
    }

    private List<Document> searchRelevantDocuments(String subject, String message, String email) {
        try {
            String searchQuery = subject + " " + message;
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(searchQuery)
                            .topK(maxDocuments)
                            .similarityThreshold(0.3)
                            .build()
            );
            log.debug("[AUTO_REPLY] Found {} relevant documents - email={}", docs.size(), email);
            return docs;
        } catch (Exception e) {
            log.warn("[AUTO_REPLY] Vector store search failed, proceeding without context - email={}, error={}",
                    email, e.getMessage());
            return List.of();
        }
    }
}
