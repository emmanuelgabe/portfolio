package com.emmanuelgabe.portfolio.chat;

import com.emmanuelgabe.portfolio.chat.tools.ContactPrepareTool.ContactFormData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolCallTrackerTest {

    // ========== Regular tracker ==========

    @Test
    void should_recordToolCalls_when_recordCalledMultipleTimes() {
        // Arrange
        ToolCallTracker tracker = new ToolCallTracker();

        // Act
        tracker.record("projectSearch");
        tracker.record("articleSearch");

        // Assert
        assertThat(tracker.getToolCalls())
                .containsExactly("projectSearch", "articleSearch");
    }

    @Test
    void should_storeContactFormData_when_recordContactFormCalled() {
        // Arrange
        ToolCallTracker tracker = new ToolCallTracker();
        ContactFormData data = new ContactFormData("subject", "message");

        // Act
        tracker.recordContactForm(data);

        // Assert
        assertThat(tracker.getContactFormData()).contains(data);
    }

    @Test
    void should_returnEmptyOptional_when_contactFormNeverRecorded() {
        // Arrange
        ToolCallTracker tracker = new ToolCallTracker();

        // Act & Assert
        assertThat(tracker.getContactFormData()).isEmpty();
    }

    // ========== No-op tracker ==========

    @Test
    void should_silentlyDiscardRecordings_when_noopTrackerUsed() {
        // Arrange
        ToolCallTracker noop = ToolCallTracker.noop();

        // Act
        noop.record("projectSearch");
        noop.recordContactForm(new ContactFormData("subject", "message"));

        // Assert
        assertThat(noop.getToolCalls()).isEmpty();
        assertThat(noop.getContactFormData()).isEmpty();
    }

    @Test
    void should_returnSameInstance_when_noopCalledMultipleTimes() {
        // Arrange & Act
        ToolCallTracker first = ToolCallTracker.noop();
        ToolCallTracker second = ToolCallTracker.noop();

        // Assert
        assertThat(first).isSameAs(second);
    }
}
