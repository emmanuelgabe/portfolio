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
8. [RabbitMQ](#8-rabbitmq)
9. [Kafka](#9-kafka)
10. [Elasticsearch](#10-elasticsearch)
11. [Resilience4j](#11-resilience4j)
12. [Spring Batch](#12-spring-batch)
13. [GraphQL](#13-graphql)
14. [Sitemap](#14-sitemap)
15. [Caching](#15-caching)
16. [AI / Spring AI](#16-ai--spring-ai)
17. [Contact Retention](#17-contact-retention)

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

## 8. RabbitMQ

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: ${RABBITMQ_VHOST:/}
    publisher-confirm-type: correlated
    publisher-returns: true

rabbitmq:
  enabled: ${RABBITMQ_ENABLED:true}
  queues:
    email: portfolio.email.queue
    image: portfolio.image.queue
    audit: portfolio.audit.queue
    autoreply: portfolio.autoreply.queue
```

**Environment Variables**:
- `RABBITMQ_HOST` - RabbitMQ host (default: localhost)
- `RABBITMQ_PORT` - RabbitMQ port (default: 5672)
- `RABBITMQ_USERNAME` - RabbitMQ username (default: guest)
- `RABBITMQ_PASSWORD` - RabbitMQ password (default: guest)
- `RABBITMQ_ENABLED` - Enable RabbitMQ (default: true)

---

## 9. Kafka

```yaml
kafka:
  enabled: ${KAFKA_ENABLED:true}
  consumer:
    group-id: ${KAFKA_CONSUMER_GROUP:portfolio-group}

spring.kafka:
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  producer:
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  consumer:
    auto-offset-reset: earliest
```

**Environment Variables**:
- `KAFKA_ENABLED` - Enable Kafka (default: true)
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka brokers (default: localhost:9092)
- `KAFKA_CONSUMER_GROUP` - Consumer group ID (default: portfolio-group)

---

## 10. Elasticsearch

```yaml
elasticsearch:
  enabled: ${ELASTICSEARCH_ENABLED:true}
  host: ${ELASTICSEARCH_HOST:localhost}
  port: ${ELASTICSEARCH_PORT:9200}
  username: ${ELASTICSEARCH_USERNAME:elastic}
  password: ${ELASTICSEARCH_PASSWORD:}
```

**Environment Variables**:
- `ELASTICSEARCH_ENABLED` - Enable Elasticsearch (default: true)
- `ELASTICSEARCH_HOST` - Elasticsearch host (default: localhost)
- `ELASTICSEARCH_PORT` - Elasticsearch port (default: 9200)
- `ELASTICSEARCH_USERNAME` - Elasticsearch username (default: elastic)
- `ELASTICSEARCH_PASSWORD` - Elasticsearch password (optional)

---

## 11. Resilience4j

```yaml
resilience4j:
  circuitbreaker:
    instances:
      emailService:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 60s
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      emailService:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
```

**Configuration**: Circuit breaker and retry patterns for email service resilience.

---

## 12. Spring Batch

```yaml
batch:
  enabled: ${BATCH_ENABLED:true}
  audit-cleanup:
    retention-days: ${BATCH_AUDIT_RETENTION_DAYS:90}
    cron: ${BATCH_AUDIT_CLEANUP_CRON:0 0 2 * * ?}  # Daily at 2 AM
  audit-report:
    output-dir: ${BATCH_AUDIT_REPORT_DIR:reports/audit}
    cron: ${BATCH_AUDIT_REPORT_CRON:0 0 3 1 * ?}  # Monthly on 1st at 3 AM
  stats-aggregation:
    cron: ${BATCH_STATS_CRON:0 0 0 * * ?}  # Daily at midnight
  sitemap:
    cron: ${BATCH_SITEMAP_CRON:0 0 4 * * ?}  # Daily at 4 AM
  image-reprocessing:
    chunk-size: ${BATCH_IMAGE_CHUNK_SIZE:10}
```

**Environment Variables**:
- `BATCH_ENABLED` - Enable batch jobs (default: true)
- `BATCH_AUDIT_RETENTION_DAYS` - Audit log retention days (default: 90)

---

## 13. GraphQL

```yaml
spring.graphql:
  graphiql:
    enabled: ${GRAPHQL_GRAPHIQL_ENABLED:true}
    path: /graphiql
  path: /graphql
  schema:
    locations: classpath:graphql/
    introspection:
      enabled: ${GRAPHQL_INTROSPECTION_ENABLED:true}
```

**Environment Variables**:
- `GRAPHQL_GRAPHIQL_ENABLED` - Enable GraphiQL UI (default: true)
- `GRAPHQL_INTROSPECTION_ENABLED` - Enable schema introspection (default: true)

---

## 14. Sitemap

```yaml
sitemap:
  base-url: ${SITEMAP_BASE_URL:https://emmanuelgabe.com}
  output-path: ${SITEMAP_OUTPUT_PATH:sitemap.xml}
```

**Environment Variables**:
- `SITEMAP_BASE_URL` - Base URL for sitemap entries
- `SITEMAP_OUTPUT_PATH` - Output file path

---

## 15. Caching

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour in milliseconds
```

**Caches**:
- `siteConfig` - Site configuration (TTL: 6 hours)
- `skills` - Skills list (TTL: 2 hours)
- `projects` - Projects list (TTL: 2 hours)
- `experiences` - Experiences list (TTL: 2 hours)
- `tags` - Tags list (TTL: 2 hours)

---

## 16. AI / Spring AI

### 16.1 Master Switch

```yaml
ai:
  enabled: ${AI_ENABLED:false}  # Global AI feature flag
```

**Environment Variables**:
- `AI_ENABLED` - Enable all AI features (default: false)
- `GOOGLE_GENAI_API_KEY` - Google Gemini API key (required when AI enabled)

### 16.2 Model Configuration

```yaml
spring.ai.google.genai:
  api-key: ${GOOGLE_GENAI_API_KEY:}
  chat:
    options:
      model: ${AI_CHAT_MODEL:gemini-2.5-flash}
      temperature: ${AI_CHAT_TEMPERATURE:0.7}
      max-output-tokens: ${AI_CHAT_MAX_TOKENS:500}
  embedding:
    text:
      options:
        model: ${AI_EMBEDDING_MODEL:gemini-embedding-001}
```

**Environment Variables**:
- `AI_CHAT_MODEL` - LLM model name (default: gemini-2.5-flash)
- `AI_CHAT_TEMPERATURE` - Response creativity (default: 0.7)
- `AI_CHAT_MAX_TOKENS` - Max response tokens (default: 500)
- `AI_EMBEDDING_MODEL` - Embedding model (default: gemini-embedding-001)

### 16.3 Chatbot

```yaml
ai:
  chat:
    rate-limit:
      max-requests-per-hour: ${AI_CHAT_RATE_LIMIT:10}
    memory:
      window-size: 20              # Messages per conversation
      idle-ttl-minutes: 120        # Conversation eviction timeout
      max-conversations: 1000     # Max concurrent conversations
      cleanup-interval-ms: 1800000 # Eviction task interval (30 min)
```

**Environment Variables**:
- `AI_CHAT_RATE_LIMIT` - Chat rate limit per IP/hour (default: 10, dev: 100)

### 16.4 Embeddings / Vector Store

```yaml
ai:
  embedding:
    index-name: ${AI_EMBEDDING_INDEX:portfolio-embeddings}
    dimensions: ${AI_EMBEDDING_DIMENSIONS:3072}

batch:
  embedding-ingestion:
    cron: ${BATCH_EMBEDDING_INGESTION_CRON:0 0 4 * * SUN}  # Weekly Sunday 4 AM
```

**Environment Variables**:
- `AI_EMBEDDING_INDEX` - Elasticsearch index name (default: portfolio-embeddings)
- `AI_EMBEDDING_DIMENSIONS` - Vector dimensions (default: 3072)
- `BATCH_EMBEDDING_INGESTION_CRON` - Embedding ingestion schedule

### 16.5 Contact Moderation

```yaml
ai:
  moderation:
    enabled: ${AI_MODERATION_ENABLED:true}
    timeout-seconds: ${AI_MODERATION_TIMEOUT:5}
```

**Environment Variables**:
- `AI_MODERATION_ENABLED` - Enable contact moderation (default: true)
- `AI_MODERATION_TIMEOUT` - Moderation AI timeout in seconds (default: 5)

### 16.6 Auto-Reply

```yaml
ai:
  autoreply:
    enabled: ${AI_AUTOREPLY_ENABLED:false}
    timeout-seconds: ${AI_AUTOREPLY_TIMEOUT:10}
    max-documents: ${AI_AUTOREPLY_MAX_DOCS:5}
```

**Environment Variables**:
- `AI_AUTOREPLY_ENABLED` - Enable auto-reply (default: false)
- `AI_AUTOREPLY_TIMEOUT` - Reply generation timeout in seconds (default: 10)
- `AI_AUTOREPLY_MAX_DOCS` - Max RAG documents retrieved (default: 5)

### 16.7 Health Monitor

```yaml
ai:
  health-monitor:
    enabled: ${AI_HEALTH_MONITOR_ENABLED:false}
    demo-mode: ${AI_HEALTH_MONITOR_DEMO_MODE:false}
    schedule-cron: ${AI_HEALTH_MONITOR_CRON:0 */15 * * * *}
    on-demand-bypass-precheck: ${AI_HEALTH_MONITOR_BYPASS_PRECHECK:true}
    action-expiration-minutes: ${AI_HEALTH_MONITOR_EXPIRATION_MINUTES:60}
    max-mutations-per-hour: ${AI_HEALTH_MONITOR_MAX_MUTATIONS:10}
    docker:
      host: ${AI_HEALTH_MONITOR_DOCKER_HOST:tcp://docker-socket-proxy:2375}
    history:
      reports-in-context: ${AI_HEALTH_MONITOR_REPORTS_CONTEXT:3}
      rejected-actions-in-context: ${AI_HEALTH_MONITOR_REJECTED_CONTEXT:5}
    logs:
      max-lines-per-call: ${AI_HEALTH_MONITOR_LOG_LINES:50}
```

**Environment Variables**:
- `AI_HEALTH_MONITOR_ENABLED` - Enable health monitor (default: false)
- `AI_HEALTH_MONITOR_DEMO_MODE` - Mock data mode (default: false)
- `AI_HEALTH_MONITOR_CRON` - Diagnostic cycle interval (default: every 15 min)
- `AI_HEALTH_MONITOR_DOCKER_HOST` - Docker proxy address (default: tcp://docker-socket-proxy:2375)
- `AI_HEALTH_MONITOR_BYPASS_PRECHECK` - Skip pre-check for manual triggers (default: true)
- `AI_HEALTH_MONITOR_EXPIRATION_MINUTES` - Pending action TTL (default: 60)
- `AI_HEALTH_MONITOR_MAX_MUTATIONS` - Max executed actions/hour (default: 10)
- `AI_HEALTH_MONITOR_REPORTS_CONTEXT` - Recent reports in LLM prompt (default: 3)
- `AI_HEALTH_MONITOR_REJECTED_CONTEXT` - Rejected actions in LLM prompt (default: 5)
- `AI_HEALTH_MONITOR_LOG_LINES` - Max log lines per tool call (default: 50)

---

## 17. Contact Retention

```yaml
contact:
  retention:
    months: ${CONTACT_RETENTION_MONTHS:12}
    cron: "0 30 3 * * ?"  # Daily at 3:30 AM
```

**Environment Variables**:
- `CONTACT_RETENTION_MONTHS` - GDPR retention period in months (default: 12)

---

## Related Documentation

- [Development: Setup](../development/setup.md) - Environment setup
- [Reference: Environments](./environments.md) - Environment details
- [Security: JWT Implementation](../security/jwt-implementation.md) - JWT configuration
- [Security: Rate Limiting](../security/rate-limiting.md) - Rate limit configuration
- [Features: Messaging](../features/messaging.md) - RabbitMQ usage
- [Features: Event Streaming](../features/event-streaming.md) - Kafka usage
- [Features: Search](../features/search.md) - Elasticsearch usage
- [Features: Batch Processing](../features/batch-processing.md) - Batch jobs
- [Features: AI Chatbot](../features/ai-chatbot.md) - Chatbot configuration
- [Features: Contact Moderation](../features/contact-moderation.md) - Moderation configuration
- [Features: Health Monitor](../features/health-monitor.md) - Health monitor configuration
