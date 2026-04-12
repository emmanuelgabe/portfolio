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
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

/**
 * Public chat endpoint backed by a RAG pipeline. Both a synchronous endpoint
 * and a server-sent-events streaming endpoint are exposed so that the frontend
 * can progressively render tokens as they are produced by the model.
 *
 * <p>Rate limiting is applied per visitor IP via the generic {@link RateLimiter}
 * infrastructure with a dedicated {@code chat} bucket. Every response includes
 * {@code X-RateLimit-Limit} and {@code X-RateLimit-Remaining} headers so the
 * frontend can display quota information without hardcoding values.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    static final String RATE_LIMIT_HEADER = "X-RateLimit-Limit";
    static final String RATE_REMAINING_HEADER = "X-RateLimit-Remaining";

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
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(RATE_LIMIT_HEADER, String.valueOf(maxRequestsPerHour))
                    .header(RATE_REMAINING_HEADER, "0")
                    .build();
        }

        long remaining = rateLimiter.remainingQuota(policy, ip);
        String reply = chatService.chat(request.getConversationId(), request.getMessage());

        return ResponseEntity.ok()
                .header(RATE_LIMIT_HEADER, String.valueOf(maxRequestsPerHour))
                .header(RATE_REMAINING_HEADER, String.valueOf(remaining))
                .body(ChatResponse.of(request.getConversationId(), reply));
    }

    /**
     * SSE streaming endpoint. Uses {@link SseEmitter} instead of
     * {@code ResponseEntity<Flux<...>>} because Spring MVC (servlet stack) does not
     * stream a {@code Flux} body wrapped in a {@code ResponseEntity} — the reactive
     * type is serialized as JSON rather than being written as server-sent events.
     * {@code SseEmitter} is Spring MVC's native SSE mechanism and works correctly
     * in this context.
     */
    @PostMapping(
            value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SseEmitter> streamChat(
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {

        String ip = IpAddressExtractor.extractIpAddress(httpRequest);
        log.info("[CHAT] Stream request received - ip={}, conversationId={}",
                ip, request.getConversationId());

        RateLimitPolicy policy = RateLimitPolicy.perHour(RATE_LIMIT_BUCKET, maxRequestsPerHour);
        if (!rateLimiter.tryAcquire(policy, ip)) {
            log.warn("[CHAT] Rate limit exceeded on stream - ip={}", ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(RATE_LIMIT_HEADER, String.valueOf(maxRequestsPerHour))
                    .header(RATE_REMAINING_HEADER, "0")
                    .build();
        }

        long remaining = rateLimiter.remainingQuota(policy, ip);
        SseEmitter emitter = new SseEmitter(180_000L);

        Disposable subscription = chatService.streamChat(
                        request.getConversationId(), request.getMessage())
                .subscribe(
                        event -> {
                            try {
                                emitter.send(buildSseEvent(event));
                            } catch (Exception e) {
                                log.warn("[CHAT] SSE send failed - conversationId={}",
                                        request.getConversationId());
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.error("[CHAT] Stream subscription error - conversationId={}",
                                    request.getConversationId(), error);
                            emitter.completeWithError(error);
                        },
                        emitter::complete
                );

        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(subscription::dispose);
        emitter.onError(e -> subscription.dispose());

        return ResponseEntity.ok()
                .header(RATE_LIMIT_HEADER, String.valueOf(maxRequestsPerHour))
                .header(RATE_REMAINING_HEADER, String.valueOf(remaining))
                .body(emitter);
    }

    /**
     * Build a spec-compliant SSE event from a {@link ServerSentEvent}. When the
     * payload contains newlines, the SSE protocol requires each line to be sent
     * as a separate {@code data:} field; the receiver is then responsible for
     * re-joining them with {@code \n}. Calling {@link SseEmitter.SseEventBuilder#data}
     * multiple times on the same builder produces this layout. This guards
     * against LLM tokens that contain embedded newlines breaking the stream.
     */
    private SseEmitter.SseEventBuilder buildSseEvent(ServerSentEvent<String> event) {
        SseEmitter.SseEventBuilder builder = SseEmitter.event()
                .name(event.event() != null ? event.event() : "message");
        String data = event.data() != null ? event.data() : "";
        // split with limit=-1 so trailing empty segments are preserved
        String[] lines = data.split("\n", -1);
        for (String line : lines) {
            builder.data(line, MediaType.TEXT_PLAIN);
        }
        return builder;
    }
}
