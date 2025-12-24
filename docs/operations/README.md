# Operations Documentation

Documentation for monitoring, health checks, and operational tasks.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Health Endpoints](#2-health-endpoints)
3. [Observability Stack](#3-observability-stack)
4. [Operational Tasks](#4-operational-tasks)

---

## 1. Overview

This section documents operational aspects of the Portfolio application including health monitoring, observability, and routine maintenance tasks.

**Components**:
- Spring Boot Actuator for health endpoints
- Grafana dashboards for visualization
- Prometheus for metrics collection
- Loki for log aggregation
- Sentry for error tracking

---

## Contents

| Document | Description |
|----------|-------------|
| [Health Checks](./health-checks.md) | Application health endpoints, monitoring configuration |
| [Observability](./observability.md) | Monitoring stack (Grafana, Prometheus, Loki), dashboards, alerting |

---

## 2. Health Endpoints

### 2.1 Available Endpoints

| Endpoint | Description | Auth Required |
|----------|-------------|---------------|
| `GET /actuator/health` | Basic health status | No |
| `GET /actuator/health/liveness` | Kubernetes liveness probe | No |
| `GET /actuator/health/readiness` | Kubernetes readiness probe | No |
| `GET /api/health/ping` | Simple ping endpoint | No |
| `GET /api/health/full` | Full stack health check | No |

### 2.2 Health Check Response

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "elasticsearch": { "status": "UP" },
    "rabbitMQ": { "status": "UP" }
  }
}
```

### 2.3 Quick Health Check

```bash
# Basic health check
curl http://localhost:8080/actuator/health

# Full stack health
curl http://localhost:8080/api/health/full

# Specific component (liveness)
curl http://localhost:8080/actuator/health/liveness
```

---

## 3. Observability Stack

### 3.1 Components

| Component | Port | Purpose |
|-----------|------|---------|
| Prometheus | 9090 | Metrics collection and alerting |
| Grafana | 3001 | Dashboards and visualization |
| Loki | 3100 | Log aggregation |
| Promtail | - | Log collector |
| Sentry | Cloud | Error tracking |

### 3.2 Available Dashboards

| Dashboard | Purpose |
|-----------|---------|
| Executive Overview | SLA uptime, Apdex score, traffic overview |
| Spring Boot Application | JVM metrics, HTTP stats, thread pools |
| PostgreSQL Database | Connections, queries, cache hit ratio |
| RabbitMQ | Queues, messages, consumers |
| Kafka | Topics, partitions, consumer lag |
| Circuit Breaker | Resilience4j states and metrics |
| Spring Batch | Job executions, duration, history |
| Security & Auth | Login attempts, rate limiting |
| Logs Explorer | Loki log queries |

### 3.3 Accessing Dashboards

- **Grafana**: http://localhost:3001 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Alertmanager**: http://localhost:9093

---

## 4. Operational Tasks

### 4.1 Log Analysis

```bash
# View backend logs
docker logs portfolio-backend -f

# Search specific logs
docker logs portfolio-backend 2>&1 | grep "[ERROR]"
```

### 4.2 Database Operations

```bash
# Connect to database
docker exec -it portfolio-db psql -U portfolio -d portfolio

# View active connections
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';
```

### 4.3 Cache Operations

```bash
# Connect to Redis
docker exec -it portfolio-redis redis-cli

# View rate limit keys
KEYS rate_limit:*

# Clear specific cache
DEL "cache:siteConfig"
```

---

## Related Documentation

- [Health Checks](./health-checks.md) - Detailed health endpoint documentation
- [Observability](./observability.md) - Full observability stack documentation
- [Deployment](../deployment/README.md) - CI/CD and deployment
- [Reference: Environments](../reference/environments.md) - URLs and ports by environment
- [Features: Caching](../features/caching.md) - Redis caching implementation
