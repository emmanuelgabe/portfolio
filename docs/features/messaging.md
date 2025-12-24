# Messaging

Asynchronous task processing using RabbitMQ for email sending, image processing, and audit logging.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Use Cases](#use-cases)
4. [Configuration](#configuration)
5. [Database Schema](#database-schema)
6. [Related Documentation](#related-documentation)

---

## Overview

The messaging feature provides asynchronous processing for:

- **Contact form emails** - Non-blocking email delivery via SMTP
- **Image optimization** - WebP conversion and thumbnail generation in background
- **Audit logging** - Non-blocking audit trail persistence

Key capabilities:
- Immediate HTTP response while processing happens asynchronously
- Dead Letter Queues (DLQ) for failed message handling
- Message TTL for automatic cleanup
- Image status tracking (PROCESSING, READY, FAILED)
- Prometheus metrics integration

---

## Architecture

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `RabbitMQConfig` | `messaging/config/` | Exchange, queue, binding definitions |
| `RabbitMQProperties` | `messaging/config/` | Configuration properties |
| `RabbitMQEventPublisher` | `messaging/publisher/` | Event publishing |
| `EmailConsumer` | `messaging/consumer/` | Contact email processing |
| `ImageProcessingConsumer` | `messaging/consumer/` | Image optimization |
| `AuditConsumer` | `messaging/consumer/` | Audit log persistence |
| `DeadLetterConsumer` | `messaging/consumer/` | Failed message handling |

### Event Types

| Event | Class | Purpose |
|-------|-------|---------|
| Contact Email | `ContactEmailEvent` | Contact form submission |
| Image Processing | `ImageProcessingEvent` | Image optimization task |
| Audit | `AuditEvent` | Admin action logging |

---

## Use Cases

### Contact Form Email

User submits contact form and receives immediate response while email is sent asynchronously.

**Flow:**
1. User submits contact form
2. `ContactController` validates input
3. `ContactServiceImpl` publishes `ContactEmailEvent` to queue
4. HTTP 202 Accepted returned immediately
5. `EmailConsumer` processes event and sends email via SMTP
6. Failed emails routed to DLQ for manual review

**Event payload:**
```json
{
  "eventId": "uuid",
  "senderName": "John Doe",
  "senderEmail": "john@example.com",
  "recipientEmail": "admin@portfolio.com",
  "subject": "Contact inquiry",
  "body": "<html>...</html>"
}
```

### Image Processing

Image upload returns immediately while optimization happens in background.

**Flow:**
1. User uploads image via admin panel
2. Image saved to temp location with PROCESSING status
3. `ImageProcessingEvent` published to queue
4. HTTP 201 Created returned with image metadata (status: PROCESSING)
5. `ImageProcessingConsumer` processes image:
   - Converts to WebP format
   - Generates thumbnail
   - Saves original for batch reprocessing
   - Updates status to READY
6. Frontend polls or receives status update

**Processing types:**

| Type | Dimensions | Quality | Output |
|------|------------|---------|--------|
| `PROJECT` | 1200×800 | 0.85 | Full + thumbnail |
| `PROJECT_CAROUSEL` | 16:9 ratio | 0.85 | Full + thumbnail |
| `ARTICLE` | 1200×675 | 0.85 | Full + thumbnail |
| `PROFILE` | 400×400 | 0.90 | Optimized only |

**Status tracking:**
```
PROCESSING → READY (success)
PROCESSING → FAILED (error)
```

### Audit Logging

Admin actions logged asynchronously without impacting request latency.

**Flow:**
1. Admin performs action (CREATE, UPDATE, DELETE)
2. `@Auditable` aspect captures action details
3. `AuditEvent` published to queue
4. HTTP response returned immediately
5. `AuditConsumer` persists audit log to database

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

# Feature toggle and queue configuration
rabbitmq:
  enabled: ${RABBITMQ_ENABLED:true}

  queues:
    email: portfolio.email.queue
    email-dlq: portfolio.email.dlq
    image: portfolio.image.queue
    image-dlq: portfolio.image.dlq
    audit: portfolio.audit.queue
    audit-dlq: portfolio.audit.dlq

  exchanges:
    email: portfolio.email.exchange
    image: portfolio.image.exchange
    audit: portfolio.audit.exchange
    dlx: portfolio.dlx.exchange

  routing-keys:
    email: email.send
    image: image.process
    audit: audit.log
```

### Queue Settings

| Queue | Message TTL | Dead Letter Exchange |
|-------|-------------|---------------------|
| `portfolio.email.queue` | 24 hours | `portfolio.dlx.exchange` |
| `portfolio.image.queue` | 1 hour | `portfolio.dlx.exchange` |
| `portfolio.audit.queue` | 24 hours | `portfolio.dlx.exchange` |

### Image Processing Settings

```yaml
image:
  storage:
    image-max-width: ${IMAGE_MAX_WIDTH:1200}
    thumbnail-size: ${THUMBNAIL_SIZE:300}
    jpeg-quality: ${JPEG_QUALITY:0.85}
    thumbnail-quality: ${THUMBNAIL_QUALITY:0.80}
    keep-originals: ${IMAGE_KEEP_ORIGINALS:true}
```

---

## Database Schema

### Image Status Column

Added to `project_images` and `article_images` tables:

```sql
-- V22: Add status to project_images
ALTER TABLE project_images ADD COLUMN status VARCHAR(20) DEFAULT 'READY';

-- V23: Add status to article_images
ALTER TABLE article_images ADD COLUMN status VARCHAR(20) DEFAULT 'READY';
```

**Status values:**

| Status | Description |
|--------|-------------|
| `PROCESSING` | Image uploaded, processing in queue |
| `READY` | Processing complete, image available |
| `FAILED` | Processing failed, original available |

---

## Feature Toggle

Messaging can be disabled for development:

```yaml
rabbitmq:
  enabled: false
```

When disabled:
- `NoOpEventPublisher` is activated
- Operations fall back to synchronous processing where applicable
- Email sending becomes synchronous
- Image processing becomes synchronous

---

## Related Documentation

- [Messaging Architecture](../architecture/messaging.md) - Technical architecture
- [Image Processing](./image-processing.md) - Image processing details
- [Contact Form](./contact-form.md) - Contact form feature
- [Audit System](./audit-system.md) - Audit logging feature
- [Observability](../operations/observability.md) - RabbitMQ monitoring
