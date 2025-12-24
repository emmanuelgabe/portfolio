# Circuit Breaker

Resilience4j circuit breaker pattern for external service calls with retry and fallback.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Usage](#usage)
4. [Configuration](#configuration)
5. [Admin API Endpoints](#admin-api-endpoints)
6. [Related Documentation](#related-documentation)

---

## Overview

The circuit breaker pattern protects the application from cascading failures:

- **Fail-fast** when external services are unavailable
- **Automatic recovery** after configured wait duration
- **Retry mechanism** before circuit opens
- **Fallback handling** for graceful degradation

Key characteristics:
- Resilience4j implementation
- Annotation-driven configuration
- Prometheus metrics exposed
- Admin dashboard monitoring

---

## Architecture

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `EmailSenderServiceImpl` | `service/impl/` | Protected email sending |
| `AdminCircuitBreakerController` | `controller/` | Status monitoring API |
| `CircuitBreakerStatusResponse` | `dto/` | Status response DTO |

### Frontend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `CircuitBreakerService` | `services/` | HTTP service for status |
| `DashboardComponent` | `pages/admin/dashboard/` | Status display card |

### Circuit States

```
     ┌─────────────┐
     │   CLOSED    │ ← Normal operation, calls allowed
     │ (healthy)   │
     └──────┬──────┘
            │ failure rate exceeds threshold
            ▼
     ┌─────────────┐
     │    OPEN     │ ← Calls blocked, fast-fail
     │  (failing)  │
     └──────┬──────┘
            │ wait duration expires
            ▼
     ┌─────────────┐
     │  HALF_OPEN  │ ← Limited calls allowed
     │  (testing)  │
     └──────┬──────┘
            │ success → CLOSED
            │ failure → OPEN
            ▼
```

---

## Usage

### Annotating Methods

Apply `@CircuitBreaker` and `@Retry` annotations:

```java
@Retry(name = "emailService")
@CircuitBreaker(name = "emailService", fallbackMethod = "sendHtmlEmailFallback")
public void sendHtmlEmail(String from, String to, String subject, String body) {
    // Implementation
}

private void sendHtmlEmailFallback(String from, String to, String subject,
        String body, Throwable t) {
    log.warn("[EMAIL_SENDER] Circuit breaker fallback triggered - to={}, reason={}",
            to, t.getMessage());
    throw new EmailException("Email service unavailable", t);
}
```

### Execution Order

1. Retry wrapper executes first
2. Circuit breaker evaluates retry results
3. If all retries fail, circuit breaker may open
4. When open, fallback method is invoked immediately

---

## Configuration

### Application Properties

```yaml
resilience4j:
  circuitbreaker:
    instances:
      emailService:
        registerHealthIndicator: true
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 60s
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        recordExceptions:
          - jakarta.mail.MessagingException
          - org.springframework.mail.MailException
          - com.emmanuelgabe.portfolio.exception.EmailException

  retry:
    instances:
      emailService:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - jakarta.mail.MessagingException
          - org.springframework.mail.MailException
          - java.net.ConnectException
        ignoreExceptions:
          - com.emmanuelgabe.portfolio.exception.EmailException
```

### Configuration Parameters

**Circuit Breaker:**

| Parameter | Value | Description |
|-----------|-------|-------------|
| `slidingWindowType` | COUNT_BASED | Window based on call count |
| `slidingWindowSize` | 10 | Number of calls in window |
| `minimumNumberOfCalls` | 5 | Min calls before evaluating |
| `failureRateThreshold` | 50% | Threshold to open circuit |
| `waitDurationInOpenState` | 60s | Time before half-open |
| `permittedNumberOfCallsInHalfOpenState` | 3 | Test calls in half-open |

**Retry:**

| Parameter | Value | Description |
|-----------|-------|-------------|
| `maxAttempts` | 3 | Maximum retry attempts |
| `waitDuration` | 1s | Initial wait between retries |
| `enableExponentialBackoff` | true | Exponential delay increase |
| `exponentialBackoffMultiplier` | 2 | Backoff multiplier |

---

## Admin API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/circuit-breakers` | List all circuit breakers |
| GET | `/api/admin/circuit-breakers/{name}` | Get specific breaker |

### Response Model

```typescript
interface CircuitBreakerStatus {
  name: string;
  state: 'CLOSED' | 'OPEN' | 'HALF_OPEN' | 'DISABLED' | 'FORCED_OPEN';
  metrics: {
    failureCount: number;
    successCount: number;
    bufferedCalls: number;
    failureRate: number;
    notPermittedCalls: number;
  };
  timestamp: string;
}
```

---

## Related Documentation

- [Messaging](./messaging.md) - Email processing via RabbitMQ
- [Observability](../operations/observability.md) - Prometheus and Grafana

