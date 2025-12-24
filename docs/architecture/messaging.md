# Messaging Architecture

RabbitMQ-based message queue infrastructure for asynchronous task processing.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Components](#components)
4. [Message Flow](#message-flow)
5. [Configuration](#configuration)
6. [Related Documentation](#related-documentation)

---

## Overview

The messaging system provides asynchronous processing for:

- **Email sending** - Contact form submissions via SMTP
- **Image processing** - WebP conversion and thumbnail generation
- **Audit logging** - Non-blocking audit trail persistence

Key characteristics:
- Direct exchange routing for reliable message delivery
- Dead Letter Queues (DLQ) for failed message handling
- JSON message serialization with Jackson
- Publisher confirms for delivery guarantees
- Conditional activation via `rabbitmq.enabled` property

---

## Architecture

```
                              ┌─────────────────────────────────────────┐
                              │            RabbitMQ Broker              │
┌─────────────┐               │                                         │
│   Service   │               │  ┌─────────────┐    ┌──────────────┐   │    ┌─────────────┐
│  (Contact,  │──publish──────│─▶│   Exchange  │───▶│    Queue     │───│───▶│  Consumer   │
│   Image,    │               │  │  (Direct)   │    │              │   │    │             │
│   Audit)    │               │  └─────────────┘    └──────────────┘   │    └─────────────┘
└─────────────┘               │         │                  │           │
                              │         │ (routing key)    │ (DLQ)     │
                              │         ▼                  ▼           │
                              │  ┌─────────────┐    ┌──────────────┐   │    ┌─────────────┐
                              │  │     DLX     │◀───│   DL Queue   │───│───▶│  DL Handler │
                              │  │  Exchange   │    │              │   │    │             │
                              │  └─────────────┘    └──────────────┘   │    └─────────────┘
                              └─────────────────────────────────────────┘
```

### Exchange Types

| Exchange | Type | Purpose |
|----------|------|---------|
| `portfolio.email.exchange` | Direct | Email message routing |
| `portfolio.image.exchange` | Direct | Image processing routing |
| `portfolio.audit.exchange` | Direct | Audit event routing |
| `portfolio.dlx.exchange` | Direct | Dead letter routing |

---

## Components

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `RabbitMQConfig` | `messaging/config/` | Exchange, queue, and binding definitions |
| `RabbitMQProperties` | `messaging/config/` | Configuration properties mapping |
| `RabbitMQEventPublisher` | `messaging/publisher/` | Message publishing implementation |
| `NoOpEventPublisher` | `messaging/publisher/` | No-op publisher when RabbitMQ disabled |
| `EmailConsumer` | `messaging/consumer/` | Email message consumer |
| `ImageProcessingConsumer` | `messaging/consumer/` | Image processing consumer |
| `AuditConsumer` | `messaging/consumer/` | Audit event consumer |
| `DeadLetterConsumer` | `messaging/consumer/` | Failed message handler |

### Event Types

| Event | Class | Fields |
|-------|-------|--------|
| Email | `EmailEvent` | eventId, eventType, recipientEmail, subject, body |
| Contact Email | `ContactEmailEvent` | Extends EmailEvent with senderName, senderEmail, message |
| Image Processing | `ImageProcessingEvent` | eventId, processingType, tempFilePath, entityId, entityType |
| Audit | `AuditEvent` | eventId, action, entityType, entityId, userId, username, oldValue, newValue |

---

## Message Flow

### Email Flow

```
ContactController          EventPublisher           RabbitMQ              EmailConsumer
      │                         │                      │                       │
      │──submitContact()───────▶│                      │                       │
      │                         │──publishEmailEvent()─▶                       │
      │                         │                      │──deliver to queue────▶│
      │◀──202 Accepted─────────│                      │                       │
      │                         │                      │                       │──sendEmail()
      │                         │                      │◀──ack─────────────────│
```

### Image Processing Flow

```
ProjectController         EventPublisher           RabbitMQ         ImageProcessingConsumer
      │                         │                      │                       │
      │──uploadImage()─────────▶│                      │                       │
      │  (save temp file)       │                      │                       │
      │                         │──publishImageEvent()─▶                       │
      │                         │                      │──deliver to queue────▶│
      │◀──201 Created──────────│                      │                       │
      │  (immediate response)   │                      │                       │──processImage()
      │                         │                      │                       │  (convert to WebP)
      │                         │                      │                       │  (generate thumbnail)
      │                         │                      │◀──ack─────────────────│
```

---

## Configuration

### Application Properties

```yaml
# RabbitMQ connection
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    publisher-confirm-type: correlated
    publisher-returns: true

# Feature toggle
rabbitmq:
  enabled: ${RABBITMQ_ENABLED:true}

  # Queue names
  queues:
    email: portfolio.email.queue
    email-dlq: portfolio.email.dlq
    image: portfolio.image.queue
    image-dlq: portfolio.image.dlq
    audit: portfolio.audit.queue
    audit-dlq: portfolio.audit.dlq

  # Exchange names
  exchanges:
    email: portfolio.email.exchange
    image: portfolio.image.exchange
    audit: portfolio.audit.exchange
    dlx: portfolio.dlx.exchange

  # Routing keys
  routing-keys:
    email: email.send
    email-dlq: email.dlq
    image: image.process
    image-dlq: image.dlq
    audit: audit.log
    audit-dlq: audit.dlq
```

### Queue Settings

| Queue | TTL | DLQ Routing |
|-------|-----|-------------|
| Email | 24 hours | `email.dlq` |
| Image | 1 hour | `image.dlq` |
| Audit | 24 hours | `audit.dlq` |

### Docker Compose

```yaml
services:
  rabbitmq:
    image: rabbitmq:3.13-management
    container_name: portfolio-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USERNAME:-guest}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:-guest}
    ports:
      - "5672:5672"   # AMQP
      - "15672:15672" # Management UI
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "check_running"]
      interval: 30s
      timeout: 10s
      retries: 5
```

---

## Feature Toggle

The messaging system can be disabled via configuration:

```yaml
rabbitmq:
  enabled: false
```

When disabled:
- `NoOpEventPublisher` is activated instead of `RabbitMQEventPublisher`
- All publish operations become no-ops
- Application continues to function with synchronous fallbacks

---

## Publisher Confirms

Publisher confirms ensure message delivery:

```java
template.setConfirmCallback((correlationData, ack, cause) -> {
    if (!ack) {
        log.error("[RABBITMQ] Message not confirmed - cause={}", cause);
    }
});

template.setReturnsCallback(returned -> {
    log.error("[RABBITMQ] Message returned - routingKey={}", returned.getRoutingKey());
});
```

---

## Dead Letter Handling

Failed messages are routed to DLQ:

1. Message processing fails (exception thrown)
2. Message is negatively acknowledged
3. Broker routes to DLX with original routing key + `.dlq`
4. `DeadLetterConsumer` logs and optionally retries

---

## Related Documentation

- [Messaging Feature](../features/messaging.md) - Feature-level documentation
- [Observability](../operations/observability.md) - RabbitMQ monitoring
- [Configuration Properties](../reference/configuration-properties.md) - Full configuration reference
