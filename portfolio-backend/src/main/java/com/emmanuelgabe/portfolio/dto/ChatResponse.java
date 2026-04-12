package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response payload for the synchronous chat endpoint.
 * Carries the assistant reply along with the conversation identifier that
 * callers should reuse for subsequent turns.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String conversationId;
    private String reply;
    private LocalDateTime timestamp;

    public static ChatResponse of(String conversationId, String reply) {
        return new ChatResponse(conversationId, reply, LocalDateTime.now());
    }
}
