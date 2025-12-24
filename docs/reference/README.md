# Reference Documentation

Technical reference documentation for configuration, dependencies, environments, and error codes.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Contents](#2-contents)
3. [Quick Reference](#3-quick-reference)
4. [Environment Summary](#4-environment-summary)

---

## 1. Overview

This section contains technical reference documentation for the Portfolio application. Use these documents for configuration, dependency management, environment setup, and error handling reference.

**Reference Categories**:
- Configuration properties (JWT, rate limiting, storage, database)
- Environment-specific settings (local, staging, production)
- Dependency versions and management
- Error codes and messages
- Scripts organization

---

## 2. Contents

| Document | Description |
|----------|-------------|
| [Configuration Properties](./configuration-properties.md) | JWT, rate limiting, file storage, database, Redis, email settings |
| [Environments](./environments.md) | URLs, ports, Docker Compose commands by environment |
| [Dependencies](./dependencies.md) | Tech stack versions and dependency management |
| [Error Codes](./error-codes.md) | API error codes and messages reference |
| [Versioning](./versioning.md) | Semantic versioning strategy |
| [Scripts Organization](./scripts-organization.md) | Scripts structure and usage |

---

## 3. Quick Reference

### 3.1 Key Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `jwt.expiration` | Access token TTL | 15 minutes |
| `jwt.refresh-expiration` | Refresh token TTL | 7 days |
| `rate-limit.contact` | Contact form limit | 3/hour |
| `rate-limit.login` | Login attempt limit | 5/15 minutes |
| `file.max-size` | Max upload size | 10 MB |
| `file.upload-dir` | Upload directory | `./uploads` |

### 3.2 Port Reference

| Service | Port | Description |
|---------|------|-------------|
| Backend API | 8080 | Spring Boot REST/GraphQL API |
| Frontend | 4200 | Angular development server |
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Cache and rate limiting |
| RabbitMQ | 5672 | Message queue |
| RabbitMQ UI | 15672 | Management interface |
| Kafka | 9092 | Event streaming |
| Elasticsearch | 9200 | Full-text search |
| Prometheus | 9090 | Metrics |
| Grafana | 3001 | Dashboards |
| Loki | 3100 | Logs |

### 3.3 Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `RESOURCE_NOT_FOUND` | 404 | Entity not found |
| `VALIDATION_ERROR` | 400 | Input validation failed |
| `UNAUTHORIZED` | 401 | Missing or invalid token |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `RATE_LIMIT_EXCEEDED` | 429 | Rate limit reached |
| `DUPLICATE_RESOURCE` | 409 | Unique constraint violation |

---

## 4. Environment Summary

### 4.1 URLs by Environment

| Environment | Backend URL | Frontend URL |
|-------------|-------------|--------------|
| Local | http://localhost:8080 | http://localhost:4200 |
| Staging | https://staging-api.example.com | https://staging.example.com |
| Production | https://api.example.com | https://example.com |

### 4.2 Docker Compose Files

| Environment | Command |
|-------------|---------|
| Local | `docker-compose -f docker-compose.yml -f docker-compose.local.yml up` |
| Staging | `docker-compose -f docker-compose.yml -f docker-compose.staging.yml up` |
| Production | `docker-compose -f docker-compose.yml -f docker-compose.prod.yml up` |

### 4.3 Profile Activation

```yaml
# Local
spring.profiles.active: local

# Staging
spring.profiles.active: staging

# Production
spring.profiles.active: prod
```

---

## Related Documentation

- [Development Guide](../development/README.md) - Development setup
- [Deployment](../deployment/README.md) - CI/CD and deployment
- [Security](../security/README.md) - Security configuration
- [Features](../features/README.md) - Feature documentation
- [Operations: Observability](../operations/observability.md) - Monitoring stack
