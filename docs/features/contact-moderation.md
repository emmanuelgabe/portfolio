# Contact Moderation and Auto-Reply

AI-powered contact form moderation with spam filtering, categorization, and automatic email responses.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Moderation Flow](#3-moderation-flow)
4. [Auto-Reply Flow](#4-auto-reply-flow)
5. [Contact Persistence](#5-contact-persistence)
6. [Admin Endpoints](#6-admin-endpoints)
7. [Configuration](#7-configuration)

---

## 1. Overview

Contact moderation analyzes incoming messages before they reach the RabbitMQ email queue. AI classifies each message, blocks spam, and generates personalized auto-replies for legitimate contacts.

**Key Characteristics**:
- Moderation runs synchronously in `ContactServiceImpl` before queue publish
- Fail-open contract: any AI failure defaults to approval
- Categories: MISSION, QUESTION, FEEDBACK, SPAM, INAPPROPRIATE
- Auto-reply uses RAG to generate contextual responses
- Contact persistence without email (GDPR compliance)
- 12-month automatic data retention with scheduled cleanup

---

## 2. Architecture

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `AiContactModerationServiceImpl` | `service/impl/` | AI moderation via Gemini |
| `AiContactAutoReplyServiceImpl` | `service/impl/` | RAG-powered reply generation |
| `ContactServiceImpl` | `service/impl/` | Orchestration (moderation + persistence + publish) |
| `ModerationPromptBuilder` | `moderation/` | System/user prompt construction |
| `EmailConsumer` | `messaging/consumer/` | Auto-reply generation after queue consumption |
| `AdminContactController` | `controller/` | Admin stats and history endpoints |
| `ContactRetentionScheduler` | `scheduler/` | GDPR 12-month bulk cleanup |

### Frontend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `ContactListComponent` | `pages/admin/contacts/` | Stats cards, history table, filters |
| `ContactAdminService` | `services/` | HTTP service for admin endpoints |

### Processing Flow

```
Contact Submission
    |
    v
Moderation Analysis (sync, timeout 5s)
    |
    +---> APPROVED
    |       +---> Persist Contact
    |       +---> Publish ContactEmailEvent --> RabbitMQ --> EmailConsumer
    |       +---> Publish AutoReplyEvent --> RabbitMQ --> AutoReplyConsumer
    |                                                        +---> Vector search (RAG)
    |                                                        +---> Generate reply (Gemini)
    |                                                        +---> Send auto-reply to sender
    |                                                        +---> Send enriched notification to owner
    |
    +---> REJECTED
            +---> Persist Contact
            +---> Silent rejection (no email sent)

HTTP Response: Always success (silent rejections)
```

---

## 3. Moderation Flow

### Step-by-Step

1. **Submission**: `ContactServiceImpl.sendContactEmail()` receives validated `ContactRequest`
2. **AI Analysis**: `ContactModerationService.moderate()` sends message to Gemini with structured prompt
3. **Classification**: Gemini returns JSON with verdict, category, reason, confidence score
4. **Persistence**: `Contact` entity saved with moderation results (no email stored)
5. **Verdict Action**:
   - APPROVED: email event published to RabbitMQ queue
   - REJECTED: no email sent, contact record kept for admin review

### Categories

| Category | Description | Verdict |
|----------|-------------|---------|
| MISSION | Job offers, freelance requests, collaboration proposals | APPROVED |
| QUESTION | Technical questions, information requests | APPROVED |
| FEEDBACK | Compliments, constructive criticism, portfolio comments | APPROVED |
| SPAM | SEO spam, marketing pitches, automated messages, suspicious links | REJECTED |
| INAPPROPRIATE | Abusive language, threats, harassment, offensive content | REJECTED |

### Fail-Open Contract

Any AI failure (timeout, parsing error, API error) results in automatic approval with confidence 1.0. This prevents blocking legitimate users during AI outages.

### Prompt Strategy

- System prompt defines 3-dimension analysis: spam detection, toxicity check, professional relevance
- User prompt wraps contact data in `<contact_data>` XML tags (injection protection)
- Expected JSON response: `{verdict, category, reason, confidenceScore}`
- Both French and English messages supported
- Moderation timeout: 5 seconds (configurable)

---

## 4. Auto-Reply Flow

### Step-by-Step

1. **Event Published**: only for APPROVED contacts, `AutoReplyEvent` sent to `portfolio.autoreply.queue`
2. **RAG Search**: vector store similarity search on contact subject + message (top 5 docs, threshold 0.3)
3. **Reply Generation**: Gemini generates contextual plain-text reply using retrieved portfolio documents
4. **Language Detection**: replies in same language as sender's message (default: French)
5. **Email Delivery**: two emails sent:
   - To sender: auto-reply with `Re: [Original Subject]` and disclaimer footer
   - To owner: enriched notification with original message + generated reply

### Error Handling

All failures are non-blocking:
- Generator failure: skip auto-reply
- Sender email failure: logged, continues
- Owner notification failure: logged, does not block
- Timeout: 10 seconds (configurable)

---

## 5. Contact Persistence

### Entity Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `name` | VARCHAR(100) | Submitter name |
| `subject` | VARCHAR(200) | Message subject |
| `message` | TEXT | Message content |
| `category` | VARCHAR(15) | AI-assigned category |
| `moderationVerdict` | VARCHAR(10) | APPROVED or REJECTED |
| `moderationReason` | TEXT | AI justification |
| `confidenceScore` | DOUBLE | 0.0 to 1.0 confidence level |
| `createdAt` | TIMESTAMP | Submission timestamp (immutable) |

**GDPR**: email address intentionally NOT stored. Already available in owner's mailbox.

### Retention Policy

- **Period**: 12 months (configurable via `contact.retention.months`)
- **Schedule**: daily at 3:30 AM (configurable via `contact.retention.cron`)
- **Method**: bulk `DELETE` query (no N+1)
- **Implementation**: `ContactRetentionScheduler` with transactional bulk delete

---

## 6. Admin Endpoints

**Base Path**: `/api/admin/contacts` (requires ADMIN role)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/stats` | Aggregated statistics for configurable period |
| GET | `/history` | Paginated contact history with category filter |
| GET | `/categories` | Available contact categories for filtering |

### Stats Response

| Field | Type | Description |
|-------|------|-------------|
| `totalCount` | long | Total contacts in period |
| `categoryCounts` | Map | Breakdown by category |
| `verdictCounts` | Map | Breakdown by verdict |
| `dailyActivity` | Map | Day-by-day contact counts |

Query parameter: `days` (default 30, range 1-365).

### History Response

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Contact ID |
| `name` | String | Submitter name |
| `subject` | String | Contact subject |
| `message` | String | Contact message |
| `category` | ContactCategory | AI-assigned category |
| `moderationVerdict` | ModerationVerdict | APPROVED or REJECTED |
| `moderationReason` | String | AI explanation |
| `confidenceScore` | double | Confidence level |
| `createdAt` | LocalDateTime | Submission timestamp |

Query parameters: `category` (optional filter), `page`, `size` (default 20), `sort` (default `createdAt,desc`).

### Grafana Dashboard

A dedicated Grafana moderation dashboard visualizes:
- Approval/rejection rates over time
- Category distribution
- Moderation response times

---

## 7. Configuration

### Moderation Properties

```yaml
ai:
  moderation:
    enabled: true
    timeout-seconds: ${AI_MODERATION_TIMEOUT:5}
```

### Auto-Reply Properties

```yaml
ai:
  autoreply:
    enabled: ${AI_AUTOREPLY_ENABLED:false}
    timeout-seconds: ${AI_AUTOREPLY_TIMEOUT:10}
    max-documents: ${AI_AUTOREPLY_MAX_DOCS:5}
```

### Retention Properties

```yaml
contact:
  retention:
    months: ${CONTACT_RETENTION_MONTHS:12}
    cron: "0 30 3 * * ?"
```

### RabbitMQ Queues

| Queue | Purpose |
|-------|---------|
| `portfolio.email.queue` | Contact notification to owner |
| `portfolio.autoreply.queue` | Auto-reply generation |
| `*.dlq` variants | Dead letter queues for failed messages |

---

## Related Documentation

- [Contact Form](./contact-form.md) - Base contact form feature
- [AI Chatbot](./ai-chatbot.md) - Agentic chatbot
- [Health Monitor](./health-monitor.md) - AI diagnostic agent
- [Messaging](./messaging.md) - RabbitMQ async processing
- [Security: Rate Limiting](../security/rate-limiting.md) - Rate limiting details
