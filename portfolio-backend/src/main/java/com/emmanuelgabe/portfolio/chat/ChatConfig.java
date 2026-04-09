package com.emmanuelgabe.portfolio.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration for chat service bean selection.
 *
 * <p>When {@code ai.enabled=true} and both {@link ChatClient} and {@link VectorStore}
 * are available, creates {@link RagChatService} with all agent tools registered.
 * Otherwise falls back to {@link NoOpChatService} so the chat endpoint remains callable.</p>
 *
 * <p>Tool instances are created via {@link ChatToolFactory} to keep the parameter
 * count manageable and improve testability.</p>
 */
@Configuration
public class ChatConfig {

    @Bean
    ChatService chatService(
            ObjectProvider<ChatClient> chatClientProvider,
            ObjectProvider<VectorStore> vectorStoreProvider,
            ObjectProvider<ChatPromptBuilder> promptBuilderProvider,
            ObjectProvider<ConversationHistoryStore> historyStoreProvider,
            ChatToolFactory toolFactory,
            Environment environment) {

        boolean aiEnabled = Boolean.parseBoolean(
                environment.getProperty("ai.enabled", "false"));
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        ChatPromptBuilder promptBuilder = promptBuilderProvider.getIfAvailable();
        ConversationHistoryStore historyStore = historyStoreProvider.getIfAvailable();

        if (aiEnabled && chatClient != null && vectorStore != null
                && promptBuilder != null && historyStore != null) {
            Object[] tools = toolFactory.createTools(vectorStore);
            return new RagChatService(chatClient, historyStore, promptBuilder, tools);
        }
        return new NoOpChatService();
    }
}
