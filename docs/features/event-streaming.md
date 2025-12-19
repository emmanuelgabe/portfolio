# Event Streaming

Real-time event streaming using Apache Kafka for admin actions, analytics, and activity tracking.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Event Categories](#event-categories)
4. [Configuration](#configuration)
5. [Related Documentation](#related-documentation)

---

## Overview

The event streaming feature provides real-time event processing for:

- **Admin events** - CRUD operations and authentication tracking for event sourcing
- **Analytics events** - Page views, entity views, user interactions
- **Activity events** - Session tracking and navigation patterns

Key capabilities:
- Real-time event processing with Kafka
- Key-based partitioning for ordered processing
- Asynchronous non-blocking publishing
- Prometheus metrics for monitoring
- Optional activation via feature toggle

---

## Architecture

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `KafkaConfig` | `config/` | Topic definitions |
| `KafkaEventProducer` | `kafka/producer/` | Event publishing |
| `NoOpKafkaEventProducer` | `kafka/producer/` | No-op fallback |
| `AdminEventConsumer` | `kafka/consumer/` | Admin event processing |
| `AnalyticsEventConsumer` | `kafka/consumer/` | Analytics processing |
| `ActivityEventConsumer` | `kafka/consumer/` | Activity processing |

### Event Classes

| Class | Purpose |
|-------|---------|
| `BaseEvent` | Common event fields |
| `AdminActionEvent` | Admin CRUD and auth events |
| `AnalyticsEvent` | Page views and interactions |
| `ActivityEvent` | Session and navigation |

---

## Event Categories

### Admin Action Events

Captures all administrative operations for audit trail and event sourcing.

**Topic:** `portfolio.admin.events`

**Actions:**

| Action | Description |
|--------|-------------|
| `CREATE` | Entity created |
| `UPDATE` | Entity updated |
| `DELETE` | Entity deleted |
| `PUBLISH` | Article published |
| `UNPUBLISH` | Article unpublished |
| `FEATURE` | Project featured |
| `UNFEATURE` | Project unfeatured |
| `LOGIN` | Admin login |
| `LOGOUT` | Admin logout |
| `LOGIN_FAILED` | Failed login attempt |
| `PASSWORD_CHANGE` | Password changed |

**Event fields:**
- `action` - Action type
- `entityType` - PROJECT, ARTICLE, SKILL, etc.
- `entityId` - Entity identifier
- `entityName` - Human-readable name
- `username` - Admin username
- `ipAddress` - Client IP
- `success` - Operation result
- `payload` - Additional data

### Analytics Events

Captures user interactions for analytics processing.

**Topic:** `portfolio.analytics.events`

**Event types:**

| Type | Description | Tracked Data |
|------|-------------|--------------|
| `PAGE_VIEW` | Page visit | path, referrer, userAgent |
| `PROJECT_VIEW` | Project detail view | projectId, sessionId |
| `ARTICLE_VIEW` | Article view | articleId, slug, sessionId |
| `CONTACT_SUBMIT` | Contact form submit | ipAddress, sessionId |
| `DOWNLOAD_CV` | CV download | sessionId |
| `EXTERNAL_LINK_CLICK` | External link click | url, sessionId |

**Event fields:**
- `analyticsType` - Event type
- `path` - Page path
- `entityId` - Related entity ID
- `entitySlug` - URL slug
- `sessionId` - User session
- `ipAddress` - Client IP
- `userAgent` - Browser info
- `referrer` - Referrer URL

### Activity Events

Captures session and navigation patterns.

**Topic:** `portfolio.activity.events`

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

# Feature toggle
kafka:
  enabled: ${KAFKA_ENABLED:false}
```

### Topics

| Topic | Partitions | Replicas | Key |
|-------|------------|----------|-----|
| `portfolio.admin.events` | 3 | 1 | entityType |
| `portfolio.analytics.events` | 3 | 1 | sessionId |
| `portfolio.activity.events` | 3 | 1 | sessionId |

---

## Feature Toggle

Event streaming can be disabled:

```yaml
kafka:
  enabled: false
```

When disabled:
- `NoOpKafkaEventProducer` is activated
- All publish operations become no-ops
- Application continues without event streaming

---

## Related Documentation

- [Event Streaming Architecture](../architecture/event-streaming.md) - Technical architecture
- [Audit System](./audit-system.md) - Audit logging feature
- [Observability](../operations/observability.md) - Kafka monitoring
