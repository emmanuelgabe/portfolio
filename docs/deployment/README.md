# Deployment Documentation

Guides for CI/CD pipelines and deployment processes.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Contents](#2-contents)
3. [Deployment Environments](#3-deployment-environments)
4. [Quick Commands](#4-quick-commands)
5. [Deployment Workflow](#5-deployment-workflow)

---

## 1. Overview

This section documents deployment processes for the Portfolio application including CI/CD pipelines, Docker deployment, and environment-specific configurations.

**Deployment Stack**:
- GitHub Actions for CI/CD
- Docker and Docker Compose for containerization
- Nginx as reverse proxy
- PostgreSQL, Redis, RabbitMQ, Elasticsearch as infrastructure services

---

## 2. Contents

| Document | Description |
|----------|-------------|
| [CI/CD Guide](./ci-cd.md) | GitHub Actions workflows, pipeline stages, deployment process |
| [Local Testing](./local-testing.md) | Local deployment testing with Docker Compose |

---

## 3. Deployment Environments

### 3.1 Environment Overview

| Environment | Branch | Trigger | Purpose |
|-------------|--------|---------|---------|
| Local | Any | Manual | Development and testing |
| Staging | `staging` | Push to staging | Pre-production validation |
| Production | `main` | Push to main | Live application |

### 3.2 Environment URLs

| Environment | Backend | Frontend |
|-------------|---------|----------|
| Local | http://localhost:8080 | http://localhost:4200 |
| Staging | https://staging-api.example.com | https://staging.example.com |
| Production | https://api.example.com | https://example.com |

### 3.3 Docker Compose Files

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Base configuration |
| `docker-compose.local.yml` | Local development overrides |
| `docker-compose.staging.yml` | Staging environment |
| `docker-compose.prod.yml` | Production environment |
| `docker-compose.monitoring.yml` | Observability stack |

---

## 4. Quick Commands

### 4.1 Local Deployment

```bash
# Start all services
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build -d

# Start with monitoring stack
docker-compose -f docker-compose.yml -f docker-compose.local.yml \
  -f docker-compose.monitoring.yml up --build -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Clean up volumes
docker-compose down -v
```

### 4.2 Staging Deployment

```bash
# Build and deploy staging
docker-compose -f docker-compose.yml -f docker-compose.staging.yml up --build -d

# View staging logs
docker-compose -f docker-compose.yml -f docker-compose.staging.yml logs -f
```

### 4.3 Production Deployment

```bash
# Build production images
docker-compose -f docker-compose.yml -f docker-compose.prod.yml build

# Deploy production
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Health check
curl https://api.example.com/actuator/health
```

---

## 5. Deployment Workflow

### 5.1 CI/CD Pipeline Stages

```
1. Build
   - Compile backend (Gradle)
   - Build frontend (npm)
   - Run Checkstyle and SpotBugs

2. Test
   - Backend unit tests
   - Frontend unit tests
   - Code coverage verification

3. Docker Build
   - Build backend image
   - Build frontend image
   - Push to registry

4. Deploy
   - Pull images on target server
   - Run docker-compose
   - Health check verification
```

### 5.2 Deployment Verification

After deployment, verify:
- Health endpoints respond with `UP`
- Frontend loads correctly
- API endpoints return expected data
- Monitoring dashboards show metrics

---

## Related Documentation

- [Development Guide](../development/README.md) - Development setup
- [Reference: Environments](../reference/environments.md) - Environment configuration
- [Operations](../operations/README.md) - Health checks and monitoring
- [Operations: Observability](../operations/observability.md) - Monitoring dashboards
- [Reference: Scripts Organization](../reference/scripts-organization.md) - Deployment scripts
