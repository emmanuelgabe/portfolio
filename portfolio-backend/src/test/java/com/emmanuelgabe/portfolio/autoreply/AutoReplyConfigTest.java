package com.emmanuelgabe.portfolio.autoreply;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutoReplyConfig} bean selection logic.
 * Verifies that the correct AutoReplyService implementation is wired
 * based on property values and available beans.
 *
 * <p>Uses {@link ApplicationContextRunner} to simulate context loading
 * without starting the full application or requiring infrastructure.</p>
 */
class AutoReplyConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AutoReplyConfig.class)
            .withBean(AutoReplyPromptBuilder.class);

    // ========== AI enabled with all beans ==========

    @Test
    void should_createAiAutoReplyService_whenAiAutoReplyEnabledAndBeansAvailable() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.autoreply.enabled=true")
                .withUserConfiguration(MockAiBeansConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AutoReplyService.class);
                    assertThat(context.getBean(AutoReplyService.class))
                            .isInstanceOf(AiAutoReplyService.class);
                });
    }

    // ========== AI disabled ==========

    @Test
    void should_createNoOpAutoReplyService_whenAiAutoReplyDisabled() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.autoreply.enabled=false")
                .withUserConfiguration(MockAiBeansConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AutoReplyService.class);
                    assertThat(context.getBean(AutoReplyService.class))
                            .isInstanceOf(NoOpAutoReplyService.class);
                });
    }

    @Test
    void should_createNoOpAutoReplyService_whenAiAutoReplyPropertyMissing() {
        // Arrange & Act & Assert
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(AutoReplyService.class);
                    assertThat(context.getBean(AutoReplyService.class))
                            .isInstanceOf(NoOpAutoReplyService.class);
                });
    }

    // ========== AI enabled but beans missing ==========

    @Test
    void should_createNoOpAutoReplyService_whenAiAutoReplyEnabledButChatClientMissing() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.autoreply.enabled=true")
                .withBean(VectorStore.class, () -> mock(VectorStore.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(AutoReplyService.class);
                    assertThat(context.getBean(AutoReplyService.class))
                            .isInstanceOf(NoOpAutoReplyService.class);
                });
    }

    @Test
    void should_createNoOpAutoReplyService_whenAiAutoReplyEnabledButVectorStoreMissing() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.autoreply.enabled=true")
                .withBean(ChatClient.class, () -> mock(ChatClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(AutoReplyService.class);
                    assertThat(context.getBean(AutoReplyService.class))
                            .isInstanceOf(NoOpAutoReplyService.class);
                });
    }

    // ========== Test configurations ==========

    @Configuration
    static class MockAiBeansConfig {

        @Bean
        ChatClient chatClient() {
            return mock(ChatClient.class);
        }

        @Bean
        VectorStore vectorStore() {
            return mock(VectorStore.class);
        }
    }
}
