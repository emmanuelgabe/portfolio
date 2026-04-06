package com.emmanuelgabe.portfolio.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory conversation history store for chat sessions.
 * Maintains a bounded sliding window of messages per conversation identifier
 * and evicts conversations idle beyond a configurable TTL.
 *
 * <p>Deliberately in-memory (no persistence): chat history is ephemeral, which
 * avoids GDPR implications and keeps the storage footprint small. State is lost
 * on application restart, which is acceptable for visitor chat sessions.</p>
 *
 * <p>Thread-safe: uses {@link ConcurrentHashMap} for conversation lookup and
 * synchronizes access to each conversation's message deque.</p>
 */
@Slf4j
@Component
public class ConversationHistoryStore {

    private final Map<String, ConversationHistory> conversations = new ConcurrentHashMap<>();

    @Value("${ai.chat.memory.window-size:20}")
    private int windowSize;

    @Value("${ai.chat.memory.idle-ttl-minutes:120}")
    private long idleTtlMinutes;

    @Value("${ai.chat.memory.max-conversations:1000}")
    private int maxConversations;

    /**
     * Append a message to the given conversation. Creates the conversation
     * if it does not yet exist. When the window size is exceeded the oldest
     * message is discarded. New conversations are rejected once the total
     * count reaches the configured maximum, as a defense-in-depth measure
     * against memory exhaustion from unauthenticated endpoints.
     *
     * @param conversationId the conversation identifier
     * @param message        the message to append
     * @return {@code true} if the message was appended, {@code false} if rejected
     */
    public boolean append(String conversationId, Message message) {
        if (!conversations.containsKey(conversationId)
                && conversations.size() >= maxConversations) {
            log.warn("[CHAT_MEMORY] Conversation limit reached, rejecting - conversationId={}, limit={}",
                    conversationId, maxConversations);
            return false;
        }
        conversations
                .computeIfAbsent(conversationId, id -> new ConversationHistory(windowSize))
                .append(message);
        return true;
    }

    /**
     * Return a snapshot of the message history for the given conversation.
     * Updates the conversation's last-access timestamp.
     *
     * @param conversationId the conversation identifier
     * @return an immutable list of messages, empty if the conversation is unknown
     */
    public List<Message> history(String conversationId) {
        ConversationHistory history = conversations.get(conversationId);
        if (history == null) {
            return List.of();
        }
        return history.snapshot();
    }

    /**
     * Clear the conversation history for the given identifier.
     *
     * @param conversationId the conversation identifier
     */
    public void clear(String conversationId) {
        conversations.remove(conversationId);
    }

    /**
     * Current number of tracked conversations, used for observability.
     *
     * @return the number of active conversations
     */
    public int activeConversationCount() {
        return conversations.size();
    }

    /**
     * Scheduled eviction of idle conversations. Runs every 30 minutes and
     * removes conversations whose last access is older than the configured TTL.
     */
    @Scheduled(fixedDelayString = "${ai.chat.memory.cleanup-interval-ms:1800000}",
            initialDelayString = "${ai.chat.memory.cleanup-interval-ms:1800000}")
    public void evictStaleConversations() {
        Duration ttl = Duration.ofMinutes(idleTtlMinutes);
        Instant cutoff = Instant.now().minus(ttl);
        int evicted = 0;
        Iterator<Map.Entry<String, ConversationHistory>> it = conversations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ConversationHistory> entry = it.next();
            if (entry.getValue().lastAccess().isBefore(cutoff)) {
                it.remove();
                evicted++;
            }
        }
        if (evicted > 0) {
            log.info("[CHAT_MEMORY] Evicted stale conversations - count={}, remaining={}",
                    evicted, conversations.size());
        }
    }

    /**
     * Per-conversation message deque with a bounded sliding window.
     * Instances are thread-safe via intrinsic locking on the deque.
     */
    private static final class ConversationHistory {

        private final Deque<Message> messages = new ArrayDeque<>();
        private final int maxSize;
        private volatile Instant lastAccess = Instant.now();

        ConversationHistory(int maxSize) {
            this.maxSize = maxSize;
        }

        synchronized void append(Message message) {
            messages.addLast(message);
            while (messages.size() > maxSize) {
                messages.removeFirst();
            }
            lastAccess = Instant.now();
        }

        synchronized List<Message> snapshot() {
            lastAccess = Instant.now();
            return List.copyOf(messages);
        }

        Instant lastAccess() {
            return lastAccess;
        }
    }
}
