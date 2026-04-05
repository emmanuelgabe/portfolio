package com.emmanuelgabe.portfolio.config;

import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions.TaskType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI configuration for Spring AI with Google Gemini.
 * All AI beans are conditionally loaded based on the {@code ai.enabled} property.
 * When disabled, no AI-related beans are created and no API calls are made.
 *
 * <p>Uses non-starter Spring AI modules for full control over bean lifecycle.
 * The Google GenAI SDK {@link Client} is configured with an API key for the
 * Gemini Developer API (free tier).</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true", matchIfMissing = false)
public class AiConfig {

    @Bean
    Client googleGenAiClient(
            @Value("${spring.ai.google.genai.api-key:}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "ai.enabled=true but GOOGLE_GENAI_API_KEY is not set. "
                    + "Set the environment variable or disable AI with AI_ENABLED=false");
        }
        log.info("[AI] Initializing Google GenAI client");
        return Client.builder().apiKey(apiKey).build();
    }

    @Bean
    ChatModel chatModel(
            Client googleGenAiClient,
            @Value("${spring.ai.google.genai.chat.options.model}") String model,
            @Value("${spring.ai.google.genai.chat.options.temperature}") double temperature,
            @Value("${spring.ai.google.genai.chat.options.max-output-tokens}") int maxOutputTokens) {
        log.info("[AI] Initializing ChatModel - model={}, temperature={}, maxTokens={}",
                model, temperature, maxOutputTokens);
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxOutputTokens(maxOutputTokens)
                .build();
        return GoogleGenAiChatModel.builder()
                .genAiClient(googleGenAiClient)
                .defaultOptions(options)
                .build();
    }

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        log.info("[AI] Initializing ChatClient");
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    EmbeddingModel embeddingModel(
            @Value("${spring.ai.google.genai.api-key}") String apiKey,
            @Value("${spring.ai.google.genai.embedding.text.options.model:text-embedding-004}") String model) {
        log.info("[AI] Initializing EmbeddingModel - model={}", model);
        GoogleGenAiEmbeddingConnectionDetails connectionDetails =
                GoogleGenAiEmbeddingConnectionDetails.builder()
                        .apiKey(apiKey)
                        .build();
        GoogleGenAiTextEmbeddingOptions options = GoogleGenAiTextEmbeddingOptions.builder()
                .model(model)
                .taskType(TaskType.RETRIEVAL_DOCUMENT)
                .build();
        return new GoogleGenAiTextEmbeddingModel(connectionDetails, options);
    }
}
