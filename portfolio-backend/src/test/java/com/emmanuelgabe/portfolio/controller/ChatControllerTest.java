package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.chat.ChatService;
import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.ChatRequest;
import com.emmanuelgabe.portfolio.ratelimit.RateLimitPolicy;
import com.emmanuelgabe.portfolio.ratelimit.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class ChatControllerTest {

    private static final String CONVERSATION_ID = "conv-abcd1234";
    private static final String VALID_MESSAGE = "Tell me about your Spring projects";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private RateLimiter rateLimiter;

    // ========== POST /api/chat: Happy Path ==========

    @Test
    void should_returnReply_when_chatRequestValidAndNotRateLimited() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest(CONVERSATION_ID, VALID_MESSAGE);
        when(rateLimiter.tryAcquire(any(RateLimitPolicy.class), anyString())).thenReturn(true);
        when(chatService.chat(CONVERSATION_ID, VALID_MESSAGE)).thenReturn("Here are my projects.");

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(CONVERSATION_ID))
                .andExpect(jsonPath("$.reply").value("Here are my projects."))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(rateLimiter).tryAcquire(any(RateLimitPolicy.class), anyString());
        verify(chatService).chat(CONVERSATION_ID, VALID_MESSAGE);
    }

    // ========== POST /api/chat: Rate Limiting ==========

    @Test
    void should_returnTooManyRequests_when_rateLimitExceeded() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest(CONVERSATION_ID, VALID_MESSAGE);
        when(rateLimiter.tryAcquire(any(RateLimitPolicy.class), anyString())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());

        verify(chatService, never()).chat(anyString(), anyString());
    }

    @Test
    void should_useChatBucket_when_rateLimitChecked() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest(CONVERSATION_ID, VALID_MESSAGE);
        when(rateLimiter.tryAcquire(any(RateLimitPolicy.class), anyString())).thenReturn(true);
        when(chatService.chat(anyString(), anyString())).thenReturn("OK");

        // Act
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Assert
        ArgumentCaptor<RateLimitPolicy> policyCaptor = ArgumentCaptor.forClass(RateLimitPolicy.class);
        verify(rateLimiter).tryAcquire(policyCaptor.capture(), anyString());
        assertThat(policyCaptor.getValue().bucketName()).isEqualTo("chat");
    }

    // ========== POST /api/chat: Validation ==========

    @Test
    void should_returnBadRequest_when_messageBlank() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest(CONVERSATION_ID, "");

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(rateLimiter, never()).tryAcquire(any(RateLimitPolicy.class), anyString());
        verify(chatService, never()).chat(anyString(), anyString());
    }

    @Test
    void should_returnBadRequest_when_conversationIdBlank() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest("", VALID_MESSAGE);

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).chat(anyString(), anyString());
    }

    @Test
    void should_returnBadRequest_when_conversationIdTooShort() throws Exception {
        // Arrange -- min size is 8
        ChatRequest request = new ChatRequest("short", VALID_MESSAGE);

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).chat(anyString(), anyString());
    }

    @Test
    void should_returnBadRequest_when_conversationIdTooLong() throws Exception {
        // Arrange -- max size is 64
        ChatRequest request = new ChatRequest("x".repeat(65), VALID_MESSAGE);

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).chat(anyString(), anyString());
    }

    @Test
    void should_returnBadRequest_when_messageTooLong() throws Exception {
        // Arrange -- max size is 2000
        ChatRequest request = new ChatRequest(CONVERSATION_ID, "x".repeat(2001));

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).chat(anyString(), anyString());
    }

    // ========== POST /api/chat: IP Extraction ==========

    @Test
    void should_extractIpFromXForwardedForHeader_when_present() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest(CONVERSATION_ID, VALID_MESSAGE);
        when(rateLimiter.tryAcquire(any(RateLimitPolicy.class), eq("203.0.113.5"))).thenReturn(true);
        when(chatService.chat(anyString(), anyString())).thenReturn("OK");

        // Act & Assert
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.5, 10.0.0.1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(rateLimiter).tryAcquire(any(RateLimitPolicy.class), eq("203.0.113.5"));
    }

    // ========== POST /api/chat/stream: Happy Path ==========

    @Test
    void should_returnStreamOk_when_streamRequestValid() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest(CONVERSATION_ID, VALID_MESSAGE);
        when(rateLimiter.tryAcquire(any(RateLimitPolicy.class), anyString())).thenReturn(true);
        when(chatService.streamChat(CONVERSATION_ID, VALID_MESSAGE))
                .thenReturn(Flux.just(
                        ServerSentEvent.<String>builder().event("token").data("Hello world").build(),
                        ServerSentEvent.<String>builder().event("done").data("").build()));

        // Act -- Flux response body requires async dispatch
        MvcResult result = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Assert
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk());
        verify(chatService).streamChat(CONVERSATION_ID, VALID_MESSAGE);
    }

    // ========== POST /api/chat/stream: Rate Limiting ==========

    @Test
    void should_returnTooManyRequests_when_streamRateLimitExceeded() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest(CONVERSATION_ID, VALID_MESSAGE);
        when(rateLimiter.tryAcquire(any(RateLimitPolicy.class), anyString())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());

        verify(chatService, never()).streamChat(anyString(), anyString());
    }

    // ========== POST /api/chat/stream: Validation ==========

    @Test
    void should_returnBadRequest_when_streamRequestInvalid() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest(CONVERSATION_ID, "");

        // Act & Assert
        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(rateLimiter, never()).tryAcquire(any(RateLimitPolicy.class), anyString());
        verify(chatService, never()).streamChat(anyString(), anyString());
    }
}
