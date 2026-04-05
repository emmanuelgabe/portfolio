package com.emmanuelgabe.portfolio.autoreply;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for auto-reply service bean selection.
 * Uses {@link ObjectProvider} for optional dependency injection to avoid
 * bean ordering issues with {@code @ConditionalOnBean}.
 *
 * <p>When {@code ai.autoreply.enabled=true} and both ChatClient and VectorStore
 * are available, creates {@link AiAutoReplyService}. Otherwise falls back
 * to {@link NoOpAutoReplyService}.</p>
 */
@Configuration
public class AutoReplyConfig {

    @Bean
    AutoReplyService autoReplyService(
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
            ObjectProvider<ChatClient> chatClientProvider,
            ObjectProvider<VectorStore> vectorStoreProvider,
            ObjectProvider<AutoReplyPromptBuilder> promptBuilderProvider,
            org.springframework.core.env.Environment env) {

        boolean enabled = "true".equalsIgnoreCase(env.getProperty("ai.autoreply.enabled"));
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        AutoReplyPromptBuilder promptBuilder = promptBuilderProvider.getIfAvailable();

        if (enabled && chatClient != null && vectorStore != null && promptBuilder != null) {
            return new AiAutoReplyService(chatClient, vectorStore, promptBuilder);
        }
        return new NoOpAutoReplyService();
    }
}
