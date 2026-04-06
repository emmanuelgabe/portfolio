package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.chat.ChatService;
import com.emmanuelgabe.portfolio.dto.ChatRequest;
import com.emmanuelgabe.portfolio.dto.ChatResponse;
import com.emmanuelgabe.portfolio.ratelimit.RateLimitPolicy;
import com.emmanuelgabe.portfolio.ratelimit.RateLimiter;
import com.emmanuelgabe.portfolio.util.IpAddressExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Public chat endpoint backed by a RAG pipeline. Both a synchronous endpoint
 * and a server-sent-events streaming endpoint are exposed so that the frontend
 * can progressively render tokens as they are produced by the model.
 *
 * <p>Rate limiting is applied per visitor IP via the generic {@link RateLimiter}
 * infrastructure with a dedicated {@code chat} bucket.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final String RATE_LIMIT_BUCKET = "chat";

    private final ChatService chatService;
    private final RateLimiter rateLimiter;

    @Value("${ai.chat.rate-limit.max-requests-per-hour:10}")
    private int maxRequestsPerHour;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {

        String ip = IpAddressExtractor.extractIpAddress(httpRequest);
        log.info("[CHAT] Request received - ip={}, conversationId={}", ip, request.getConversationId());

        RateLimitPolicy policy = RateLimitPolicy.perHour(RATE_LIMIT_BUCKET, maxRequestsPerHour);
        if (!rateLimiter.tryAcquire(policy, ip)) {
            log.warn("[CHAT] Rate limit exceeded - ip={}", ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        String reply = chatService.chat(request.getConversationId(), request.getMessage());
        return ResponseEntity.ok(ChatResponse.of(request.getConversationId(), reply));
    }

    @PostMapping(
            value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> streamChat(
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {

        String ip = IpAddressExtractor.extractIpAddress(httpRequest);
        log.info("[CHAT] Stream request received - ip={}, conversationId={}",
                ip, request.getConversationId());

        RateLimitPolicy policy = RateLimitPolicy.perHour(RATE_LIMIT_BUCKET, maxRequestsPerHour);
        if (!rateLimiter.tryAcquire(policy, ip)) {
            log.warn("[CHAT] Rate limit exceeded on stream - ip={}", ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        Flux<String> stream = chatService.streamChat(request.getConversationId(), request.getMessage());
        return ResponseEntity.ok(stream);
    }
}
