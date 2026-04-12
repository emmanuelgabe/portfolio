package com.emmanuelgabe.portfolio.chat;

import com.emmanuelgabe.portfolio.chat.tools.ContactPrepareTool.ContactFormData;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks tool calls during a single chat request so the streaming
 * endpoint can report which tools were invoked and extract structured
 * data such as contact form pre-fill values.
 *
 * <p>Created per request and passed to tool instances. Thread-safe:
 * tools may execute on different threads than the stream consumer
 * that reads the recorded data.</p>
 */
public class ToolCallTracker {

    private static final ToolCallTracker NOOP_INSTANCE = new NoOpToolCallTracker();

    private final List<String> toolCalls = new CopyOnWriteArrayList<>();
    private final AtomicReference<ContactFormData> contactFormData = new AtomicReference<>();

    /**
     * Record that a tool was invoked during this request.
     *
     * @param toolName the tool name for frontend display
     */
    public void record(String toolName) {
        toolCalls.add(toolName);
    }

    /**
     * Record contact form data returned by the prepareContact tool.
     *
     * @param data pre-filled contact form subject and message
     */
    public void recordContactForm(ContactFormData data) {
        contactFormData.set(data);
    }

    /**
     * @return ordered list of tool names invoked during this request
     */
    public List<String> getToolCalls() {
        return List.copyOf(toolCalls);
    }

    /**
     * @return contact form data if prepareContact was called, empty otherwise
     */
    public Optional<ContactFormData> getContactFormData() {
        return Optional.ofNullable(contactFormData.get());
    }

    /**
     * Returns a no-op tracker that silently discards all recordings.
     * Used when tool call tracking is not needed (sync endpoint, tests).
     *
     * @return a singleton no-op tracker
     */
    public static ToolCallTracker noop() {
        return NOOP_INSTANCE;
    }

    private static final class NoOpToolCallTracker extends ToolCallTracker {

        @Override
        public void record(String toolName) {
            // Intentionally empty
        }

        @Override
        public void recordContactForm(ContactFormData data) {
            // Intentionally empty
        }
    }
}
