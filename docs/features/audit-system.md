# Audit System

AOP-based audit logging for tracking all administrative actions with complete change history.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Usage](#usage)
4. [Configuration](#configuration)
5. [Database Schema](#database-schema)
6. [Related Documentation](#related-documentation)

---

## Overview

The audit system provides comprehensive tracking of:

- **Entity operations** - CREATE, UPDATE, DELETE on all entities
- **Business actions** - PUBLISH, UNPUBLISH, FEATURE, UNFEATURE
- **Authentication events** - LOGIN, LOGOUT, LOGIN_FAILED, PASSWORD_CHANGE
- **Change capture** - Old and new values for UPDATE/DELETE operations

Key capabilities:
- Non-blocking asynchronous logging via RabbitMQ
- SpEL expressions for dynamic entity ID/name extraction
- Automatic old/new value capture
- Sensitive field redaction (passwords, tokens)
- Export to CSV, JSON, and monthly PDF reports
- 90-day retention with automatic cleanup

---

## Architecture

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `@Auditable` | `audit/` | Method annotation for audit logging |
| `AuditAspect` | `audit/` | AOP aspect intercepting annotated methods |
| `AuditContext` | `audit/` | Request context (userId, IP, userAgent) |
| `AuditContextHolder` | `audit/` | ThreadLocal context storage |
| `AuditContextFilter` | `audit/` | Filter populating context at request start |
| `AuditLogSpecification` | `audit/` | JPA Specification for filtering |
| `AuditService` | `service/` | Audit log CRUD operations |
| `AuditServiceImpl` | `service/impl/` | Service implementation |
| `AuditConsumer` | `messaging/consumer/` | Async message consumer |
| `AdminAuditController` | `controller/` | REST API endpoints |

### Frontend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `AuditService` | `services/` | HTTP service for audit API |
| `AdminAuditListComponent` | `pages/admin/audit/` | Audit log viewer |

---

## Usage

### Annotating Methods

Use `@Auditable` annotation on service methods:

```java
@Auditable(
    action = AuditAction.CREATE,
    entityType = "Project",
    entityIdExpression = "#result.id",
    entityNameExpression = "#result.title"
)
public ProjectResponse createProject(CreateProjectRequest request) {
    // implementation
}

@Auditable(
    action = AuditAction.UPDATE,
    entityType = "Project",
    entityIdExpression = "#id",
    entityNameExpression = "#result.title",
    captureOldValues = true
)
public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
    // implementation
}

@Auditable(
    action = AuditAction.DELETE,
    entityType = "Project",
    entityIdExpression = "#id",
    captureOldValues = true
)
public void deleteProject(Long id) {
    // implementation
}
```

### SpEL Expressions

| Expression | Description |
|------------|-------------|
| `#id` | Method parameter named "id" |
| `#request.id` | Property from request object |
| `#result.id` | Property from return value |
| `#result.title` | Entity name from return value |

### Audit Actions

| Action | Description | Captures |
|--------|-------------|----------|
| `CREATE` | Entity created | New values |
| `UPDATE` | Entity updated | Old + new values |
| `DELETE` | Entity deleted | Old values |
| `PUBLISH` | Article published | Old + new values |
| `UNPUBLISH` | Article unpublished | Old + new values |
| `FEATURE` | Project featured | Old + new values |
| `UNFEATURE` | Project unfeatured | Old + new values |
| `SET_CURRENT` | CV set as current | Old + new values |
| `LOGIN` | User logged in | - |
| `LOGOUT` | User logged out | - |
| `LOGIN_FAILED` | Login attempt failed | - |
| `PASSWORD_CHANGE` | Password changed | - |

### Entity Types

| Entity Type | Audited Operations |
|-------------|-------------------|
| `PROJECT` | CRUD, FEATURE/UNFEATURE |
| `ARTICLE` | CRUD, PUBLISH/UNPUBLISH |
| `SKILL` | CRUD |
| `EXPERIENCE` | CRUD |
| `TAG` | CRUD |
| `CV` | CRUD, SET_CURRENT |
| `SITE_CONFIGURATION` | UPDATE |
| `USER` | Authentication events |

---

## Configuration

### Application Properties

```yaml
# Audit retention
audit:
  retention-days: ${AUDIT_RETENTION_DAYS:90}

# Batch cleanup job
batch:
  audit-cleanup:
    cron: "0 0 2 * * ?"  # Daily at 2:00 AM

# Batch report job
batch:
  audit-report:
    cron: "0 0 3 1 * ?"  # 1st of month at 3:00 AM
```

### Sensitive Fields

Fields automatically redacted in audit logs:

- `password`
- `passwordHash`
- `secret`
- `token`
- `refreshToken`
- `accessToken`

---

## Database Schema

### audit_logs Table

```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    entity_name VARCHAR(255),
    user_id BIGINT,
    username VARCHAR(100),
    user_role VARCHAR(50),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_id VARCHAR(36),
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],
    success BOOLEAN DEFAULT true,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_username_lower ON audit_logs(LOWER(username));
```

---

## Export Formats

### CSV Export

Includes columns: timestamp, action, entityType, entityId, entityName, username, ipAddress, success, changedFields

### JSON Export

Full audit log entries including old/new values as JSON objects.

### Monthly PDF Report

Generated automatically on the 1st of each month containing:
- Total actions by type
- Actions by entity type
- Actions by user
- Daily activity summary
- Top modified entities

---

## Related Documentation

- [Audit API](../api/audit.md) - REST API endpoints
- [Messaging](./messaging.md) - Async audit logging
- [Batch Processing](./batch-processing.md) - Cleanup and report jobs
