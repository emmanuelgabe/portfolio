# Infrastructure Credentials

Configuration for infrastructure services: Redis, RabbitMQ, Elasticsearch, Sentry, and Grafana.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Redis Configuration](#2-redis-configuration)
3. [RabbitMQ Configuration](#3-rabbitmq-configuration)
4. [Elasticsearch Configuration](#4-elasticsearch-configuration)
5. [Observability Configuration](#5-observability-configuration)
6. [Environment Variables Summary](#6-environment-variables-summary)
7. [Docker Compose Configuration](#7-docker-compose-configuration)

---

## 1. Overview

Infrastructure credentials for services used by the Portfolio application.

### 1.1 Requirements by Environment

| Variable | Dev (local) | Staging | Production |
|----------|-------------|---------|------------|
| `REDIS_PASSWORD` | Optional | Required | Required |
| `RABBITMQ_USERNAME` | Required | Required | Required |
| `RABBITMQ_PASSWORD` | Required | Required | Required |
| `ELASTICSEARCH_PASSWORD` | Optional | Optional | Required |
| `SENTRY_DSN` | Optional | Optional | Recommended |
| `SENTRY_DSN_FRONTEND` | Optional | Optional | Recommended |
| `GRAFANA_ADMIN_PASSWORD` | N/A | Optional | Required |

For core security (Admin, JWT, Database, Mail), see [Initial Setup](./initial-setup.md).

### 1.2 What Happens If Missing?

| Variable | Dev | Staging/Prod |
|----------|-----|--------------|
| `REDIS_PASSWORD` | Redis starts without auth | Container fails to start |
| `RABBITMQ_USERNAME/PASSWORD` | Uses defaults | Container fails to start |
| `ELASTICSEARCH_PASSWORD` | Uses "changeme" | Should be configured |
| `SENTRY_DSN` | Error tracking disabled | Error tracking disabled |
| `GRAFANA_ADMIN_PASSWORD` | N/A | Monitoring inaccessible |

---

## 2. Redis Configuration

### 2.1 Overview

Redis is used for caching and rate limiting.

**Required Variable** (staging/prod):
- `REDIS_PASSWORD`: Redis authentication password

### 2.2 Generate Password

```bash
openssl rand -base64 32
```

### 2.3 Set Environment Variable

**CI/CD** (GitHub Actions):
- Add `REDIS_PASSWORD` as a repository secret

**Docker Compose** (staging/prod):
```yaml
redis-cache:
  command: redis-server --requirepass ${REDIS_PASSWORD:?REDIS_PASSWORD required}
```

---

## 3. RabbitMQ Configuration

### 3.1 Overview

RabbitMQ is used for asynchronous message processing:
- Email sending
- Image processing
- Audit logging

**Required Variables**:
- `RABBITMQ_USERNAME`: RabbitMQ admin username
- `RABBITMQ_PASSWORD`: RabbitMQ admin password

### 3.2 Generate Credentials

**Username**: Use a descriptive name like `portfolio_prod` or `portfolio_staging`

**Password**:
```bash
openssl rand -base64 32
```

### 3.3 Set Environment Variables

**CI/CD** (GitHub Actions):
- Add `RABBITMQ_USERNAME` as a repository secret
- Add `RABBITMQ_PASSWORD` as a repository secret

**Docker Compose**:
```yaml
rabbitmq:
  environment:
    RABBITMQ_DEFAULT_USER: ${RABBITMQ_USERNAME:?RABBITMQ_USERNAME required}
    RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:?RABBITMQ_PASSWORD required}
```

---

## 4. Elasticsearch Configuration

### 4.1 Overview

Elasticsearch is used for full-text search on articles and projects.

**Required Variable** (prod):
- `ELASTICSEARCH_PASSWORD`: Password for the `elastic` user

### 4.2 Generate Password

```bash
openssl rand -base64 32
```

### 4.3 Set Environment Variable

**CI/CD** (GitHub Actions):
- Add `ELASTICSEARCH_PASSWORD` as a repository secret

**Docker Compose**:
```yaml
elasticsearch:
  environment:
    - ELASTIC_PASSWORD=${ELASTICSEARCH_PASSWORD:-changeme}
```

---

## 5. Observability Configuration

### 5.1 Sentry Error Tracking

Sentry is used for error tracking in backend and frontend.

**Required Variables**:
- `SENTRY_DSN`: Backend Sentry DSN
- `SENTRY_DSN_FRONTEND`: Frontend Sentry DSN

**Get DSN from Sentry**:
1. Create a project at https://sentry.io
2. Go to Settings > Projects > [Project] > Client Keys (DSN)
3. Copy the DSN URL

**CI/CD** (GitHub Actions):
- Add `SENTRY_DSN` as a repository secret
- Add `SENTRY_DSN_FRONTEND` as a repository secret

### 5.2 Grafana Monitoring

Grafana is used for monitoring dashboards.

**Required Variable** (prod):
- `GRAFANA_ADMIN_PASSWORD`: Admin password for Grafana UI

**Generate Password**:
```bash
openssl rand -base64 24
```

**CI/CD** (GitHub Actions):
- Add `GRAFANA_ADMIN_PASSWORD` as a repository secret

---

## 6. Environment Variables Summary

### 6.1 All Infrastructure Variables

| Variable | Description | Required In |
|----------|-------------|-------------|
| `REDIS_PASSWORD` | Redis authentication | Staging/Prod |
| `RABBITMQ_USERNAME` | RabbitMQ username | All |
| `RABBITMQ_PASSWORD` | RabbitMQ password | All |
| `ELASTICSEARCH_PASSWORD` | Elasticsearch password | Prod |
| `SENTRY_DSN` | Backend Sentry DSN | Recommended |
| `SENTRY_DSN_FRONTEND` | Frontend Sentry DSN | Recommended |
| `GRAFANA_ADMIN_PASSWORD` | Grafana admin password | Prod |
| `DEPLOY_BASE_PATH` | CI/CD deployment path | CI/CD only |

### 6.2 Validation Checklist

**For Development (local):**
- [ ] `RABBITMQ_USERNAME` and `RABBITMQ_PASSWORD` are set
- [ ] (Optional) `ELASTICSEARCH_PASSWORD` for search

**For Staging/Production:**
- [ ] `REDIS_PASSWORD` is set with secure password
- [ ] `RABBITMQ_USERNAME` and `RABBITMQ_PASSWORD` are set
- [ ] `ELASTICSEARCH_PASSWORD` is set (prod)
- [ ] `SENTRY_DSN` and `SENTRY_DSN_FRONTEND` are configured
- [ ] `GRAFANA_ADMIN_PASSWORD` is set
- [ ] CI/CD secrets are configured in GitHub Actions

---

## 7. Docker Compose Configuration

### 7.1 docker-compose.yml (base)

```yaml
services:
  spring-backend:
    environment:
      - RABBITMQ_USERNAME=${RABBITMQ_USERNAME}
      - RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
      - ELASTICSEARCH_PASSWORD=${ELASTICSEARCH_PASSWORD:-changeme}
      - SENTRY_DSN=${SENTRY_DSN:-}

  rabbitmq:
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USERNAME}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}

  elasticsearch:
    environment:
      - ELASTIC_PASSWORD=${ELASTICSEARCH_PASSWORD:-changeme}
```

### 7.2 docker-compose.prod.yml

```yaml
services:
  spring-backend:
    environment:
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - RABBITMQ_USERNAME=${RABBITMQ_USERNAME:?RABBITMQ_USERNAME required}
      - RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:?RABBITMQ_PASSWORD required}

  redis-cache:
    command: redis-server --requirepass ${REDIS_PASSWORD:?REDIS_PASSWORD required}

  rabbitmq:
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USERNAME:?RABBITMQ_USERNAME required}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:?RABBITMQ_PASSWORD required}
```

### 7.3 .env File Template

**For Development (local):**
```bash
RABBITMQ_USERNAME=portfolio_dev
RABBITMQ_PASSWORD=local_rabbitmq_password
ELASTICSEARCH_PASSWORD=changeme
```

**For Staging/Production:**
```bash
REDIS_PASSWORD=secure_redis_password
RABBITMQ_USERNAME=portfolio_prod
RABBITMQ_PASSWORD=secure_rabbitmq_password
ELASTICSEARCH_PASSWORD=secure_elasticsearch_password
SENTRY_DSN=https://xxx@sentry.io/xxx
SENTRY_DSN_FRONTEND=https://xxx@sentry.io/xxx
GRAFANA_ADMIN_PASSWORD=secure_grafana_password
```

---

## Related Documentation

- [Initial Setup](./initial-setup.md) - Core security (Admin, JWT, Database, Mail)
- [Messaging](../features/messaging.md) - RabbitMQ usage
- [Search](../features/search.md) - Elasticsearch usage
- [Observability](../operations/observability.md) - Grafana dashboards
- [Configuration Properties](../reference/configuration-properties.md) - All configuration options
