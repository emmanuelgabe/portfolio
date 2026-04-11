package com.emmanuelgabe.portfolio.healthmonitor.tools;

import com.emmanuelgabe.portfolio.healthmonitor.HealthToolCallTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Green tool: reports message backlog on a specific portfolio work queue
 * (not a DLQ). A large backlog on {@code portfolio.email.queue} combined
 * with an open circuit breaker is a classic failure scenario the agent
 * should correlate and act on.
 */
@Slf4j
public class RabbitQueueDepthTool {

    private static final Set<String> ALLOWED_QUEUES = Set.of(
            "portfolio.email.queue",
            "portfolio.image.queue",
            "portfolio.audit.queue",
            "portfolio.autoreply.queue"
    );

    private final RabbitAdmin rabbitAdmin;
    private final HealthToolCallTracker tracker;

    public RabbitQueueDepthTool(RabbitAdmin rabbitAdmin, HealthToolCallTracker tracker) {
        this.rabbitAdmin = rabbitAdmin;
        this.tracker = tracker;
    }

    @Tool(description = "Return the message backlog and consumer count of a specific portfolio "
            + "work queue. Allowed queueName values: portfolio.email.queue, portfolio.image.queue, "
            + "portfolio.audit.queue, portfolio.autoreply.queue. A large backlog combined with an "
            + "open circuit breaker often indicates a downstream failure.")
    public Map<String, Object> getRabbitMqQueueDepth(
            @ToolParam(description = "Fully-qualified queue name from the whitelist") String queueName) {
        tracker.record("getRabbitMqQueueDepth");
        log.debug("[HEALTH_TOOL] getRabbitMqQueueDepth - queueName={}", queueName);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("queue", queueName);

        if (!ALLOWED_QUEUES.contains(queueName)) {
            result.put("error", "queueName not allowed");
            result.put("allowed", ALLOWED_QUEUES);
            return result;
        }

        try {
            Properties info = rabbitAdmin.getQueueProperties(queueName);
            if (info == null) {
                result.put("state", "not_found");
                return result;
            }
            result.put("state", "available");
            result.put("messageCount", info.getOrDefault(RabbitAdmin.QUEUE_MESSAGE_COUNT, 0));
            result.put("consumerCount", info.getOrDefault(RabbitAdmin.QUEUE_CONSUMER_COUNT, 0));
        } catch (Exception e) {
            log.warn("[HEALTH_TOOL] Queue fetch failed - queue={}, error={}",
                    queueName, e.getMessage());
            result.put("state", "error");
            result.put("error", e.getMessage());
        }
        return result;
    }
}
