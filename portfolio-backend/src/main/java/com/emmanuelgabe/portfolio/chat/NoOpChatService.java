package com.emmanuelgabe.portfolio.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * No-operation chat service used when AI is disabled or the vector store is
 * unavailable. Returns a static fallback message without calling any external
 * service, keeping the chat endpoint alive but non-functional.
 */
@Slf4j
public class NoOpChatService implements ChatService {

    private static final String DISABLED_MESSAGE =
            "Le chat est temporairement indisponible. Merci d'utiliser le "
            + "formulaire de contact pour me joindre.";

    @Override
    public String chat(String conversationId, String userMessage) {
        log.debug("[CHAT] Service disabled, returning fallback - conversationId={}", conversationId);
        return DISABLED_MESSAGE;
    }

    @Override
    public Flux<ServerSentEvent<String>> streamChat(String conversationId, String userMessage) {
        log.debug("[CHAT] Service disabled, returning fallback stream - conversationId={}", conversationId);
        return Flux.just(
                ServerSentEvent.<String>builder().event("token").data(DISABLED_MESSAGE).build(),
                ServerSentEvent.<String>builder().event("done").data("").build()
        );
    }
}
