# Error Tracking (Sentry)

Sentry integration for error tracking in backend and frontend applications.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Backend Integration](#2-backend-integration)
3. [Frontend Integration](#3-frontend-integration)
4. [Configuration](#4-configuration)
5. [Error Capture](#5-error-capture)

---

## 1. Overview

Sentry provides real-time error tracking and monitoring for both backend and frontend applications.

**Key Capabilities**:
- Automatic exception capture
- Stack trace analysis
- Error grouping and deduplication
- Release tracking
- Environment separation (local, staging, production)
- User context attachment
- GDPR compliance (feature flag)

**Feature Flag**: `SENTRY_ENABLED` controls error tracking activation.

---

## 2. Backend Integration

### 2.1 Dependencies

```gradle
implementation 'io.sentry:sentry-spring-boot-starter-jakarta:7.x'
implementation 'io.sentry:sentry-logback:7.x'
```

### 2.2 Captured Errors

| Type | Captured |
|------|----------|
| Uncaught exceptions | Yes (automatic) |
| HTTP 5xx errors | Yes (automatic) |
| HTTP 4xx errors | Configurable |
| Log errors (ERROR level) | Yes (via Logback) |
| Scheduled job failures | Yes |

### 2.3 Context Information

Captured with each error:
- Request URL, method, headers
- User ID (if authenticated)
- Environment (local/staging/prod)
- Release version
- Server name
- Transaction/span IDs

---

## 3. Frontend Integration

### 3.1 Dependencies

```json
{
  "dependencies": {
    "@sentry/angular": "^8.x",
    "@sentry/tracing": "^8.x"
  }
}
```

### 3.2 Captured Errors

| Type | Captured |
|------|----------|
| JavaScript exceptions | Yes |
| Unhandled promise rejections | Yes |
| Angular error handler | Yes |
| HTTP errors | Configurable |
| Console errors | Configurable |

### 3.3 Context Information

Captured with each error:
- Browser and OS
- URL and referrer
- User session ID
- Release version
- Angular component tree
- Redux/NgRx state (if applicable)

---

## 4. Configuration

### 4.1 Backend Configuration

```yaml
# application.yml
sentry:
  dsn: ${SENTRY_DSN:}
  environment: ${SPRING_PROFILES_ACTIVE:local}
  traces-sample-rate: 0.1
  send-default-pii: false
  in-app-includes:
    - com.emmanuelgabe.portfolio
  ignored-exceptions-for-type:
    - com.emmanuelgabe.portfolio.exception.ResourceNotFoundException
```

### 4.2 Frontend Configuration

```typescript
// environment.ts
export const environment = {
  sentryDsn: 'https://xxx@sentry.io/xxx',
  sentryEnabled: true,
  environment: 'production'
};
```

### 4.3 Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `SENTRY_DSN` | Backend Sentry DSN | Staging/Prod |
| `SENTRY_DSN_FRONTEND` | Frontend Sentry DSN | Staging/Prod |
| `SENTRY_ENABLED` | Enable/disable tracking | Optional |

### 4.4 DSN Setup

1. Create project at https://sentry.io
2. Go to Settings > Projects > [Project] > Client Keys (DSN)
3. Copy DSN URL
4. Add to environment variables or secrets

---

## 5. Error Capture

### 5.1 Automatic Capture

Sentry automatically captures:
- Unhandled exceptions in controllers
- Spring MVC errors
- Async method failures
- Scheduled task errors
- Angular uncaught exceptions

### 5.2 Manual Capture (Backend)

```java
// Capture exception with context
try {
    // risky operation
} catch (Exception e) {
    Sentry.captureException(e);
    throw e;
}

// Capture message
Sentry.captureMessage("Custom event occurred");

// With extra context
Sentry.withScope(scope -> {
    scope.setExtra("orderId", orderId);
    scope.setTag("module", "payments");
    Sentry.captureException(exception);
});
```

### 5.3 Manual Capture (Frontend)

```typescript
// Capture exception
try {
  // risky operation
} catch (error) {
  Sentry.captureException(error);
}

// Capture message
Sentry.captureMessage('User action failed');

// With context
Sentry.withScope(scope => {
  scope.setExtra('userId', userId);
  scope.setTag('component', 'checkout');
  Sentry.captureException(error);
});
```

### 5.4 Filtering Errors

**Backend (excluded exceptions)**:
- `ResourceNotFoundException` - Expected 404 errors
- `ValidationException` - User input errors
- `RateLimitExceededException` - Rate limiting

**Frontend (excluded errors)**:
- Network errors (handled separately)
- User-initiated cancellations
- Third-party script errors

---

## 6. GDPR Compliance

### 6.1 Feature Flag

Error tracking can be disabled for GDPR compliance:

```yaml
sentry:
  enabled: ${SENTRY_ENABLED:true}
```

### 6.2 PII Handling

- `send-default-pii: false` - No IP addresses or user agents
- User IDs are anonymized
- Request bodies are stripped of sensitive data
- Cookies are not captured

---

## Related Documentation

- [Security: Initial Setup](../security/initial-setup.md) - Environment variables
- [Security: Infrastructure Credentials](../security/infrastructure-credentials.md) - Sentry DSN configuration
- [Operations: Observability](../operations/observability.md) - Monitoring overview
- [Reference: Configuration Properties](../reference/configuration-properties.md) - All configuration options
