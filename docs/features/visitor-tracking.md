# Visitor Tracking

Real-time visitor tracking using Redis sessions and Server-Sent Events (SSE).

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Session Management](#session-management)
4. [Real-time Updates](#real-time-updates)
5. [Configuration](#configuration)
6. [Related Documentation](#related-documentation)

---

## Overview

The visitor tracking system provides real-time analytics:

- **Active visitors** - Current users browsing the site
- **Daily unique visitors** - Aggregated by day for reporting
- **Real-time updates** - SSE stream for admin dashboard
- **Historical data** - 7-day chart with daily breakdowns

Key characteristics:
- Redis-based session storage with automatic TTL
- Heartbeat mechanism (30-second intervals)
- SSE broadcasting for live dashboard updates
- No cookies or PII stored

---

## Architecture

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `VisitorTrackingService` | `service/` | Service interface |
| `VisitorTrackingServiceImpl` | `service/impl/` | Redis implementation |
| `VisitorController` | `controller/` | Public heartbeat endpoint |
| `AdminActiveUsersController` | `controller/` | Admin SSE and stats |
| `DailyStatsRepository` | `repository/` | Historical data access |

### Frontend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `VisitorTrackerService` | `services/` | Heartbeat sender |
| `ActiveUsersService` | `services/` | SSE consumer |
| `VisitorsChartComponent` | `components/shared/` | Chart.js visualization |

### Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     Public Frontend                          │
│  VisitorTrackerService sends heartbeat every 30 seconds     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     VisitorController                        │
│  POST /api/visitors/heartbeat                               │
│  Header: X-Session-Id (UUID from sessionStorage)            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  VisitorTrackingService                      │
│  1. Set Redis key with TTL (60s) for active tracking        │
│  2. Add to daily unique visitors set (7-day TTL)            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                          Redis                               │
│  visitor:{sessionId} → timestamp (TTL: 60s)                 │
│  visitors:daily:{YYYY-MM-DD} → Set of sessionIds (TTL: 7d) │
└─────────────────────────────────────────────────────────────┘
```

---

## Session Management

### Session ID Generation

Frontend generates a UUID stored in sessionStorage:

```typescript
private getOrCreateSessionId(): string {
  let sessionId = sessionStorage.getItem(this.SESSION_KEY);
  if (!sessionId) {
    sessionId = crypto.randomUUID();
    sessionStorage.setItem(this.SESSION_KEY, sessionId);
  }
  return sessionId;
}
```

Session ID characteristics:
- Generated client-side using `crypto.randomUUID()`
- Persists across page refreshes (sessionStorage)
- Cleared on browser tab close
- Not linked to any user identity

### Redis Key Structure

| Key Pattern | Value | TTL | Purpose |
|-------------|-------|-----|---------|
| `visitor:{sessionId}` | timestamp | 60s | Active session marker |
| `visitors:daily:{YYYY-MM-DD}` | Set of sessionIds | 7 days | Daily unique tracking |

### Heartbeat Flow

1. Frontend starts tracking on page load
2. Immediate heartbeat sent
3. Interval heartbeat every 30 seconds
4. Backend updates Redis key TTL
5. Key expires after 60s without heartbeat

---

## Real-time Updates

### Server-Sent Events (SSE)

Admin dashboard receives live updates via SSE:

```
GET /api/admin/visitors/stream
Content-Type: text/event-stream
Authorization: Bearer {token}
```

**Event Format:**
```
event: active-users
data: {"count": 42, "timestamp": "2024-01-15T10:30:00Z"}
```

### SSE Implementation

Backend maintains list of active emitters:

```java
private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

@Scheduled(fixedRate = 30000)
public void broadcastActiveUsers() {
    if (emitters.isEmpty()) return;

    ActiveUsersResponse response = new ActiveUsersResponse(
        visitorTrackingService.getActiveVisitorsCount(),
        Instant.now()
    );

    for (SseEmitter emitter : emitters) {
        emitter.send(SseEmitter.event()
            .name("active-users")
            .data(response, MediaType.APPLICATION_JSON));
    }
}
```

### Frontend SSE Consumption

Uses Fetch API with streaming for authenticated SSE:

```typescript
private async connectWithFetch(token: string): Promise<void> {
  const response = await fetch(`${this.apiUrl}/api/admin/visitors/stream`, {
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'text/event-stream',
    },
  });

  const reader = response.body.getReader();
  // ... process stream
}
```

Features:
- Automatic reconnection (5 attempts)
- BehaviorSubject for reactive updates
- NgZone integration for change detection

---

## Configuration

### Application Properties

```yaml
# Redis connection (required for visitor tracking)
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

### Constants

| Constant | Value | Description |
|----------|-------|-------------|
| Heartbeat interval | 30 seconds | Frontend sends heartbeat |
| Session TTL | 60 seconds | Redis key expiration |
| Daily set TTL | 7 days | Historical data retention |
| SSE broadcast rate | 30 seconds | Admin dashboard updates |
| Max reconnect attempts | 5 | SSE client reconnection |

### Docker Compose (Redis)

```yaml
services:
  redis:
    image: redis:7-alpine
    container_name: portfolio-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3
```

---

## Related Documentation

- [Visitors API](../api/visitors.md) - REST API endpoints
- [Batch Processing](./batch-processing.md) - Stats aggregation job
- [Rate Limiting](../security/rate-limiting.md) - Redis-backed rate limiting

