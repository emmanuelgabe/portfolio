package com.emmanuelgabe.portfolio.moderation;

import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for contact moderation service bean selection.
 * Uses {@link ObjectProvider} for optional dependency injection to avoid
 * bean ordering issues with {@code @ConditionalOnBean}.
 *
 * <p>When {@code ai.moderation.enabled} is not explicitly set to {@code false}
 * and a ChatClient is available, creates {@link AiContactModerationService}.
 * Otherwise falls back to {@link NoOpContactModerationService}.</p>
 */
@Configuration
public class ModerationConfig {

    @Bean
    ContactModerationService contactModerationService(
            ObjectProvider<ChatClient> chatClientProvider,
            ObjectProvider<ModerationPromptBuilder> promptBuilderProvider,
            BusinessMetrics metrics,
            org.springframework.core.env.Environment env) {

        // Moderation is enabled by default (matchIfMissing = true)
        String moderationEnabled = env.getProperty("ai.moderation.enabled", "true");
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        ModerationPromptBuilder promptBuilder = promptBuilderProvider.getIfAvailable();

        if ("true".equalsIgnoreCase(moderationEnabled) && chatClient != null && promptBuilder != null) {
            return new AiContactModerationService(chatClient, promptBuilder, metrics);
        }
        return new NoOpContactModerationService();
    }
}
