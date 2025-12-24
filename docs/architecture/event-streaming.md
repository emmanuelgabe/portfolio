# Event Streaming Architecture

Apache Kafka-based event streaming for real-time event processing and analytics.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Components](#components)
4. [Event Types](#event-types)
5. [Configuration](#configuration)
6. [Related Documentation](#related-documentation)

---

## Overview

The event streaming system provides real-time event processing for:

- **Admin events** - CRUD operations and authentication tracking
- **Analytics events** - Page views, entity views, user interactions
- **Activity events** - Session tracking and navigation patterns

Key characteristics:
- Kafka topics with 3 partitions each
- Key-based partitioning for ordered processing per entity/session
- Asynchronous non-blocking publishing
- Prometheus metrics integration
- Conditional activation via `kafka.enabled` property

---

## Architecture

```
                          ┌──────────────────────────────────────────────────┐
                          │              Apache Kafka Cluster                │
┌─────────────┐           │                                                  │
│   Service   │           │  ┌────────────────────────────────────────────┐  │
│  (Admin,    │──publish──│──│  portfolio.admin.events [3 partitions]    │──│──▶ AdminEventConsumer
│  Analytics, │           │  └────────────────────────────────────────────┘  │
│  Activity)  │           │                                                  │
└─────────────┘           │  ┌────────────────────────────────────────────┐  │
       │                  │──│  portfolio.analytics.events [3 partitions] │──│──▶ AnalyticsEventConsumer
       │                  │  └────────────────────────────────────────────┘  │
       │                  │                                                  │
       └──────────────────│  ┌────────────────────────────────────────────┐  │
                          │──│  portfolio.activity.events [3 partitions]  │──│──▶ ActivityEventConsumer
                          │  └────────────────────────────────────────────┘  │
                          └──────────────────────────────────────────────────┘
```

### Topics

| Topic | Partitions | Purpose | Key |
|-------|------------|---------|-----|
| `portfolio.admin.events` | 3 | Admin CRUD and auth events | entityType |
| `portfolio.analytics.events` | 3 | Page views, interactions | sessionId |
| `portfolio.activity.events` | 3 | Session and navigation | sessionId |

---

## Components

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `KafkaConfig` | `config/` | Topic definitions and configuration |
| `KafkaEventProducer` | `kafka/producer/` | Event publishing implementation |
| `NoOpKafkaEventProducer` | `kafka/producer/` | No-op producer when Kafka disabled |
| `AdminEventConsumer` | `kafka/consumer/` | Admin event processor |
| `AnalyticsEventConsumer` | `kafka/consumer/` | Analytics event processor |
| `ActivityEventConsumer` | `kafka/consumer/` | Activity event processor |

### Event Classes

| Class | Location | Purpose |
|-------|----------|---------|
| `BaseEvent` | `kafka/event/` | Base class with common fields |
| `AdminActionEvent` | `kafka/event/` | Admin CRUD and auth events |
| `AnalyticsEvent` | `kafka/event/` | Page views and interactions |
| `ActivityEvent` | `kafka/event/` | Session and navigation events |

---

## Event Types

### Admin Action Events

Published to `portfolio.admin.events` topic.

**Actions tracked:**
- CREATE, UPDATE, DELETE
- PUBLISH, UNPUBLISH
- FEATURE, UNFEATURE
- LOGIN, LOGOUT, LOGIN_FAILED
- PASSWORD_CHANGE

**Event structure:**
```json
{
  "eventId": "uuid",
  "eventType": "ADMIN_ACTION",
  "timestamp": "2024-01-15T10:30:00Z",
  "source": "portfolio-backend",
  "action": "CREATE",
  "entityType": "PROJECT",
  "entityId": 123,
  "entityName": "My Project",
  "username": "admin",
  "ipAddress": "192.168.1.1",
  "success": true,
  "payload": {}
}
```

### Analytics Events

Published to `portfolio.analytics.events` topic.

**Event types:**
| Type | Description |
|------|-------------|
| `PAGE_VIEW` | General page view |
| `PROJECT_VIEW` | Project detail view |
| `ARTICLE_VIEW` | Article/blog post view |
| `CONTACT_SUBMIT` | Contact form submission |
| `DOWNLOAD_CV` | CV download |
| `EXTERNAL_LINK_CLICK` | External link click |

**Event structure:**
```json
{
  "eventId": "uuid",
  "eventType": "ANALYTICS",
  "timestamp": "2024-01-15T10:30:00Z",
  "source": "portfolio-backend",
  "analyticsType": "PROJECT_VIEW",
  "path": "/projects/123",
  "entityId": 123,
  "sessionId": "session-uuid",
  "ipAddress": "192.168.1.1",
  "userAgent": "Mozilla/5.0...",
  "referrer": "https://google.com"
}
```

### Activity Events

Published to `portfolio.activity.events` topic.

**Event types:**
| Type | Description |
|------|-------------|
| `SESSION_START` | New session initiated |
| `SESSION_END` | Session terminated |
| `NAVIGATION` | Page navigation |
| `SEARCH` | Search query |

---

## Configuration

### Application Properties

```yaml
# Kafka connection
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
    consumer:
      group-id: portfolio-consumer-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.emmanuelgabe.portfolio.kafka.event

# Feature toggle
kafka:
  enabled: ${KAFKA_ENABLED:false}
```

### Docker Compose

```yaml
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: portfolio-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: portfolio-kafka
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    ports:
      - "9092:9092"
    volumes:
      - kafka_data:/var/lib/kafka/data
```

---

## Feature Toggle

The event streaming system can be disabled via configuration:

```yaml
kafka:
  enabled: false
```

When disabled:
- `NoOpKafkaEventProducer` is activated
- All publish operations become no-ops
- Application continues without event streaming

---

## Publishing Pattern

Events are published asynchronously with callback handling:

```java
CompletableFuture<SendResult<String, Object>> future =
    kafkaTemplate.send(topic, key, event);

future.whenComplete((result, ex) -> {
    if (ex != null) {
        log.error("[KAFKA_PRODUCER] Failed - topic={}", topic);
        metrics.recordKafkaPublishFailure(topic, eventType);
    } else {
        log.debug("[KAFKA_PRODUCER] Success - partition={}, offset={}",
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
        metrics.recordKafkaEventPublished(topic, eventType);
    }
});
```

---

## Partitioning Strategy

Events are partitioned by key for ordered processing:

| Topic | Key | Ordering Guarantee |
|-------|-----|-------------------|
| Admin Events | entityType | Events for same entity type processed in order |
| Analytics Events | sessionId | Events from same session processed in order |
| Activity Events | sessionId | Session events processed in order |

---

## Related Documentation

- [Event Streaming Feature](../features/event-streaming.md) - Feature-level documentation
- [Observability](../operations/observability.md) - Kafka monitoring
- [Configuration Properties](../reference/configuration-properties.md) - Full configuration reference
