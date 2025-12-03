package com.emmanuelgabe.portfolio.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Event representing analytics data for real-time processing.
 * Captures page views, interactions, and user behavior.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AnalyticsEvent extends BaseEvent {

    public static final String EVENT_TYPE = "ANALYTICS";

    /**
     * Type of analytics event.
     */
    public enum AnalyticsType {
        PAGE_VIEW,
        PROJECT_VIEW,
        ARTICLE_VIEW,
        CONTACT_SUBMIT,
        DOWNLOAD_CV,
        EXTERNAL_LINK_CLICK
    }

    private AnalyticsType analyticsType;
    private String path;
    private String referrer;
    private String userAgent;
    private String ipAddress;
    private String sessionId;
    private Long entityId;
    private String entitySlug;
    private Map<String, Object> metadata;

    /**
     * Create a page view event.
     */
    public static AnalyticsEvent pageView(String path, String referrer, String userAgent,
                                          String ipAddress, String sessionId) {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .analyticsType(AnalyticsType.PAGE_VIEW)
                .path(path)
                .referrer(referrer)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .sessionId(sessionId)
                .build();
        event.initializeFields();
        return event;
    }

    /**
     * Create a project view event.
     */
    public static AnalyticsEvent projectView(Long projectId, String ipAddress,
                                             String userAgent, String sessionId) {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .analyticsType(AnalyticsType.PROJECT_VIEW)
                .path("/projects/" + projectId)
                .entityId(projectId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .sessionId(sessionId)
                .build();
        event.initializeFields();
        return event;
    }

    /**
     * Create an article view event.
     */
    public static AnalyticsEvent articleView(Long articleId, String slug, String ipAddress,
                                             String userAgent, String sessionId) {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .analyticsType(AnalyticsType.ARTICLE_VIEW)
                .path("/blog/" + slug)
                .entityId(articleId)
                .entitySlug(slug)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .sessionId(sessionId)
                .build();
        event.initializeFields();
        return event;
    }

    /**
     * Create a contact form submission event.
     */
    public static AnalyticsEvent contactSubmit(String ipAddress, String sessionId) {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .analyticsType(AnalyticsType.CONTACT_SUBMIT)
                .path("/contact")
                .ipAddress(ipAddress)
                .sessionId(sessionId)
                .build();
        event.initializeFields();
        return event;
    }

    private void initializeFields() {
        setEventId(UUID.randomUUID().toString());
        setEventType(EVENT_TYPE);
        setTimestamp(Instant.now());
        setSource("portfolio-backend");
    }
}
