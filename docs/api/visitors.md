# Visitors API

REST API for visitor tracking and real-time analytics.

---

## Table of Contents

1. [Overview](#overview)
2. [Public Endpoints](#public-endpoints)
3. [Admin Endpoints](#admin-endpoints)
4. [Data Models](#data-models)
5. [Error Codes](#error-codes)
6. [Related Documentation](#related-documentation)

---

## Overview

The Visitors API provides:

- Public heartbeat endpoint for anonymous tracking
- Admin endpoints for real-time statistics
- SSE stream for live dashboard updates
- Historical visitor data for charts

**Authentication:**
- Public endpoints: No authentication
- Admin endpoints: JWT Bearer token (ADMIN role)

---

## Public Endpoints

### Register Heartbeat

```
POST /api/visitors/heartbeat
```

Registers a visitor heartbeat to track active sessions.

**Headers:**

| Header | Type | Required | Description |
|--------|------|----------|-------------|
| `X-Session-Id` | string | Yes | UUID session identifier |

**Response:** `200 OK` (empty body)

**Notes:**
- Call every 30 seconds from frontend
- Session expires after 60 seconds without heartbeat
- No request body required

---

## Admin Endpoints

### Get Active Users Count

```
GET /api/admin/visitors/count
```

Returns current active visitors count.

**Response:**
```json
{
  "count": 42,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

### Get Visitor Statistics

```
GET /api/admin/visitors/stats
```

Returns active count and last month's total visitors.

**Response:**
```json
{
  "activeCount": 42,
  "lastMonthCount": 1250,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**Calculation:**
- `activeCount`: Current Redis session count
- `lastMonthCount`: Sum of unique visitors from DailyStats (previous full month)

---

### Get Daily Visitor Data

```
GET /api/admin/visitors/daily
```

Returns unique visitors per day for the last 7 days.

**Response:**
```json
[
  { "date": "2024-01-09", "count": 145 },
  { "date": "2024-01-10", "count": 167 },
  { "date": "2024-01-11", "count": 189 },
  { "date": "2024-01-12", "count": 134 },
  { "date": "2024-01-13", "count": 98 },
  { "date": "2024-01-14", "count": 112 },
  { "date": "2024-01-15", "count": 156 }
]
```

**Notes:**
- Returns 7 days from oldest to newest
- Missing days have count = 0
- Data sourced from DailyStats table

---

### Stream Active Users (SSE)

```
GET /api/admin/visitors/stream
Content-Type: text/event-stream
```

Server-Sent Events stream for real-time active users count.

**Event Format:**
```
event: active-users
data: {"count":42,"timestamp":"2024-01-15T10:30:00Z"}

event: active-users
data: {"count":45,"timestamp":"2024-01-15T10:30:30Z"}
```

**Behavior:**
- Initial count sent immediately on connection
- Updates broadcast every 30 seconds
- Connection kept alive indefinitely
- Automatic cleanup on client disconnect

**Client Implementation:**
```typescript
const response = await fetch('/api/admin/visitors/stream', {
  headers: {
    Authorization: `Bearer ${token}`,
    Accept: 'text/event-stream',
  },
});

const reader = response.body.getReader();
const decoder = new TextDecoder();

while (true) {
  const { done, value } = await reader.read();
  if (done) break;

  const text = decoder.decode(value);
  // Parse SSE format and extract data
}
```

---

## Data Models

### ActiveUsersResponse

```json
{
  "count": "integer",
  "timestamp": "datetime (ISO 8601)"
}
```

### VisitorStatsResponse

```json
{
  "activeCount": "integer",
  "lastMonthCount": "long",
  "timestamp": "datetime (ISO 8601)"
}
```

### DailyVisitorData

```json
{
  "date": "date (YYYY-MM-DD)",
  "count": "long"
}
```

---

## Error Codes

| Status | Code | Description |
|--------|------|-------------|
| 400 | BAD_REQUEST | Missing X-Session-Id header |
| 401 | UNAUTHORIZED | Not authenticated (admin endpoints) |
| 403 | FORBIDDEN | Not authorized (requires ADMIN role) |

---

## Related Documentation

- [Visitor Tracking](../features/visitor-tracking.md) - Feature documentation
- [Authentication](../security/authentication.md) - JWT authentication

