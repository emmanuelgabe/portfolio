package com.emmanuelgabe.portfolio.healthmonitor.tools;

import com.emmanuelgabe.portfolio.healthmonitor.HealthToolCallTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Green tool: reports the depth of every dead letter queue in the portfolio
 * RabbitMQ cluster. Non-zero depth means messages have failed all retries
 * and require either {@code RETRY_DLQ_MESSAGES} or manual inspection.
 */
@Slf4j
public class DeadLetterQueueTool {

    private static final List<String> DLQ_NAMES = List.of(
            "portfolio.email.dlq",
            "portfolio.image.dlq",
            "portfolio.audit.dlq",
            "portfolio.autoreply.dlq"
    );

    private final RabbitAdmin rabbitAdmin;
    private final HealthToolCallTracker tracker;

    public DeadLetterQueueTool(RabbitAdmin rabbitAdmin, HealthToolCallTracker tracker) {
        this.rabbitAdmin = rabbitAdmin;
        this.tracker = tracker;
    }

    @Tool(description = "Return the current message count of every portfolio dead letter queue "
            + "(email, image, audit, autoreply). Any non-zero value means messages have "
            + "failed their retries and are stuck. Use this to decide whether to propose "
            + "RETRY_DLQ_MESSAGES.")
    public List<Map<String, Object>> checkDeadLetterQueue() {
        tracker.record("checkDeadLetterQueue");
        log.debug("[HEALTH_TOOL] checkDeadLetterQueue");

        return DLQ_NAMES.stream().map(this::describe).toList();
    }

    private Map<String, Object> describe(String queueName) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("queue", queueName);
        try {
            Properties info = rabbitAdmin.getQueueProperties(queueName);
            if (info == null) {
                result.put("state", "not_found");
                result.put("messageCount", 0);
                return result;
            }
            result.put("state", "available");
            result.put("messageCount",
                    info.getOrDefault(RabbitAdmin.QUEUE_MESSAGE_COUNT, 0));
            result.put("consumerCount",
                    info.getOrDefault(RabbitAdmin.QUEUE_CONSUMER_COUNT, 0));
        } catch (Exception e) {
            log.warn("[HEALTH_TOOL] DLQ fetch failed - queue={}, error={}",
                    queueName, e.getMessage());
            result.put("state", "error");
            result.put("error", e.getMessage());
        }
        return result;
    }

    // Suppress unused import warning for Queue
    @SuppressWarnings("unused")
    private Queue unused;
}
