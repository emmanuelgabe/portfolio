# Contact Form Feature

---

## Table of Contents
1. [Overview](#1-overview)
2. [Form Structure](#2-form-structure)
3. [Rate Limiting](#3-rate-limiting)
4. [Email Sending](#4-email-sending)
5. [IP Address Extraction](#5-ip-address-extraction)
6. [Redis Integration](#6-redis-integration)
7. [Configuration](#7-configuration)

---

## 1. Overview

The Contact Form feature provides a public endpoint for users to send messages without requiring authentication. Built with spam prevention through IP-based rate limiting and Redis-backed quota tracking.

**Key Capabilities**:
- Public contact form submission
- Email sending to portfolio owner
- IP-based rate limiting (default: 5 messages per hour)
- Redis-backed distributed rate limit tracking
- Automatic quota reset after 1-hour window
- Comprehensive input validation
- Support for proxied requests (X-Forwarded-For header)

**Security Features**:
- Rate limiting prevents spam and abuse
- IP tracking for quota management
- Validation of all input fields
- No authentication required (public access)

---

## 2. Form Structure

### ContactRequest DTO

**Request Model** (`com.emmanuelgabe.portfolio.dto.ContactRequest`):

```java
@Data
public class ContactRequest {
    private String name;      // 2-100 characters
    private String email;     // Valid email, max 100 characters
    private String subject;   // 3-200 characters
    private String message;   // 10-5000 characters
}
```

### Field Details

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | String | Yes | 2-100 chars | Sender's full name |
| `email` | String | Yes | Valid email, max 100 chars | Sender's email address |
| `subject` | String | Yes | 3-200 chars | Message subject line |
| `message` | String | Yes | 10-5000 chars | Message body |

### Validation Rules

**Name Validation**:
```java
@NotBlank(message = "Name is required")
@Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
```
- Minimum 2 characters prevents single-letter entries
- Maximum 100 characters prevents abuse

**Email Validation**:
```java
@NotBlank(message = "Email is required")
@Email(message = "Email must be valid")
@Size(max = 100, message = "Email must not exceed 100 characters")
```
- Must be RFC 5322 compliant email format
- Examples: `john.doe@example.com`, `user+tag@domain.co.uk`

**Subject Validation**:
```java
@NotBlank(message = "Subject is required")
@Size(min = 3, max = 200, message = "Subject must be between 3 and 200 characters")
```
- Minimum 3 characters ensures meaningful subject
- Maximum 200 characters for reasonable length

**Message Validation**:
```java
@NotBlank(message = "Message is required")
@Size(min = 10, max = 5000, message = "Message must be between 10 and 5000 characters")
```
- Minimum 10 characters prevents spam
- Maximum 5000 characters prevents abuse

### ContactResponse DTO

**Response Model** (`com.emmanuelgabe.portfolio.dto.ContactResponse`):

```java
@Data
public class ContactResponse {
    private String message;     // Response message
    private boolean success;    // Success indicator
    private LocalDateTime timestamp;  // Response timestamp
}
```

**Factory Methods**:
```java
ContactResponse.success("Your message has been sent successfully.")
ContactResponse.error("Rate limit exceeded. Please try again later.")
```

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
4. If count > limit:
   → Return 429 Too Many Requests
5. If count <= limit:
   → Send email
   → Return 200 OK
```

### RateLimitService Interface

**Service Contract** (`com.emmanuelgabe.portfolio.service.RateLimitService`):

```java
public interface RateLimitService {
    boolean isAllowed(String ip);
    long getRemainingAttempts(String ip);
}
```

**Method Descriptions**:
- `isAllowed(ip)`: Checks if IP is allowed to make a request, increments counter
- `getRemainingAttempts(ip)`: Returns remaining quota without incrementing

### Rate Limit Implementation

**Service Implementation** (`com.emmanuelgabe.portfolio.service.impl.RateLimitServiceImpl`):

```java
@Service
public class RateLimitServiceImpl implements RateLimitService {
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:contact:";
    private static final long WINDOW_SIZE_HOURS = 1;

    @Value("${app.rate-limit.contact.max-requests-per-hour:5}")
    private int maxRequestsPerHour;

    @Override
    public boolean isAllowed(String ip) {
        String key = RATE_LIMIT_KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, WINDOW_SIZE_HOURS, TimeUnit.HOURS);
        }

        return count <= maxRequestsPerHour;
    }
}
```

### Rate Limit Enforcement

**Controller Logic**:
```java
@PostMapping
public ResponseEntity<ContactResponse> sendContactMessage(
        @Valid @RequestBody ContactRequest request,
        HttpServletRequest httpRequest) {

    String ip = IpAddressExtractor.extractIpAddress(httpRequest);

    // Check rate limiting
    if (!rateLimitService.isAllowed(ip)) {
        String errorMessage = String.format(
            "Rate limit exceeded. You can send maximum %d messages per hour.",
            maxRequestsPerHour
        );
        ContactResponse response = ContactResponse.error(errorMessage);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    // Send email
    ContactResponse response = contactService.sendContactEmail(request);
    return ResponseEntity.ok(response);
}
```

---

## 4. Email Sending

### ContactService Interface

**Service Contract** (`com.emmanuelgabe.portfolio.service.ContactService`):

```java
public interface ContactService {
    ContactResponse sendContactEmail(ContactRequest request);
}
```

### Email Sending Process

1. **Validate Request**: Validated by `@Valid` annotation before reaching service
2. **Compose Email**: Create email with sender details and message
3. **Send Email**: Use configured email service (SMTP)
4. **Return Response**: Success or error response

### Email Configuration

**Required Properties**:
```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: your-email@example.com
    password: your-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

### Error Handling

**Email Send Failure**:
```java
try {
    // Send email logic
    return ContactResponse.success("Your message has been sent successfully.");
} catch (Exception e) {
    log.error("[CONTACT] Failed to send email - error={}", e.getMessage(), e);
    return ContactResponse.error("Failed to send email. Please try again later.");
}
```

**HTTP Status**:
- Success: 200 OK
- Email Failure: 500 Internal Server Error

---

## 5. IP Address Extraction

### IpAddressExtractor Utility

**Purpose**: Extract client IP address from HTTP request, supporting proxied requests

**Implementation** (`com.emmanuelgabe.portfolio.util.IpAddressExtractor`):

```java
public class IpAddressExtractor {
    public static String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For can contain multiple IPs (comma-separated)
        // The first IP is the original client IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
```

### Header Priority

1. **X-Forwarded-For**: Standard header for proxied requests
2. **Proxy-Client-IP**: Alternative proxy header
3. **WL-Proxy-Client-IP**: WebLogic proxy header
4. **RemoteAddr**: Direct connection IP (fallback)

### Use Cases

**Direct Connection**:
```
Client (192.168.1.100) → Server
→ IP = 192.168.1.100 (from RemoteAddr)
```

**Proxied Connection**:
```
Client (203.0.113.5) → Proxy (10.0.0.1) → Server
→ X-Forwarded-For: 203.0.113.5
→ IP = 203.0.113.5 (from X-Forwarded-For)
```

**Multiple Proxies**:
```
Client (203.0.113.5) → Proxy1 (10.0.0.1) → Proxy2 (10.0.0.2) → Server
→ X-Forwarded-For: 203.0.113.5, 10.0.0.1
→ IP = 203.0.113.5 (first IP in chain)
```

---

## 6. Redis Integration

### Redis Configuration

**Configuration Class** (`com.emmanuelgabe.portfolio.config.RateLimitConfig`):

```java
@Configuration
public class RateLimitConfig {
    @Bean
    public RedisTemplate<String, Long> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        return template;
    }
}
```

### Redis Key Structure

**Key Format**: `rate_limit:contact:{ip}`

**Examples**:
```
rate_limit:contact:192.168.1.100 = 3   (TTL: 2400 seconds)
rate_limit:contact:10.0.0.50 = 5       (TTL: 1800 seconds)
rate_limit:contact:172.16.0.1 = 1      (TTL: 3599 seconds)
```

### Redis Operations

**Increment Counter**:
```java
Long count = redisTemplate.opsForValue().increment(key);
```
- Atomically increments counter
- Creates key if doesn't exist
- Thread-safe in distributed environment

**Set Expiration**:
```java
redisTemplate.expire(key, WINDOW_SIZE_HOURS, TimeUnit.HOURS);
```
- Sets TTL of 1 hour on first request
- Automatic cleanup after expiration

**Get Counter**:
```java
String value = redisTemplate.opsForValue().get(key);
long count = Long.parseLong(value);
```
- Retrieves current count without incrementing

### Redis Connection

**Required Properties**:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      timeout: 3000ms
```

---

## 7. Configuration

### Application Properties

**Rate Limit Configuration**:
```yaml
app:
  rate-limit:
    contact:
      max-requests-per-hour: 5  # Configurable quota per IP
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

**Development**:
```yaml
app:
  rate-limit:
    contact:
      max-requests-per-hour: 10  # More lenient for testing
```

**Production**:
```yaml
app:
  rate-limit:
    contact:
      max-requests-per-hour: 3   # Stricter for production
```

### Configuration Injection

**Controller Level**:
```java
@Value("${app.rate-limit.contact.max-requests-per-hour:5}")
private int maxRequestsPerHour;
```

**Service Level**:
```java
@Value("${app.rate-limit.contact.max-requests-per-hour:5}")
private int maxRequestsPerHour;
```

**Default Value**: 5 (used if property not configured)

---

## Related Documentation

- [Contact API](../api/contact.md) - Complete API reference
- [Security: Rate Limiting](../security/rate-limiting.md) - Rate limiting implementation details
- [Reference: Configuration Properties](../reference/configuration-properties.md) - All configuration options
