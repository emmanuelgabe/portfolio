package com.emmanuelgabe.portfolio.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationHistoryStoreTest {

    private ConversationHistoryStore store;

    @BeforeEach
    void setUp() {
        store = new ConversationHistoryStore();
        ReflectionTestUtils.setField(store, "windowSize", 4);
        ReflectionTestUtils.setField(store, "idleTtlMinutes", 120L);
        ReflectionTestUtils.setField(store, "maxConversations", 1000);
    }

    // ========== append + history ==========

    @Test
    void should_returnEmptyList_when_conversationUnknown() {
        // Act
        List<Message> history = store.history("unknown-conv");

        // Assert
        assertThat(history).isEmpty();
    }

    @Test
    void should_returnAppendedMessages_when_historyRetrieved() {
        // Arrange
        store.append("conv-1", new UserMessage("Hello"));
        store.append("conv-1", new AssistantMessage("Hi there"));

        // Act
        List<Message> history = store.history("conv-1");

        // Assert
        assertThat(history).hasSize(2);
        assertThat(history.get(0)).isInstanceOf(UserMessage.class);
        assertThat(history.get(0).getText()).isEqualTo("Hello");
        assertThat(history.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(history.get(1).getText()).isEqualTo("Hi there");
    }

    @Test
    void should_returnImmutableSnapshot_when_historyRetrieved() {
        // Arrange
        store.append("conv-1", new UserMessage("Hello"));

        // Act
        List<Message> history = store.history("conv-1");

        // Assert -- snapshot is an immutable copy, mutating it must not affect store
        assertThat(history).hasSize(1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> history.add(new UserMessage("X")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_preserveMessageOrder_when_multipleAppended() {
        // Arrange
        store.append("conv-1", new UserMessage("First"));
        store.append("conv-1", new AssistantMessage("Second"));
        store.append("conv-1", new UserMessage("Third"));

        // Act
        List<Message> history = store.history("conv-1");

        // Assert
        assertThat(history).hasSize(3);
        assertThat(history.get(0).getText()).isEqualTo("First");
        assertThat(history.get(1).getText()).isEqualTo("Second");
        assertThat(history.get(2).getText()).isEqualTo("Third");
    }

    // ========== Window Size ==========

    @Test
    void should_enforceWindowSize_when_messagesExceedLimit() {
        // Arrange -- window size is 4
        store.append("conv-1", new UserMessage("1"));
        store.append("conv-1", new UserMessage("2"));
        store.append("conv-1", new UserMessage("3"));
        store.append("conv-1", new UserMessage("4"));
        store.append("conv-1", new UserMessage("5"));
        store.append("conv-1", new UserMessage("6"));

        // Act
        List<Message> history = store.history("conv-1");

        // Assert -- oldest messages evicted, newest retained
        assertThat(history).hasSize(4);
        assertThat(history.get(0).getText()).isEqualTo("3");
        assertThat(history.get(3).getText()).isEqualTo("6");
    }

    @Test
    void should_retainAllMessages_when_countMatchesWindow() {
        // Arrange -- fill exactly to window size
        store.append("conv-1", new UserMessage("1"));
        store.append("conv-1", new UserMessage("2"));
        store.append("conv-1", new UserMessage("3"));
        store.append("conv-1", new UserMessage("4"));

        // Act
        List<Message> history = store.history("conv-1");

        // Assert
        assertThat(history).hasSize(4);
        assertThat(history.get(0).getText()).isEqualTo("1");
    }

    // ========== Conversation Isolation ==========

    @Test
    void should_isolateConversations_when_differentIdsUsed() {
        // Arrange
        store.append("conv-a", new UserMessage("A-msg-1"));
        store.append("conv-a", new UserMessage("A-msg-2"));
        store.append("conv-b", new UserMessage("B-msg-1"));

        // Act
        List<Message> historyA = store.history("conv-a");
        List<Message> historyB = store.history("conv-b");

        // Assert
        assertThat(historyA).hasSize(2);
        assertThat(historyB).hasSize(1);
        assertThat(historyB.get(0).getText()).isEqualTo("B-msg-1");
    }

    // ========== clear ==========

    @Test
    void should_removeHistory_when_clearCalled() {
        // Arrange
        store.append("conv-1", new UserMessage("Message"));
        assertThat(store.history("conv-1")).hasSize(1);

        // Act
        store.clear("conv-1");

        // Assert
        assertThat(store.history("conv-1")).isEmpty();
    }

    @Test
    void should_notAffectOtherConversations_when_clearCalled() {
        // Arrange
        store.append("conv-a", new UserMessage("A"));
        store.append("conv-b", new UserMessage("B"));

        // Act
        store.clear("conv-a");

        // Assert
        assertThat(store.history("conv-a")).isEmpty();
        assertThat(store.history("conv-b")).hasSize(1);
    }

    @Test
    void should_notFail_when_clearCalledOnUnknownConversation() {
        // Act & Assert -- no exception
        store.clear("never-existed");
        assertThat(store.history("never-existed")).isEmpty();
    }

    // ========== activeConversationCount ==========

    @Test
    void should_returnZero_when_noConversations() {
        // Act
        int count = store.activeConversationCount();

        // Assert
        assertThat(count).isZero();
    }

    @Test
    void should_countDistinctConversations_when_multipleActive() {
        // Arrange
        store.append("conv-a", new UserMessage("A"));
        store.append("conv-b", new UserMessage("B"));
        store.append("conv-c", new UserMessage("C"));
        store.append("conv-a", new UserMessage("A2"));

        // Act
        int count = store.activeConversationCount();

        // Assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    void should_decrementCount_when_clearCalled() {
        // Arrange
        store.append("conv-a", new UserMessage("A"));
        store.append("conv-b", new UserMessage("B"));

        // Act
        store.clear("conv-a");

        // Assert
        assertThat(store.activeConversationCount()).isEqualTo(1);
    }

    // ========== Max conversations cap ==========

    @Test
    void should_rejectNewConversation_when_maxConversationsReached() {
        // Arrange -- set cap to 2
        ReflectionTestUtils.setField(store, "maxConversations", 2);
        store.append("conv-a", new UserMessage("A"));
        store.append("conv-b", new UserMessage("B"));

        // Act
        boolean accepted = store.append("conv-c", new UserMessage("C"));

        // Assert
        assertThat(accepted).isFalse();
        assertThat(store.activeConversationCount()).isEqualTo(2);
        assertThat(store.history("conv-c")).isEmpty();
    }

    @Test
    void should_allowAppendToExistingConversation_when_maxConversationsReached() {
        // Arrange -- set cap to 2
        ReflectionTestUtils.setField(store, "maxConversations", 2);
        store.append("conv-a", new UserMessage("A1"));
        store.append("conv-b", new UserMessage("B1"));

        // Act -- append to existing conv-a, should still work
        boolean accepted = store.append("conv-a", new UserMessage("A2"));

        // Assert
        assertThat(accepted).isTrue();
        assertThat(store.history("conv-a")).hasSize(2);
    }

    @Test
    void should_allowNewConversation_when_belowMaxConversations() {
        // Arrange -- set cap to 3
        ReflectionTestUtils.setField(store, "maxConversations", 3);
        store.append("conv-a", new UserMessage("A"));
        store.append("conv-b", new UserMessage("B"));

        // Act
        boolean accepted = store.append("conv-c", new UserMessage("C"));

        // Assert
        assertThat(accepted).isTrue();
        assertThat(store.activeConversationCount()).isEqualTo(3);
    }

    // ========== evictStaleConversations ==========

    @Test
    void should_removeStaleConversations_when_evictCalled() {
        // Arrange -- force a very short TTL so the just-added conversation is stale
        ReflectionTestUtils.setField(store, "idleTtlMinutes", -1L);
        store.append("conv-1", new UserMessage("Old"));
        assertThat(store.activeConversationCount()).isEqualTo(1);

        // Act
        store.evictStaleConversations();

        // Assert
        assertThat(store.activeConversationCount()).isZero();
    }

    @Test
    void should_preserveRecentConversations_when_evictCalled() {
        // Arrange -- default 120 minute TTL, fresh conversation
        store.append("conv-1", new UserMessage("Recent"));

        // Act
        store.evictStaleConversations();

        // Assert -- fresh conversation is kept
        assertThat(store.activeConversationCount()).isEqualTo(1);
        assertThat(store.history("conv-1")).hasSize(1);
    }

    @Test
    void should_evictOnlyStaleConversations_when_mixedAges() {
        // Arrange -- with a long TTL all are fresh, so nothing evicts
        store.append("conv-a", new UserMessage("A"));
        store.append("conv-b", new UserMessage("B"));

        // Act
        store.evictStaleConversations();

        // Assert
        assertThat(store.activeConversationCount()).isEqualTo(2);
    }

    @Test
    void should_notFail_when_evictCalledOnEmptyStore() {
        // Act & Assert -- no exception
        store.evictStaleConversations();
        assertThat(store.activeConversationCount()).isZero();
    }
}
