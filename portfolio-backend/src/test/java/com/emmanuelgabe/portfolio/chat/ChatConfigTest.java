package com.emmanuelgabe.portfolio.chat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChatConfig} bean selection logic.
 * Verifies that the correct ChatService implementation is wired
 * based on the {@code ai.enabled} property and available optional beans.
 *
 * <p>Uses {@link ApplicationContextRunner} to simulate context loading
 * without starting the full application or requiring infrastructure.</p>
 */
class ChatConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ChatConfig.class)
            .withBean(ChatToolFactory.class, () -> {
                ChatToolFactory factory = mock(ChatToolFactory.class);
                when(factory.createTools(any())).thenReturn(new Object[0]);
                return factory;
            });

    // ========== AI enabled with all dependencies ==========

    @Test
    void should_createRagChatService_when_aiEnabledAndAllDependenciesAvailable() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.enabled=true")
                .withBean(ChatClient.class, () -> mock(ChatClient.class))
                .withBean(VectorStore.class, () -> mock(VectorStore.class))
                .withBean(ChatPromptBuilder.class)
                .withBean(ConversationHistoryStore.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatService.class);
                    assertThat(context.getBean(ChatService.class))
                            .isInstanceOf(RagChatService.class);
                });
    }

    // ========== AI disabled ==========

    @Test
    void should_createNoOpChatService_when_aiExplicitlyDisabled() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.enabled=false")
                .withBean(ChatClient.class, () -> mock(ChatClient.class))
                .withBean(VectorStore.class, () -> mock(VectorStore.class))
                .withBean(ChatPromptBuilder.class)
                .withBean(ConversationHistoryStore.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatService.class);
                    assertThat(context.getBean(ChatService.class))
                            .isInstanceOf(NoOpChatService.class);
                });
    }

    // ========== Missing dependencies ==========

    @Test
    void should_createNoOpChatService_when_aiEnabledButChatClientMissing() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.enabled=true")
                .withBean(VectorStore.class, () -> mock(VectorStore.class))
                .withBean(ChatPromptBuilder.class)
                .withBean(ConversationHistoryStore.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatService.class);
                    assertThat(context.getBean(ChatService.class))
                            .isInstanceOf(NoOpChatService.class);
                });
    }

    @Test
    void should_createNoOpChatService_when_aiEnabledButVectorStoreMissing() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.enabled=true")
                .withBean(ChatClient.class, () -> mock(ChatClient.class))
                .withBean(ChatPromptBuilder.class)
                .withBean(ConversationHistoryStore.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatService.class);
                    assertThat(context.getBean(ChatService.class))
                            .isInstanceOf(NoOpChatService.class);
                });
    }

    @Test
    void should_createNoOpChatService_when_aiEnabledButPromptBuilderMissing() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.enabled=true")
                .withBean(ChatClient.class, () -> mock(ChatClient.class))
                .withBean(VectorStore.class, () -> mock(VectorStore.class))
                .withBean(ConversationHistoryStore.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatService.class);
                    assertThat(context.getBean(ChatService.class))
                            .isInstanceOf(NoOpChatService.class);
                });
    }

    @Test
    void should_createNoOpChatService_when_aiEnabledButHistoryStoreMissing() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.enabled=true")
                .withBean(ChatClient.class, () -> mock(ChatClient.class))
                .withBean(VectorStore.class, () -> mock(VectorStore.class))
                .withBean(ChatPromptBuilder.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatService.class);
                    assertThat(context.getBean(ChatService.class))
                            .isInstanceOf(NoOpChatService.class);
                });
    }

    @Test
    void should_createNoOpChatService_when_aiEnabledButAiDependenciesMissing() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatService.class);
                    assertThat(context.getBean(ChatService.class))
                            .isInstanceOf(NoOpChatService.class);
                });
    }
}
