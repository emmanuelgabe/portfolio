package com.emmanuelgabe.portfolio.healthmonitor.tools;

import com.emmanuelgabe.portfolio.healthmonitor.HealthToolCallTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadLetterQueueToolTest {

    @Mock
    private RabbitAdmin rabbitAdmin;

    private HealthToolCallTracker tracker;
    private DeadLetterQueueTool tool;

    @BeforeEach
    void setUp() {
        tracker = new HealthToolCallTracker();
        tool = new DeadLetterQueueTool(rabbitAdmin, tracker);
    }

    // ========== Happy path ==========

    @Test
    void should_returnAllDlqStates_when_checkDeadLetterQueueCalledAndQueuesAvailable() {
        // Arrange
        Properties emailDlq = new Properties();
        emailDlq.put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 3);
        emailDlq.put(RabbitAdmin.QUEUE_CONSUMER_COUNT, 1);
        when(rabbitAdmin.getQueueProperties("portfolio.email.dlq")).thenReturn(emailDlq);

        Properties imageDlq = new Properties();
        imageDlq.put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 0);
        imageDlq.put(RabbitAdmin.QUEUE_CONSUMER_COUNT, 1);
        when(rabbitAdmin.getQueueProperties("portfolio.image.dlq")).thenReturn(imageDlq);

        Properties auditDlq = new Properties();
        auditDlq.put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 0);
        auditDlq.put(RabbitAdmin.QUEUE_CONSUMER_COUNT, 1);
        when(rabbitAdmin.getQueueProperties("portfolio.audit.dlq")).thenReturn(auditDlq);

        Properties autoreplyDlq = new Properties();
        autoreplyDlq.put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 0);
        autoreplyDlq.put(RabbitAdmin.QUEUE_CONSUMER_COUNT, 1);
        when(rabbitAdmin.getQueueProperties("portfolio.autoreply.dlq")).thenReturn(autoreplyDlq);

        // Act
        List<Map<String, Object>> result = tool.checkDeadLetterQueue();

        // Assert
        assertThat(result).hasSize(4);
        assertThat(result).extracting(m -> m.get("queue"))
                .containsExactly("portfolio.email.dlq", "portfolio.image.dlq",
                        "portfolio.audit.dlq", "portfolio.autoreply.dlq");
        assertThat(result.get(0)).containsEntry("state", "available");
        assertThat(result.get(0)).containsEntry("messageCount", 3);
        assertThat(result.get(0)).containsEntry("consumerCount", 1);
        assertThat(tracker.getCalls()).containsExactly("checkDeadLetterQueue");
    }

    // ========== Missing queue ==========

    @Test
    void should_markQueueNotFound_when_getQueuePropertiesReturnsNull() {
        // Arrange
        when(rabbitAdmin.getQueueProperties("portfolio.email.dlq")).thenReturn(null);
        when(rabbitAdmin.getQueueProperties("portfolio.image.dlq")).thenReturn(null);
        when(rabbitAdmin.getQueueProperties("portfolio.audit.dlq")).thenReturn(null);
        when(rabbitAdmin.getQueueProperties("portfolio.autoreply.dlq")).thenReturn(null);

        // Act
        List<Map<String, Object>> result = tool.checkDeadLetterQueue();

        // Assert
        assertThat(result).hasSize(4);
        assertThat(result).allSatisfy(entry -> {
            assertThat(entry).containsEntry("state", "not_found");
            assertThat(entry).containsEntry("messageCount", 0);
        });
    }

    // ========== Error handling ==========

    @Test
    void should_returnErrorState_when_getQueuePropertiesThrows() {
        // Arrange
        when(rabbitAdmin.getQueueProperties("portfolio.email.dlq"))
                .thenThrow(new RuntimeException("broker unreachable"));
        Properties ok = new Properties();
        ok.put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 0);
        ok.put(RabbitAdmin.QUEUE_CONSUMER_COUNT, 1);
        when(rabbitAdmin.getQueueProperties("portfolio.image.dlq")).thenReturn(ok);
        when(rabbitAdmin.getQueueProperties("portfolio.audit.dlq")).thenReturn(ok);
        when(rabbitAdmin.getQueueProperties("portfolio.autoreply.dlq")).thenReturn(ok);

        // Act
        List<Map<String, Object>> result = tool.checkDeadLetterQueue();

        // Assert
        assertThat(result).hasSize(4);
        assertThat(result.get(0)).containsEntry("state", "error");
        assertThat(result.get(0)).containsEntry("error", "broker unreachable");
        assertThat(result.get(1)).containsEntry("state", "available");
    }
}
