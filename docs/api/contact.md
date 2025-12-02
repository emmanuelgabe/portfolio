# Contact API

---

## Table of Contents
1. [Overview](#1-overview)
2. [Endpoints](#2-endpoints)
   - 2.1 [Send Contact Message](#21-send-contact-message)
3. [Rate Limiting](#3-rate-limiting)
4. [Data Models](#4-data-models)
5. [Business Rules](#5-business-rules)
6. [Error Handling](#6-error-handling)

---

## 1. Overview

The Contact API provides a public endpoint for users to send contact messages. Features built-in rate limiting to prevent spam and abuse.

**Base Path**: `/api/contact`

**Features**:
- Public endpoint (no authentication required)
- Email sending functionality
- IP-based rate limiting (default: 5 requests per hour)
- Redis-backed rate limit tracking
- Automatic cleanup after 1-hour window
- Validation for all input fields

---

## 2. Endpoints

### 2.1 Send Contact Message

Send a contact form message (public endpoint with rate limiting).

**Endpoint**: `POST /api/contact`

**Authentication**: None (public)

**Rate Limit**: 5 requests per hour per IP address (configurable)

**Request Body**:
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "subject": "Inquiry about your services",
  "message": "Hello, I would like to know more about your portfolio projects and availability for freelance work."
}
```

**Request Schema**:
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | Yes | 2-100 chars | Sender's full name |
| `email` | string | Yes | Valid email, max 100 chars | Sender's email address |
| `subject` | string | Yes | 3-200 chars | Message subject line |
| `message` | string | Yes | 10-5000 chars | Message body |

**Success Response** (200 OK):
```json
{
  "message": "Your message has been sent successfully. We will get back to you soon.",
  "success": true,
  "timestamp": "2025-11-19T15:30:00.123"
}
```

**Error Responses**:

**Validation Error (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "name",
      "rejectedValue": "",
      "message": "Name is required"
    },
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "Email must be valid"
    },
    {
      "field": "subject",
      "rejectedValue": "Hi",
      "message": "Subject must be between 3 and 200 characters"
    },
    {
      "field": "message",
      "rejectedValue": "Short",
      "message": "Message must be between 10 and 5000 characters"
    }
  ],
  "path": "/api/contact"
}
```

**Rate Limit Exceeded (429 Too Many Requests)**:
```json
{
  "message": "Rate limit exceeded. You can send maximum 5 messages per hour. Please try again later.",
  "success": false,
  "timestamp": "2025-11-19T15:30:00.123"
}
```

**Email Send Failure (500 Internal Server Error)**:
```json
{
  "message": "Failed to send email. Please try again later or contact us directly.",
  "success": false,
  "timestamp": "2025-11-19T15:30:00.123"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/contact \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "subject": "Inquiry about your services",
    "message": "Hello, I would like to know more about your portfolio projects."
  }'
```

**Example Request (JavaScript)**:
```javascript
const response = await fetch('http://localhost:8080/api/contact', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: 'John Doe',
    email: 'john.doe@example.com',
    subject: 'Inquiry about your services',
    message: 'Hello, I would like to know more about your portfolio projects.'
  })
});

const data = await response.json();

if (response.status === 429) {
  alert('Rate limit exceeded. Please try again later.');
} else if (data.success) {
  alert('Message sent successfully!');
} else {
  alert('Failed to send message: ' + data.message);
}
```

---

## 3. Rate Limiting

### Overview

The Contact API implements IP-based rate limiting to prevent spam and abuse. Uses Redis for distributed rate limit tracking.

### Configuration

**Default Limits**:
- **Maximum Requests**: 5 requests per hour per IP
- **Window Size**: 1 hour (60 minutes)
- **Reset**: Automatic after 1 hour

**Configuration Property**:
```yaml
app:
  rate-limit:
    contact:
      max-requests-per-hour: 5  # Configurable in application.yml
```

### How Rate Limiting Works

1. **IP Address Extraction**: Client IP address extracted from `HttpServletRequest`
   - Supports X-Forwarded-For header for proxied requests
   - Uses RemoteAddr as fallback

2. **Redis Counter**:
   - Key format: `rate_limit:contact:{ip}`
   - Value: Request count
   - Expiration: 1 hour from first request

3. **Request Flow**:
   - Extract client IP address
   - Increment Redis counter for IP
   - Check if count exceeds limit
   - If exceeded: Return 429 Too Many Requests
   - If allowed: Process contact request
   - Set 1-hour expiration on first request

4. **Atomic Operations**:
   - Counter increment is atomic (thread-safe)
   - Expiration set only on first request (count == 1)
   - Prevents race conditions in distributed environments

### Rate Limit Headers

The current implementation does not include rate limit headers in responses. Consider adding these headers for better client experience:
- `X-RateLimit-Limit` - Maximum requests per hour
- `X-RateLimit-Remaining` - Remaining requests in current window
- `X-RateLimit-Reset` - Timestamp when the rate limit resets

### Redis Storage

**Key**: `rate_limit:contact:{ip}`

**Value**: Request count (Long)

**TTL**: 1 hour (3600 seconds)

**Example Redis entries**:
```
rate_limit:contact:192.168.1.100 = 3  (TTL: 2400 seconds remaining)
rate_limit:contact:10.0.0.50 = 5      (TTL: 1800 seconds remaining)
rate_limit:contact:172.16.0.1 = 1     (TTL: 3599 seconds remaining)
```

---

## 4. Data Models

### ContactRequest

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | Yes | 2-100 chars | Sender's full name |
| `email` | string | Yes | Valid email format, max 100 chars | Sender's email address |
| `subject` | string | Yes | 3-200 chars | Message subject line |
| `message` | string | Yes | 10-5000 chars | Message body |

**Validation Rules**:
- **name**: Cannot be blank, minimum 2 characters to prevent single-letter names
- **email**: Must match valid email regex pattern (RFC 5322 compliant)
- **subject**: Minimum 3 characters to ensure meaningful subject
- **message**: Minimum 10 characters to prevent spam, maximum 5000 to prevent abuse

**Example ContactRequest**:
```json
{
  "name": "Jane Smith",
  "email": "jane.smith@example.com",
  "subject": "Question about collaboration",
  "message": "I came across your portfolio and I'm impressed with your work on microservices architecture. Would you be available for a consulting project?"
}
```

---

### ContactResponse

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `message` | string | No | Response message (success or error description) |
| `success` | boolean | No | True if email sent successfully, false otherwise |
| `timestamp` | datetime | No | Response timestamp (ISO 8601 format) |

**Factory Methods**:
```java
// Success response
ContactResponse.success("Your message has been sent successfully.")

// Error response
ContactResponse.error("Rate limit exceeded. Please try again later.")
```

**Example Success Response**:
```json
{
  "message": "Your message has been sent successfully. We will get back to you soon.",
  "success": true,
  "timestamp": "2025-11-19T15:30:00.123"
}
```

**Example Error Response**:
```json
{
  "message": "Failed to send email. Please try again later.",
  "success": false,
  "timestamp": "2025-11-19T15:30:00.456"
}
```

---

## 5. Business Rules

### Message Validation

1. **Name Requirements**:
   - Minimum 2 characters (prevents single-letter entries)
   - Maximum 100 characters (prevents abuse)
   - Must not be blank or whitespace-only

2. **Email Requirements**:
   - Must be valid email format (RFC 5322 compliant)
   - Maximum 100 characters
   - Must not be blank

3. **Subject Requirements**:
   - Minimum 3 characters (ensures meaningful subject)
   - Maximum 200 characters
   - Must not be blank or whitespace-only

4. **Message Requirements**:
   - Minimum 10 characters (prevents spam)
   - Maximum 5000 characters (prevents abuse)
   - Must not be blank or whitespace-only

### Rate Limiting Rules

1. **IP-Based Throttling**:
   - Each IP address has independent quota
   - Quota resets automatically after 1 hour
   - Counter persists in Redis (survives application restarts)

2. **Default Quota**:
   - 5 requests per hour per IP address
   - Configurable via `app.rate-limit.contact.max-requests-per-hour`

3. **Rate Limit Enforcement**:
   - Checked before processing request
   - If exceeded: 429 Too Many Requests returned immediately
   - If allowed: Request processed normally

4. **Window Sliding**:
   - Fixed 1-hour window from first request
   - Counter expires automatically after 1 hour
   - New window starts on next request after expiration

### Email Handling

1. **Synchronous Processing**: Email sent synchronously (blocks request until complete)
2. **Error Handling**: Email send failures return 500 Internal Server Error
3. **No Retry Logic**: Failed sends are not retried automatically
4. **No Persistence**: Contact messages are not stored in database (sent via email only)

---

## 6. Error Handling

### Common Error Scenarios

| Scenario | HTTP Status | Response Format | Example |
|----------|-------------|-----------------|---------|
| Rate limit exceeded | 429 | ContactResponse | `{"message": "Rate limit exceeded...", "success": false}` |
| Missing required field | 400 | Validation Error | Field-specific validation errors |
| Invalid email format | 400 | Validation Error | `"Email must be valid"` |
| Message too short | 400 | Validation Error | `"Message must be between 10 and 5000 characters"` |
| Email send failure | 500 | ContactResponse | `{"message": "Failed to send email...", "success": false}` |

### Validation Error Response Structure

```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "name",
      "rejectedValue": "J",
      "message": "Name must be between 2 and 100 characters"
    },
    {
      "field": "email",
      "rejectedValue": "invalid",
      "message": "Email must be valid"
    },
    {
      "field": "subject",
      "rejectedValue": "Hi",
      "message": "Subject must be between 3 and 200 characters"
    },
    {
      "field": "message",
      "rejectedValue": "Hello",
      "message": "Message must be between 10 and 5000 characters"
    }
  ],
  "path": "/api/contact"
}
```

### Rate Limit Error Response

**Status Code**: 429 Too Many Requests

**Response Body**:
```json
{
  "message": "Rate limit exceeded. You can send maximum 5 messages per hour. Please try again later.",
  "success": false,
  "timestamp": "2025-11-19T15:30:00.123"
}
```

**Client Handling**:
1. Display user-friendly error message
2. Show remaining time until reset (if available)
3. Prevent form resubmission
4. Consider implementing exponential backoff for retries

### Email Send Failure Response

**Status Code**: 500 Internal Server Error

**Response Body**:
```json
{
  "message": "Failed to send email. Please try again later or contact us directly.",
  "success": false,
  "timestamp": "2025-11-19T15:30:00.123"
}
```

**Possible Causes**:
- Email server unavailable
- Network connectivity issues
- Authentication failure with email provider
- Invalid email configuration

---

## Related Documentation

- [API Overview](./README.md) - General API documentation
- [Features: Contact Form](../features/contact-form.md) - Contact form feature details
- [Security: Rate Limiting](../security/rate-limiting.md) - Rate limiting implementation
- [Reference: Configuration Properties](../reference/configuration-properties.md) - Configuration options
