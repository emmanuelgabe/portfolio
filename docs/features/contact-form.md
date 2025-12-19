# Contact Form Feature

---

## Table of Contents
1. [Overview](#1-overview)
2. [Form Structure](#2-form-structure)
3. [Rate Limiting](#3-rate-limiting)
4. [Email Sending](#4-email-sending)
5. [IP Address Extraction](#5-ip-address-extraction)
6. [Configuration](#6-configuration)

---

## 1. Overview

The Contact Form feature provides a public endpoint for users to send messages without requiring authentication.

**Key Capabilities**:
- Public contact form submission
- Email sending to portfolio owner
- IP-based rate limiting (default: 5 messages per hour)
- Redis-backed distributed rate limit tracking
- Automatic quota reset after 1-hour window
- Support for proxied requests (X-Forwarded-For header)

---

## 2. Form Structure

### ContactRequest DTO

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | String | Yes | 2-100 chars | Sender's full name |
| `email` | String | Yes | Valid email, max 100 chars | Sender's email address |
| `subject` | String | Yes | 3-200 chars | Message subject line |
| `message` | String | Yes | 10-5000 chars | Message body |

### Validation Rules

**Name**:
- `@NotBlank(message = "Name is required")`
- `@Size(min = 2, max = 100)`

**Email**:
- `@NotBlank`, `@Email`, `@Size(max = 100)`
- Must be RFC 5322 compliant

**Subject**:
- `@NotBlank`, `@Size(min = 3, max = 200)`

**Message**:
- `@NotBlank`, `@Size(min = 10, max = 5000)`

### ContactResponse DTO

| Field | Type | Description |
|-------|------|-------------|
| `message` | String | Response message |
| `success` | boolean | Success indicator |
| `timestamp` | LocalDateTime | Response timestamp |

---

## 3. Rate Limiting

### Rate Limit Strategy

**Implementation**: IP-based rate limiting using Redis

**Default Configuration**:
- **Max Requests**: 5 messages per hour per IP
- **Window Size**: 1 hour (fixed window)
- **Reset**: Automatic after 1 hour

### Rate Limit Flow

```
1. Client submits contact form
2. Extract client IP address
3. Check Redis counter for IP
4. If count > limit → Return 429 Too Many Requests
5. If count <= limit → Send email → Return 200 OK
```

### Redis Key Structure

**Key Format**: `rate_limit:contact:{ip}`

**Operations**:
- `INCR` - Atomic increment counter
- `EXPIRE` - Set 1-hour TTL on first request
- Automatic cleanup after expiration

---

## 4. Email Sending

### Asynchronous Email Sending

Email sending is handled asynchronously via **RabbitMQ**:

```
1. Client submits contact form
2. Backend validates request
3. Message published to RabbitMQ queue
4. Response returned immediately to client
5. Consumer processes queue and sends email via SMTP
6. Circuit breaker protects against SMTP failures
```

**Benefits**:
- Non-blocking response (faster user experience)
- Retry mechanism for transient failures
- Dead Letter Queue (DLQ) for failed messages
- Circuit breaker prevents cascade failures

---

## 5. IP Address Extraction

### Header Priority

1. **X-Forwarded-For**: Standard header for proxied requests
2. **Proxy-Client-IP**: Alternative proxy header
3. **WL-Proxy-Client-IP**: WebLogic proxy header
4. **RemoteAddr**: Direct connection IP (fallback)

### Multiple Proxies Handling

For `X-Forwarded-For: 203.0.113.5, 10.0.0.1`:
- First IP (203.0.113.5) is used as original client IP

---

## 6. Configuration

### Application Properties

**Rate Limit Configuration**:
```yaml
app:
  rate-limit:
    contact:
      max-requests-per-hour: 5  # Default quota per IP
```

**Email Configuration**:
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_APP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

**Redis Configuration**:
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

### Environment-Specific Settings

| Environment | Rate Limit |
|-------------|------------|
| Development | 10 requests/hour |
| Staging | 5 requests/hour |
| Production | 3 requests/hour |

---

## Related Documentation

- [Contact API](../api/contact.md) - Complete API reference
- [Security: Rate Limiting](../security/rate-limiting.md) - Rate limiting details
- [Reference: Configuration Properties](../reference/configuration-properties.md) - All configuration options
- [Features: Messaging](./messaging.md) - RabbitMQ async email sending
