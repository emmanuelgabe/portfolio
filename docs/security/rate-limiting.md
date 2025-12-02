# Rate Limiting

---

## Table of Contents
1. [Overview](#1-overview)
2. [Implementation](#2-implementation)
3. [Redis Storage](#3-redis-storage)
4. [Configuration](#4-configuration)
5. [Authentication Rate Limiting](#5-authentication-rate-limiting)

---

## 1. Overview

IP-based rate limiting using Redis for distributed tracking. Prevents spam and abuse on public endpoints.

**Current Implementation**: Contact form endpoint only

**Strategy**: Fixed window (1-hour window, resets after expiration)

**Storage**: Redis for distributed rate limit tracking

---

## 2. Implementation

### RateLimitService

**Interface**:
```java
public interface RateLimitService {
    boolean isAllowed(String ip);
    long getRemainingAttempts(String ip);
}
```

**Implementation**:
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

        // Set expiration on first request
        if (count == 1) {
            redisTemplate.expire(key, WINDOW_SIZE_HOURS, TimeUnit.HOURS);
        }

        return count <= maxRequestsPerHour;
    }

    @Override
    public long getRemainingAttempts(String ip) {
        String key = RATE_LIMIT_KEY_PREFIX + ip;
        String value = redisTemplate.opsForValue().get(key);
        long count = value != null ? Long.parseLong(value) : 0;
        return Math.max(0, maxRequestsPerHour - count);
    }
}
```

### Controller Integration

**Pattern**:
```java
@PostMapping
public ResponseEntity<?> sendContactMessage(
        @Valid @RequestBody ContactRequest request,
        HttpServletRequest httpRequest) {

    String ip = IpAddressExtractor.extractIpAddress(httpRequest);

    // Check rate limit
    if (!rateLimitService.isAllowed(ip)) {
        String errorMessage = String.format(
            "Rate limit exceeded. Maximum %d messages per hour.",
            maxRequestsPerHour
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ContactResponse.error(errorMessage));
    }

    // Process request
    ContactResponse response = contactService.sendContactEmail(request);
    return ResponseEntity.ok(response);
}
```

---

## 3. Redis Storage

### Key Format

**Pattern**: `rate_limit:contact:{ip}`

**Examples**:
```
rate_limit:contact:192.168.1.100 = 3   (TTL: 2400s)
rate_limit:contact:10.0.0.50 = 5       (TTL: 1800s)
```

### Atomic Operations

**Increment Counter**:
```java
Long count = redisTemplate.opsForValue().increment(key);
```

**Benefits**:
- Thread-safe in distributed environments
- No race conditions
- Automatic key creation if doesn't exist

**Set Expiration**:
```java
redisTemplate.expire(key, WINDOW_SIZE_HOURS, TimeUnit.HOURS);
```

**Benefits**:
- Automatic cleanup after 1 hour
- No manual cleanup required
- Memory efficient

---

## 4. Configuration

### Application Properties

**Rate Limit Configuration**:
```yaml
app:
  rate-limit:
    contact:
      max-requests-per-hour: 5  # Configurable per environment
```

**Redis Configuration**:
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 3000ms
```

### RedisTemplate Configuration

**Bean Configuration**:
```java
@Configuration
public class RateLimitConfig {
    @Bean
    public RedisTemplate<String, Long> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        return template;
    }
}
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

---

---

## 5. Authentication Rate Limiting

### AuthRateLimitFilter

A dedicated filter protects authentication endpoints against brute force attacks.

**Protected Endpoints**:
- `POST /api/auth/login`
- `POST /api/auth/refresh`

**Implementation**:
```java
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REFRESH_PATH = "/api/auth/refresh";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain) {

        String ip = IpAddressExtractor.extractIpAddress(request);

        if (LOGIN_PATH.equals(path)) {
            if (!authRateLimitService.isLoginAllowed(ip)) {
                sendRateLimitResponse(response, "login", maxLoginRequestsPerHour);
                return;
            }
        } else if (REFRESH_PATH.equals(path)) {
            if (!authRateLimitService.isRefreshAllowed(ip)) {
                sendRateLimitResponse(response, "refresh", maxRefreshRequestsPerHour);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### AuthRateLimitService

**Interface**:
```java
public interface AuthRateLimitService {
    boolean isLoginAllowed(String ip);
    boolean isRefreshAllowed(String ip);
    int getMaxLoginRequestsPerHour();
    int getMaxRefreshRequestsPerHour();
}
```

### Configuration

```yaml
app:
  rate-limit:
    auth:
      login:
        max-requests-per-hour: 10
      refresh:
        max-requests-per-hour: 20
```

### Response Format

When rate limit is exceeded, returns HTTP 429:

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded for login. Maximum 10 attempts per hour.",
  "timestamp": "2024-06-20T14:45:00"
}
```

---

## Related Documentation

- [Features: Contact Form](../features/contact-form.md) - Contact form implementation
- [Contact API](../api/contact.md) - API documentation
- [Authentication](./authentication.md) - JWT authentication
- [Reference: Configuration Properties](../reference/configuration-properties.md) - All config options
