package com.emmanuelgabe.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for the visitor chat endpoints.
 * Callers must supply a stable {@code conversationId} (typically a client-generated UUID)
 * so that the server can maintain per-session message history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "conversationId is required")
    @Size(min = 8, max = 64, message = "conversationId must be between 8 and 64 characters")
    private String conversationId;

    @NotBlank(message = "message is required")
    @Size(min = 1, max = 2000, message = "message must be between 1 and 2000 characters")
    private String message;
}
