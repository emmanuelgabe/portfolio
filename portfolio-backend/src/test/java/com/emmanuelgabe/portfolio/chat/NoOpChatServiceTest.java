package com.emmanuelgabe.portfolio.chat;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpChatServiceTest {

    private final NoOpChatService service = new NoOpChatService();

    @Test
    void should_returnFallbackMessage_when_chatCalled() {
        // Act
        String reply = service.chat("conversation-1", "Hello");

        // Assert
        assertThat(reply).isNotBlank();
        assertThat(reply).containsIgnoringCase("contact");
    }

    @Test
    void should_returnFallbackMessage_when_chatCalledWithNullInputs() {
        // Act
        String reply = service.chat(null, null);

        // Assert -- no NPE, returns fallback
        assertThat(reply).isNotBlank();
    }

    @Test
    void should_returnSingleElementFlux_when_streamChatCalled() {
        // Act & Assert
        StepVerifier.create(service.streamChat("conversation-1", "Hello"))
                .expectNextMatches(msg -> msg != null && !msg.isBlank())
                .verifyComplete();
    }
}
