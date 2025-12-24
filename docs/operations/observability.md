# Observability Stack

## Overview

- Prometheus for metrics collection and alerting
- Loki for centralized log aggregation
- Grafana for unified visualization
- Sentry for error tracking (frontend and backend)
- Web Vitals for frontend performance metrics

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        GRAFANA (:3001)                          │
│              Unified dashboard (metrics + logs)                 │
├──────────────┬──────────────┬──────────────┬────────────────────┤
│  Prometheus  │     Loki     │    Sentry    │   Exporters        │
│   (:9090)    │   (:3100)    │  (sentry.io) │                    │
├──────────────┼──────────────┼──────────────┼────────────────────┤
│  Micrometer  │   Promtail   │  Sentry SDK  │ - RabbitMQ Exporter│
│  + Web Vitals│  (collector) │  (FE + BE)   │ - Kafka Exporter   │
│              │              │              │ - PG Exporter      │
├──────────────┴──────────────┴──────────────┴────────────────────┤
│          Backend (Spring Boot) + Frontend (Angular)             │
├─────────────────────────────────────────────────────────────────┤
│  RabbitMQ (:5672)  │  Kafka (:9092)  │  Elasticsearch (:9200)  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Components

### Prometheus

Metrics collection and storage with 15-day retention.

**Endpoints scraped:**
- `spring-backend:8080/actuator/prometheus` (10s interval)

**Alert rules:**
- ServiceDown: Backend unavailable > 1 minute
- HighErrorRate: Error rate > 10% for 5 minutes
- HighResponseTime: p95 latency > 2 seconds
- HighMemoryUsage: Heap > 90%
- HighAuthFailureRate: Auth failures > 50%
- RabbitMQDLQMessages: Dead letter queue messages > 0
- RabbitMQNoConsumers: Queue has no consumers
- KafkaConsumerLag: Consumer lag > 1000
- CircuitBreakerOpen: Circuit breaker in OPEN state
- BatchJobFailed: Batch job execution failed

### Loki

Centralized log aggregation with 7-day retention.

**Log sources:**
- Docker container logs via Promtail
- Spring Boot application logs (JSON format)

### Grafana

Visualization platform with 11 pre-provisioned dashboards covering JVM, API, business metrics, messaging, database, and security.

### RabbitMQ Exporter

Metrics from rabbitmq_prometheus plugin (port 15692).

**Metrics:** Queue depth, message rates, consumer count, unacked messages, DLQ messages

### Kafka Exporter

Metrics via kafka-exporter (port 9308).

**Metrics:** Consumer group lag, partition offsets, topic message rate

### PostgreSQL Exporter

Metrics via postgres_exporter (port 9187).

**Metrics:** Connection count, transaction rate, cache hit ratio, table sizes

### Sentry

Error tracking and performance monitoring.

**Backend:**
- Automatic exception capture via `sentry-spring-boot-starter-jakarta`
- Integration with Logback for error-level logs
- Trace sampling: 10% (staging), 5% (production)

**Frontend:**
- Global error handler via `@sentry/angular`
- Browser tracing integration
- Web Vitals reporting (LCP, INP, CLS, FCP, TTFB)

---

## Configuration

### Backend Metrics

**File:** `portfolio-backend/src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.75, 0.95, 0.99
```

### Backend Sentry

**File:** `portfolio-backend/src/main/resources/application.yml`

```yaml
sentry:
  dsn: ${SENTRY_DSN:}
  environment: ${SPRING_PROFILES_ACTIVE:dev}
  traces-sample-rate: ${SENTRY_TRACES_SAMPLE_RATE:0.1}
  send-default-pii: false
  logging:
    minimum-event-level: error
    minimum-breadcrumb-level: info
```

### Frontend Sentry

**File:** `portfolio-frontend/src/environments/environment.prod.ts`

```typescript
sentry: {
  dsn: '',  // Set via build or replace placeholder
  enabled: true,
  tracesSampleRate: 0.05,
},
webVitals: {
  enabled: true,
},
```

---

## Custom Metrics

### BusinessMetrics Component

**File:** `portfolio-backend/src/main/java/com/emmanuelgabe/portfolio/metrics/BusinessMetrics.java`

**Counters:**
- `portfolio.contact.submissions` - Contact form submissions
- `portfolio.contact.failures` - Contact form failures
- `portfolio.auth.attempts` - Authentication attempts
- `portfolio.auth.failures` - Authentication failures
- `portfolio.ratelimit.hits` - Rate limit hits

**Timers:**
- `portfolio.image.processing` - Image processing duration
- `portfolio.email.sending` - Email sending duration

---

## Deployment

### Start Monitoring Stack

```bash
# Staging
docker-compose -f docker-compose.yml \
  -f docker-compose.staging.yml \
  -f docker-compose.monitoring.yml up -d

# Production
docker-compose -f docker-compose.yml \
  -f docker-compose.prod.yml \
  -f docker-compose.monitoring.yml up -d
```

### Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3001 | GRAFANA_ADMIN_USER / GRAFANA_ADMIN_PASSWORD |
| Prometheus | http://localhost:9090 | N/A |
| Loki | http://localhost:3100 | N/A |

---

## Environment Variables

### GitHub Secrets (CI/CD)

Configure these secrets in GitHub repository settings for CI/CD deployment:

| Secret | Description | Required |
|--------|-------------|----------|
| `SENTRY_DSN` | Backend Sentry DSN (Spring Boot project) | Yes |
| `SENTRY_DSN_FRONTEND` | Frontend Sentry DSN (Angular project) | Yes |
| `GRAFANA_ADMIN_PASSWORD` | Grafana admin password | Yes |

### Local Development

```bash
# Sentry
SENTRY_DSN=https://xxx@sentry.io/xxx
SENTRY_DSN_FRONTEND=https://xxx@sentry.io/xxx

# Grafana
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=<secure-password>
```

### Optional (Email Alerting)

```bash
GF_SMTP_ENABLED=true
GF_SMTP_HOST=smtp.gmail.com:587
GF_SMTP_USER=<email>
GF_SMTP_PASSWORD=<app-password>
GF_SMTP_FROM_ADDRESS=grafana@localhost
```

---

## Resource Requirements

| Service | Memory Limit | CPU Limit |
|---------|--------------|-----------|
| Prometheus | 512MB | 0.5 |
| Loki | 512MB | 0.5 |
| Promtail | 256MB | 0.25 |
| Grafana | 256MB | 0.25 |
| **Total** | **~1.5GB** | **~1.5 CPU** |

---

## Grafana Dashboards

11 pre-provisioned dashboards available.

### Executive Overview Dashboard

High-level system health and SLA monitoring.

**Panels:** Uptime %, Apdex score, traffic overview, services status, active alerts

### JVM Metrics Dashboard

**Panels:** JVM Heap Memory, Heap Usage %, CPU Usage %, JVM Threads, GC Pause Duration

### API Performance Dashboard

**Panels:** Request Rate, Response Time Percentiles (p50, p95, p99), Error Rate %, Status Code Distribution, Top Endpoints

### Business Metrics Dashboard

**Panels:** Contact Submissions, Auth Attempts/Failures, Rate Limit Hits, Contact Form Activity, Authentication Activity

### PostgreSQL Dashboard

Database performance and health monitoring.

**Panels:** Connections pool, active connections, slow queries, cache hit ratio, transactions/sec, table sizes

### RabbitMQ Dashboard

Message queue monitoring with rabbitmq_prometheus plugin.

**Panels:** Queue depth, message rates, consumer count, unacked messages, DLQ monitoring, connection status

### Kafka Dashboard

Event streaming monitoring with kafka-exporter.

**Panels:** Consumer lag, partition distribution, message rate, topic health, producer/consumer metrics

### Circuit Breaker Dashboard

Resilience4j metrics visualization.

**Panels:** Circuit state (CLOSED/OPEN/HALF-OPEN), failure rate, call count, slow call rate, state transitions

### Spring Batch Dashboard

Scheduled job monitoring.

**Panels:** Job execution status, success/failure counts, duration, items processed, execution history

### Security/Auth Dashboard

Authentication and security monitoring.

**Panels:** Login attempts, failed logins, rate limiting triggers, JWT tokens issued, blocked IPs

### Logs Explorer Dashboard

Loki log visualization and search.

**Panels:** Log volume, errors by service, log level distribution, request traces

---

## Web Vitals

### Metrics Collected

| Metric | Description | Good Threshold |
|--------|-------------|----------------|
| LCP | Largest Contentful Paint | < 2.5s |
| INP | Interaction to Next Paint | < 200ms |
| CLS | Cumulative Layout Shift | < 0.1 |
| FCP | First Contentful Paint | < 1.8s |
| TTFB | Time to First Byte | < 800ms |

### Reporting

Web Vitals are reported to:
- Browser console (debug level)
- Sentry measurements (when enabled)
- Poor scores trigger Sentry breadcrumbs (warning level)

---

## Files Reference

### Backend
- `portfolio-backend/build.gradle` - Micrometer and Sentry dependencies
- `portfolio-backend/src/main/resources/application*.yml` - Metrics and Sentry config
- `portfolio-backend/src/main/java/.../metrics/BusinessMetrics.java` - Custom metrics

### Frontend
- `portfolio-frontend/package.json` - @sentry/angular, web-vitals
- `portfolio-frontend/src/environments/` - Sentry and Web Vitals config
- `portfolio-frontend/src/main.ts` - Sentry initialization
- `portfolio-frontend/src/app/services/logger.service.ts` - Sentry integration
- `portfolio-frontend/src/app/services/web-vitals.service.ts` - Web Vitals collection

### Infrastructure
- `docker-compose.monitoring.yml` - Monitoring stack
- `monitoring/prometheus/prometheus.yml` - Prometheus config
- `monitoring/prometheus/alert-rules.yml` - Alert rules
- `monitoring/loki/loki-config.yml` - Loki config
- `monitoring/promtail/promtail-config.yml` - Promtail config
- `monitoring/grafana/provisioning/dashboards/` - 11 pre-provisioned dashboards:
  - `executive-overview-dashboard.json`
  - `jvm-dashboard.json`
  - `api-dashboard.json`
  - `business-dashboard.json`
  - `postgresql-dashboard.json`
  - `rabbitmq-dashboard.json`
  - `kafka-dashboard.json`
  - `circuit-breaker-dashboard.json`
  - `spring-batch-dashboard.json`
  - `security-auth-dashboard.json`
  - `logs-explorer-dashboard.json`

---

## Related Documentation

- [Health Checks](health-checks.md)
- [Logging Conventions](../development/logging-conventions.md)
- [Environments](../reference/environments.md)
- [Messaging (RabbitMQ)](../features/messaging.md)
- [Event Streaming (Kafka)](../features/event-streaming.md)
- [Circuit Breaker](../features/circuit-breaker.md)
- [Batch Processing](../features/batch-processing.md)
