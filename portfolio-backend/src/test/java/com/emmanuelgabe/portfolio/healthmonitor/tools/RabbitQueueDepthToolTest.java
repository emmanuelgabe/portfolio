package com.emmanuelgabe.portfolio.healthmonitor.tools;

import com.emmanuelgabe.portfolio.healthmonitor.HealthToolCallTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitQueueDepthToolTest {

    @Mock
    private RabbitAdmin rabbitAdmin;

    private HealthToolCallTracker tracker;
    private RabbitQueueDepthTool tool;

    @BeforeEach
    void setUp() {
        tracker = new HealthToolCallTracker();
        tool = new RabbitQueueDepthTool(rabbitAdmin, tracker);
    }

    // ========== Whitelist validation ==========

    @Test
    void should_returnError_when_getRabbitMqQueueDepthCalledWithDisallowedName() {
        // Act
        Map<String, Object> result = tool.getRabbitMqQueueDepth("evil.queue");

        // Assert
        assertThat(result).containsEntry("queue", "evil.queue");
        assertThat(result).containsEntry("error", "queueName not allowed");
        assertThat(result).containsKey("allowed");
        assertThat(tracker.getCalls()).containsExactly("getRabbitMqQueueDepth");
        verifyNoInteractions(rabbitAdmin);
    }

    // ========== Happy path ==========

    @Test
    void should_returnQueueStats_when_getRabbitMqQueueDepthCalledWithWhitelistedQueue() {
        // Arrange
        Properties info = new Properties();
        info.put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 42);
        info.put(RabbitAdmin.QUEUE_CONSUMER_COUNT, 2);
        when(rabbitAdmin.getQueueProperties("portfolio.email.queue")).thenReturn(info);

        // Act
        Map<String, Object> result = tool.getRabbitMqQueueDepth("portfolio.email.queue");

        // Assert
        assertThat(result).containsEntry("queue", "portfolio.email.queue");
        assertThat(result).containsEntry("state", "available");
        assertThat(result).containsEntry("messageCount", 42);
        assertThat(result).containsEntry("consumerCount", 2);
    }

    @Test
    void should_returnNotFound_when_getQueuePropertiesReturnsNull() {
        // Arrange
        when(rabbitAdmin.getQueueProperties("portfolio.image.queue")).thenReturn(null);

        // Act
        Map<String, Object> result = tool.getRabbitMqQueueDepth("portfolio.image.queue");

        // Assert
        assertThat(result).containsEntry("state", "not_found");
        assertThat(result).doesNotContainKey("messageCount");
    }

    // ========== Error handling ==========

    @Test
    void should_returnErrorState_when_getQueuePropertiesThrows() {
        // Arrange
        when(rabbitAdmin.getQueueProperties("portfolio.autoreply.queue"))
                .thenThrow(new RuntimeException("broker unreachable"));

        // Act
        Map<String, Object> result = tool.getRabbitMqQueueDepth("portfolio.autoreply.queue");

        // Assert
        assertThat(result).containsEntry("state", "error");
        assertThat(result).containsEntry("error", "broker unreachable");
    }
}
