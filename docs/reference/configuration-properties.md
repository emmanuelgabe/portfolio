# Configuration Properties Reference

---

## Table of Contents
1. [JWT Configuration](#1-jwt-configuration)
2. [Rate Limiting](#2-rate-limiting)
3. [File Storage](#3-file-storage)
4. [Database](#4-database)
5. [Redis](#5-redis)
6. [Email](#6-email)
7. [Server](#7-server)

---

## 1. JWT Configuration

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}              # HS256 secret key (min 256 bits)
    expiration: 900000                 # Access token expiration (ms): 15 minutes
    refresh-expiration: 7              # Refresh token expiration (days): 7 days
    issuer: portfolio-backend          # Token issuer claim
    audience: portfolio-frontend       # Token audience claim
```

**Environment Variables**:
- `JWT_SECRET` - Secret key for signing JWTs (required)

---

## 2. Rate Limiting

```yaml
app:
  rate-limit:
    contact:
      max-requests-per-hour: 5  # Maximum contact form submissions per IP per hour
```

**Defaults**: 5 requests/hour

**Environment-Specific**:
- Dev: 10 requests/hour
- Staging: 5 requests/hour
- Prod: 3 requests/hour

---

## 3. File Storage

```yaml
app:
  file-storage:
    upload-dir: uploads/              # Base directory for file uploads
    max-file-size: 10485760           # Max file size (bytes): 10 MB
    allowed-extensions:               # Allowed file extensions
      - jpg
      - jpeg
      - png
      - webp
      - pdf
```

**Subdirectories**:
- `uploads/projects/` - Project images
- `uploads/cvs/` - CV files
- `uploads/articles/` - Article images

---

## 4. Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:portfolio}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # Use Flyway for schema management
    show-sql: false       # Set to true in dev for SQL logging
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

**Environment Variables**:
- `DB_HOST` - Database host (default: localhost)
- `DB_PORT` - Database port (default: 5432)
- `DB_NAME` - Database name (default: portfolio)
- `DB_USERNAME` - Database username (default: postgres)
- `DB_PASSWORD` - Database password (required)

---

## 5. Redis

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 3000ms
```

**Environment Variables**:
- `REDIS_HOST` - Redis host (default: localhost)
- `REDIS_PORT` - Redis port (default: 6379)
- `REDIS_PASSWORD` - Redis password (optional)

**Use Cases**:
- Rate limiting (contact form)
- Session management (future)
- Caching (future)

---

## 6. Email

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_APP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

**Environment Variables**:
- `MAIL_HOST` - SMTP server host (default: smtp.gmail.com)
- `MAIL_PORT` - SMTP server port (default: 587)
- `MAIL_USERNAME` - SMTP username (required)
- `MAIL_APP_PASSWORD` - SMTP password (required)

---

## 7. Server

```yaml
server:
  port: ${PORT:8080}
  servlet:
    context-path: /
  error:
    include-message: always
    include-binding-errors: always
    include-stacktrace: never  # Set to on_param in dev
```

**Environment Variables**:
- `PORT` - Server port (default: 8080)

---

## Environment Profiles

### Development (`application-dev.yml`)

```yaml
spring:
  jpa:
    show-sql: true  # SQL logging enabled

logging:
  level:
    root: INFO
    com.emmanuelgabe.portfolio: DEBUG

app:
  rate-limit:
    contact:
      max-requests-per-hour: 10  # Lenient for testing
```

### Staging (`application-staging.yml`)

```yaml
logging:
  level:
    root: INFO
    com.emmanuelgabe.portfolio: INFO

app:
  rate-limit:
    contact:
      max-requests-per-hour: 5
```

### Production (`application-prod.yml`)

```yaml
logging:
  level:
    root: WARN
    com.emmanuelgabe.portfolio: WARN

app:
  rate-limit:
    contact:
      max-requests-per-hour: 3  # Strict for production
```

---

## Related Documentation

- [Development: Setup](../development/setup.md) - Environment setup
- [Reference: Environments](./environments.md) - Environment details
- [Security: JWT Implementation](../security/jwt-implementation.md) - JWT configuration
- [Security: Rate Limiting](../security/rate-limiting.md) - Rate limit configuration
