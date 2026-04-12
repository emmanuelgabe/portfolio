# Agentic Chatbot

AI-powered conversational assistant with function calling, multi-step reasoning, and SSE streaming.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Chat Tools](#3-chat-tools)
4. [SSE Streaming](#4-sse-streaming)
5. [Conversation Memory](#5-conversation-memory)
6. [Rate Limiting](#6-rate-limiting)
7. [Configuration](#7-configuration)

---

## 1. Overview

The chatbot provides visitors with an interactive assistant that answers questions about projects, skills, experiences, and articles. Built on Spring AI with Google Gemini, it uses function calling to query real portfolio data.

**Key Characteristics**:
- Agentic architecture with 13 tools (all read-only on public data)
- SSE streaming with real-time tool call indicators
- In-memory conversation history (no persistence, GDPR-safe)
- Auto-detect visitor language (French/English)
- Rate limiting: 10 messages/hour/IP (production)
- Max 500 tokens per response

---

## 2. Architecture

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `ChatController` | `controller/` | REST endpoints (sync + SSE stream) |
| `RagChatService` | `chat/` | Agentic chat with function calling |
| `NoOpChatService` | `chat/` | Fallback when AI disabled |
| `ChatConfig` | `chat/` | Bean wiring and conditional activation |
| `ChatPromptBuilder` | `chat/` | System prompt construction |
| `ChatToolFactory` | `chat/` | Tool instance creation |
| `ConversationHistoryStore` | `chat/` | In-memory sliding window |
| `ToolCallTracker` | `chat/` | SSE tool call event tracking |

### Tool Classes

| Class | Location | Tools |
|-------|----------|-------|
| `ProjectSearchTool` | `chat/tools/` | searchProjects, listProjects, getFeaturedProjects, getProjectDetails |
| `ArticleSearchTool` | `chat/tools/` | listArticles, getArticle |
| `ExperienceTool` | `chat/tools/` | listExperiences, getRecentExperiences |
| `SkillTool` | `chat/tools/` | listSkills |
| `SiteInfoTool` | `chat/tools/` | getSiteConfig, getCurrentCV |
| `ContactPrepareTool` | `chat/tools/` | prepareContact |
| `PortfolioSearchTool` | `chat/tools/` | searchPortfolio (RAG vector search) |

### Frontend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `ChatWidgetComponent` | `components/chat-widget/` | Floating chat panel with SSE |
| `ChatService` | `services/` | Streaming fetch, conversation management |

### Request Flow

```
Visitor message
    |
    v
ChatController (rate limit check)
    |
    v
RagChatService (build prompt + inject tools)
    |
    v
Spring AI ChatClient (Gemini function calling)
    |
    +---> Tool calls (read-only service layer queries)
    |
    v
SSE stream tokens to frontend
```

---

## 3. Chat Tools

All tools are read-only and operate on public data. The agent decides which tools to call based on the visitor's question.

### Project Tools

| Tool | Parameters | Description |
|------|-----------|-------------|
| `searchProjects` | `query: String` | Search projects by keyword or technology |
| `listProjects` | - | List all projects ordered by display order |
| `getFeaturedProjects` | - | Get curated featured projects |
| `getProjectDetails` | `projectId: Long` | Full details of a specific project |

### Article Tools

| Tool | Parameters | Description |
|------|-----------|-------------|
| `listArticles` | `page: int, size: int` | Published articles with pagination (max 10/page) |
| `getArticle` | `slug: String` | Full article content by slug |

### Experience Tools

| Tool | Parameters | Description |
|------|-----------|-------------|
| `listExperiences` | `type: String (nullable)` | Experiences filtered by WORK/STAGE/EDUCATION/CERTIFICATION/VOLUNTEERING |
| `getRecentExperiences` | `limit: int` | Most recent experiences (max 10) |

### Skill Tools

| Tool | Parameters | Description |
|------|-----------|-------------|
| `listSkills` | `category: String (nullable)` | Skills filtered by FRONTEND/BACKEND/DATABASE/DEVOPS/TOOLS |

### Site Info Tools

| Tool | Parameters | Description |
|------|-----------|-------------|
| `getSiteConfig` | - | Site identity, bio, hero text, social links |
| `getCurrentCV` | - | Link to current CV/resume PDF |

### Contact Tool

| Tool | Parameters | Description |
|------|-----------|-------------|
| `prepareContact` | `subject: String, message: String` | Returns pre-filled contact form data (does NOT send) |

The frontend detects `prepareContact` usage and displays a button to open the contact form with pre-filled fields.

### RAG Search Tool

| Tool | Parameters | Description |
|------|-----------|-------------|
| `searchPortfolio` | `query: String` | Semantic search across embedded portfolio documents |

Uses Elasticsearch vector store with embedding similarity (threshold >= 0.3, max 5 results, 600 chars/result).

---

## 4. SSE Streaming

### Protocol

Server-Sent Events via HTTP/1.1 streaming (`SseEmitter`, timeout 180s).

### Event Types

| Event | Data | When |
|-------|------|------|
| `tool_calls` | JSON array of tool names | Before first token (batched) |
| `token` | Single text token | Repeated during generation |
| `contact_form` | `{subject, message}` JSON | When `prepareContact` is invoked |
| `done` | Empty | Stream complete |

### Frontend Parsing

The frontend uses `fetch()` with `ReadableStream` (not `EventSource`, since POST is required). Events are delimited by `\n\n`, with multiple `data:` lines within one event concatenated with newlines.

---

## 5. Conversation Memory

### Storage Model

In-memory `ConcurrentHashMap` keyed by `conversationId`. Each conversation holds a `Deque<Message>` with sliding window eviction.

| Parameter | Default | Description |
|-----------|---------|-------------|
| Window size | 20 messages | Oldest messages evicted on overflow |
| Idle TTL | 120 minutes | Conversations evicted after inactivity |
| Max conversations | 1000 | Prevents memory exhaustion |
| Cleanup interval | 30 minutes | Eviction task frequency |

**No persistence**: data lost on restart, no GDPR implications.

---

## 6. Rate Limiting

**Implementation**: Redis-backed per-IP rate limiting.

| Environment | Limit |
|-------------|-------|
| Production | 10 requests/hour |
| Development | 100 requests/hour |

**Response headers**: `X-RateLimit-Limit`, `X-RateLimit-Remaining` on every response.

**Exceeded**: HTTP 429 with headers set to `Remaining: 0`.

**Frontend**: warning displayed at 3 or fewer remaining requests, input disabled at 0.

---

## 7. Configuration

### Chat Properties

```yaml
ai:
  enabled: ${AI_ENABLED:false}
  chat:
    rate-limit:
      max-requests-per-hour: ${AI_CHAT_RATE_LIMIT:10}
    memory:
      window-size: 20
      idle-ttl-minutes: 120
      max-conversations: 1000
      cleanup-interval-ms: 1800000
```

### Model Properties

```yaml
spring.ai.google.genai:
  api-key: ${GOOGLE_GENAI_API_KEY:}
  chat:
    options:
      model: ${AI_CHAT_MODEL:gemini-2.5-flash}
      temperature: ${AI_CHAT_TEMPERATURE:0.7}
      max-output-tokens: ${AI_CHAT_MAX_TOKENS:500}
  embedding:
    text:
      options:
        model: ${AI_EMBEDDING_MODEL:gemini-embedding-001}
```

### Vector Store Properties

```yaml
ai:
  embedding:
    index-name: ${AI_EMBEDDING_INDEX:portfolio-embeddings}
    dimensions: ${AI_EMBEDDING_DIMENSIONS:3072}
```

### System Prompt Behavior

- **Language**: auto-detect from visitor message (French/English)
- **Tone**: warm, professional, third-person (never impersonates the portfolio owner)
- **Scope**: portfolio topics only, redirects off-topic questions
- **Security**: visitor message wrapped in XML tags for prompt injection defense
- **Output**: plain text only (no markdown, no emoji)

---

## Related Documentation

- [Contact Moderation](./contact-moderation.md) - AI moderation and auto-reply
- [Health Monitor](./health-monitor.md) - AI diagnostic agent
- [API Reference](../api/README.md) - Chat API endpoints
- [Features: Search](./search.md) - Elasticsearch integration
- [Architecture: Event Streaming](../architecture/event-streaming.md) - Vector store configuration
