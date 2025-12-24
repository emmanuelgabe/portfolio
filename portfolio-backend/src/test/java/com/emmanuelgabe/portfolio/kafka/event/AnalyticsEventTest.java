package com.emmanuelgabe.portfolio.kafka.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AnalyticsEvent.
 */
class AnalyticsEventTest {

    // ========== Factory Method Tests ==========

    @Test
    void should_createPageViewEvent_when_pageViewFactoryMethodCalled() {
        // Act
        AnalyticsEvent event = AnalyticsEvent.pageView(
                "/projects", "https://google.com",
                "Mozilla/5.0", "192.168.1.1", "session123");

        // Assert
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo(AnalyticsEvent.EVENT_TYPE);
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getAnalyticsType()).isEqualTo(AnalyticsEvent.AnalyticsType.PAGE_VIEW);
        assertThat(event.getPath()).isEqualTo("/projects");
        assertThat(event.getReferrer()).isEqualTo("https://google.com");
        assertThat(event.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(event.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(event.getSessionId()).isEqualTo("session123");
    }

    @Test
    void should_createProjectViewEvent_when_projectViewFactoryMethodCalled() {
        // Act
        AnalyticsEvent event = AnalyticsEvent.projectView(
                5L, "10.0.0.1", "Chrome/120", "session456");

        // Assert
        assertThat(event.getAnalyticsType()).isEqualTo(AnalyticsEvent.AnalyticsType.PROJECT_VIEW);
        assertThat(event.getPath()).isEqualTo("/projects/5");
        assertThat(event.getEntityId()).isEqualTo(5L);
        assertThat(event.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(event.getSessionId()).isEqualTo("session456");
    }

    @Test
    void should_createArticleViewEvent_when_articleViewFactoryMethodCalled() {
        // Act
        AnalyticsEvent event = AnalyticsEvent.articleView(
                10L, "my-first-article", "192.168.0.1",
                "Firefox/115", "session789");

        // Assert
        assertThat(event.getAnalyticsType()).isEqualTo(AnalyticsEvent.AnalyticsType.ARTICLE_VIEW);
        assertThat(event.getPath()).isEqualTo("/blog/my-first-article");
        assertThat(event.getEntityId()).isEqualTo(10L);
        assertThat(event.getEntitySlug()).isEqualTo("my-first-article");
        assertThat(event.getSessionId()).isEqualTo("session789");
    }

    @Test
    void should_createContactSubmitEvent_when_contactSubmitFactoryMethodCalled() {
        // Act
        AnalyticsEvent event = AnalyticsEvent.contactSubmit("172.16.0.1", "session-abc");

        // Assert
        assertThat(event.getAnalyticsType()).isEqualTo(AnalyticsEvent.AnalyticsType.CONTACT_SUBMIT);
        assertThat(event.getPath()).isEqualTo("/contact");
        assertThat(event.getIpAddress()).isEqualTo("172.16.0.1");
        assertThat(event.getSessionId()).isEqualTo("session-abc");
    }

    @Test
    void should_generateUniqueEventId_when_multipleEventsCreated() {
        // Act
        AnalyticsEvent event1 = AnalyticsEvent.pageView("/", null, null, null, null);
        AnalyticsEvent event2 = AnalyticsEvent.pageView("/", null, null, null, null);

        // Assert
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }

    @Test
    void should_haveCorrectEventType_when_eventCreated() {
        // Assert
        assertThat(AnalyticsEvent.EVENT_TYPE).isEqualTo("ANALYTICS");
    }
}
