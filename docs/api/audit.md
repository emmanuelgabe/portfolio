# Audit API

REST API for viewing, filtering, and exporting audit logs.

---

## Table of Contents

1. [Overview](#overview)
2. [Admin Endpoints](#admin-endpoints)
3. [Data Models](#data-models)
4. [Error Codes](#error-codes)
5. [Related Documentation](#related-documentation)

---

## Overview

The Audit API provides access to the audit trail:

- View paginated audit logs with filtering
- Get entity change history
- Export logs to CSV/JSON
- View audit statistics

**Base URL:** `/api/admin/audit`

**Authentication:** Required (ADMIN role)

---

## Admin Endpoints

### Get Audit Logs

```
GET /api/admin/audit
```

Returns paginated audit logs with optional filtering.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `action` | string | Filter by action (CREATE, UPDATE, DELETE, etc.) |
| `entityType` | string | Filter by entity type (PROJECT, ARTICLE, etc.) |
| `entityId` | long | Filter by entity ID |
| `username` | string | Filter by username (case-insensitive) |
| `success` | boolean | Filter by success status |
| `startDate` | datetime | Filter by start date (ISO 8601) |
| `endDate` | datetime | Filter by end date (ISO 8601) |
| `page` | int | Page number (default: 0) |
| `size` | int | Page size (default: 20) |
| `sort` | string | Sort field (default: createdAt,desc) |

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "action": "CREATE",
      "entityType": "PROJECT",
      "entityId": 123,
      "entityName": "My Project",
      "username": "admin",
      "ipAddress": "192.168.1.1",
      "userAgent": "Mozilla/5.0...",
      "requestId": "uuid",
      "oldValues": null,
      "newValues": { "title": "My Project", "featured": false },
      "changedFields": ["title", "description"],
      "success": true,
      "errorMessage": null,
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```

---

### Get Entity History

```
GET /api/admin/audit/entity/{entityType}/{entityId}
```

Returns complete audit history for a specific entity.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `entityType` | string | Entity type (PROJECT, ARTICLE, etc.) |
| `entityId` | long | Entity ID |

**Response:**
```json
[
  {
    "id": 3,
    "action": "UPDATE",
    "entityType": "PROJECT",
    "entityId": 123,
    "entityName": "My Project Updated",
    "username": "admin",
    "oldValues": { "title": "My Project", "featured": false },
    "newValues": { "title": "My Project Updated", "featured": true },
    "changedFields": ["title", "featured"],
    "success": true,
    "createdAt": "2024-01-16T14:00:00Z"
  },
  {
    "id": 1,
    "action": "CREATE",
    "entityType": "PROJECT",
    "entityId": 123,
    "entityName": "My Project",
    "username": "admin",
    "oldValues": null,
    "newValues": { "title": "My Project" },
    "success": true,
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

---

### Get Statistics

```
GET /api/admin/audit/stats
```

Returns audit statistics for dashboard.

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `days` | int | 30 | Number of days (1-365) |

**Response:**
```json
{
  "totalLogs": 500,
  "successCount": 480,
  "failureCount": 20,
  "actionCounts": {
    "CREATE": 150,
    "UPDATE": 250,
    "DELETE": 50,
    "LOGIN": 40,
    "LOGOUT": 10
  },
  "entityTypeCounts": {
    "PROJECT": 200,
    "ARTICLE": 150,
    "SKILL": 100,
    "EXPERIENCE": 50
  },
  "dailyActivity": [
    { "date": "2024-01-15", "count": 45 },
    { "date": "2024-01-14", "count": 38 }
  ],
  "userActivity": [
    { "username": "admin", "count": 450 }
  ]
}
```

---

### Export to CSV

```
GET /api/admin/audit/export/csv
```

Exports audit logs to CSV format.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `action` | string | Filter by action |
| `entityType` | string | Filter by entity type |
| `startDate` | datetime | Filter by start date |
| `endDate` | datetime | Filter by end date |

**Response:**
- Content-Type: `text/csv; charset=UTF-8`
- Content-Disposition: `attachment; filename=audit-logs.csv`

**CSV Columns:**
```
timestamp,action,entityType,entityId,entityName,username,ipAddress,success,changedFields
```

---

### Export to JSON

```
GET /api/admin/audit/export/json
```

Exports audit logs to JSON format.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `action` | string | Filter by action |
| `entityType` | string | Filter by entity type |
| `startDate` | datetime | Filter by start date |
| `endDate` | datetime | Filter by end date |

**Response:**
- Content-Type: `application/json`
- Content-Disposition: `attachment; filename=audit-logs.json`

---

### Get Available Actions

```
GET /api/admin/audit/actions
```

Returns all available audit actions.

**Response:**
```json
[
  "CREATE",
  "UPDATE",
  "DELETE",
  "PUBLISH",
  "UNPUBLISH",
  "FEATURE",
  "UNFEATURE",
  "SET_CURRENT",
  "LOGIN",
  "LOGOUT",
  "LOGIN_FAILED",
  "PASSWORD_CHANGE"
]
```

---

### Get Available Entity Types

```
GET /api/admin/audit/entity-types
```

Returns all available entity types.

**Response:**
```json
[
  "PROJECT",
  "ARTICLE",
  "SKILL",
  "EXPERIENCE",
  "TAG",
  "CV",
  "SITE_CONFIGURATION",
  "USER"
]
```

---

## Data Models

### AuditLogResponse

```json
{
  "id": "long",
  "action": "string (AuditAction enum)",
  "entityType": "string",
  "entityId": "long (nullable)",
  "entityName": "string (nullable)",
  "username": "string (nullable)",
  "ipAddress": "string (nullable)",
  "userAgent": "string (nullable)",
  "requestId": "string (nullable)",
  "oldValues": "object (nullable)",
  "newValues": "object (nullable)",
  "changedFields": "string[] (nullable)",
  "success": "boolean",
  "errorMessage": "string (nullable)",
  "createdAt": "datetime"
}
```

### AuditStatsResponse

```json
{
  "totalLogs": "long",
  "successCount": "long",
  "failureCount": "long",
  "actionCounts": "Map<string, long>",
  "entityTypeCounts": "Map<string, long>",
  "dailyActivity": "DailyActivity[]",
  "userActivity": "UserActivity[]"
}
```

---

## Error Codes

| Status | Code | Description |
|--------|------|-------------|
| 400 | BAD_REQUEST | Invalid filter parameters |
| 401 | UNAUTHORIZED | Not authenticated |
| 403 | FORBIDDEN | Not authorized (requires ADMIN role) |
| 404 | NOT_FOUND | Entity not found |

---

## Related Documentation

- [Audit System](../features/audit-system.md) - Feature documentation
- [Authentication](../security/authentication.md) - Authentication details
