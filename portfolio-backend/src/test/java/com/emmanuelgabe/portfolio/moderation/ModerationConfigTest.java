package com.emmanuelgabe.portfolio.moderation;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ModerationConfig} bean selection logic.
 * Verifies that the correct ContactModerationService implementation is wired
 * based on property values and available beans.
 *
 * <p>Uses {@link ApplicationContextRunner} to simulate context loading
 * without starting the full application or requiring infrastructure.</p>
 */
class ModerationConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ModerationConfig.class)
            .withBean(ModerationPromptBuilder.class);

    // ========== AI moderation enabled with ChatClient ==========

    @Test
    void should_createAiModerationService_whenModerationEnabledAndChatClientAvailable() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.moderation.enabled=true")
                .withBean(ChatClient.class, () -> mock(ChatClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ContactModerationService.class);
                    assertThat(context.getBean(ContactModerationService.class))
                            .isInstanceOf(AiContactModerationService.class);
                });
    }

    @Test
    void should_createAiModerationService_whenModerationPropertyMissingAndChatClientAvailable() {
        // Arrange & Act & Assert
        // moderation is enabled by default (matchIfMissing equivalent)
        contextRunner
                .withBean(ChatClient.class, () -> mock(ChatClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ContactModerationService.class);
                    assertThat(context.getBean(ContactModerationService.class))
                            .isInstanceOf(AiContactModerationService.class);
                });
    }

    // ========== AI moderation disabled ==========

    @Test
    void should_createNoOpModerationService_whenModerationExplicitlyDisabled() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.moderation.enabled=false")
                .withBean(ChatClient.class, () -> mock(ChatClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ContactModerationService.class);
                    assertThat(context.getBean(ContactModerationService.class))
                            .isInstanceOf(NoOpContactModerationService.class);
                });
    }

    // ========== ChatClient missing ==========

    @Test
    void should_createNoOpModerationService_whenModerationEnabledButChatClientMissing() {
        // Arrange & Act & Assert
        contextRunner
                .withPropertyValues("ai.moderation.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ContactModerationService.class);
                    assertThat(context.getBean(ContactModerationService.class))
                            .isInstanceOf(NoOpContactModerationService.class);
                });
    }

    @Test
    void should_createNoOpModerationService_whenNoPropertiesAndNoChatClient() {
        // Arrange & Act & Assert
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(ContactModerationService.class);
                    assertThat(context.getBean(ContactModerationService.class))
                            .isInstanceOf(NoOpContactModerationService.class);
                });
    }
}
