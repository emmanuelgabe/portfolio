package com.emmanuelgabe.portfolio.kafka.consumer;

import com.emmanuelgabe.portfolio.config.KafkaConfig;
import com.emmanuelgabe.portfolio.kafka.event.AnalyticsEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for analytics events.
 * Processes page views, project views, article views, etc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class AnalyticsEventConsumer {

    private static final String TOPIC_NAME = "analytics_events";

    private final BusinessMetrics metrics;

    /**
     * Process analytics events from Kafka.
     *
     * @param event the analytics event to process
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_ANALYTICS_EVENTS,
            groupId = "${kafka.consumer.group-id:portfolio-analytics-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAnalyticsEvent(AnalyticsEvent event) {
        log.debug("[KAFKA_CONSUMER] Received analytics event - eventId={}, type={}, path={}",
                event.getEventId(), event.getAnalyticsType(), event.getPath());

        try {
            processEvent(event);
            metrics.recordKafkaEventConsumed(TOPIC_NAME, event.getAnalyticsType().name());
            log.debug("[KAFKA_CONSUMER] Analytics event processed - eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("[KAFKA_CONSUMER] Failed to process analytics event - eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            metrics.recordKafkaConsumeFailure(TOPIC_NAME, event.getAnalyticsType().name());
            throw e;
        }
    }

    private void processEvent(AnalyticsEvent event) {
        switch (event.getAnalyticsType()) {
            case PROJECT_VIEW -> {
                metrics.recordProjectView();
                log.info("[ANALYTICS] Project view recorded - projectId={}, ip={}",
                        event.getEntityId(), maskIp(event.getIpAddress()));
            }
            case ARTICLE_VIEW -> {
                metrics.recordArticleView();
                log.info("[ANALYTICS] Article view recorded - articleId={}, slug={}, ip={}",
                        event.getEntityId(), event.getEntitySlug(), maskIp(event.getIpAddress()));
            }
            case CONTACT_SUBMIT -> {
                metrics.recordContactSubmission();
                log.info("[ANALYTICS] Contact submission recorded - ip={}",
                        maskIp(event.getIpAddress()));
            }
            case PAGE_VIEW -> log.debug("[ANALYTICS] Page view recorded - path={}, ip={}",
                    event.getPath(), maskIp(event.getIpAddress()));
            case DOWNLOAD_CV -> log.info("[ANALYTICS] CV download recorded - ip={}",
                    maskIp(event.getIpAddress()));
            case EXTERNAL_LINK_CLICK -> log.debug("[ANALYTICS] External link click - path={}",
                    event.getPath());
            default -> log.warn("[ANALYTICS] Unknown analytics type - type={}",
                    event.getAnalyticsType());
        }
    }

    /**
     * Mask IP address for privacy in logs.
     */
    private String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "unknown";
        }
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".***";
        }
        return "***";
    }
}
