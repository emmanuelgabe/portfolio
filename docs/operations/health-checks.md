# Health Check Operations Guide

**Document Type:** Operational Guide
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active

---

## Table of Contents

1. [Overview](#1-overview)
2. [Health Check Endpoints](#2-health-check-endpoints)
3. [Testing Procedures](#3-testing-procedures)
4. [Docker Health Checks](#4-docker-health-checks)
5. [CI/CD Integration](#5-cicd-integration)
6. [System Architecture](#6-system-architecture)
7. [Pre-Deployment Checklist](#7-pre-deployment-checklist)

---

## 1. Overview

Health check infrastructure verifies system components are operational and can communicate with each other.

### 1.1 Health Check Layers

1. **Nginx Layer:** Availability checks
2. **Frontend Layer:** Angular application status
3. **Backend Layer:** Spring Boot API status
4. **Database Layer:** PostgreSQL connectivity
5. **Integration Layer:** End-to-end verification

### 1.2 Health Check Types

| Type | Purpose | Frequency |
|------|---------|-----------|
| Liveness | Service running | Every 30 seconds |
| Readiness | Service can accept traffic | Every 30 seconds |
| Startup | Service initialization | On container start |

---

## 2. Health Check Endpoints

### 2.1 Production Endpoints

**Base URL:** `http://localhost:80`

| Endpoint | Description | Expected Status |
|----------|-------------|-----------------|
| `/health` | Nginx simple check | 200 OK |
| `/health/full` | Complete system check | 200 OK (JSON) |
| `/health/frontend` | Frontend check | 200 OK (JSON) |
| `/health/backend` | Backend check | 200 OK (JSON) |

### 2.2 Backend Direct Access

**Base URL:** `http://localhost:8080`

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Spring Boot Actuator |
| `/api/health/ping` | Simple API ping |
| `/api/health/db` | Database connectivity |
| `/api/health/status` | Complete status (API + DB) |

### 2.3 Frontend Direct Access

**Base URL:** `http://localhost:4200`

| Endpoint | Description |
|----------|-------------|
| `/health.json` | Static health file |

### 2.4 Environment-Specific Ports

| Environment | Port |
|-------------|------|
| Production | 80 |
| Staging | 3000 |
| Development | 8081 |

---

## 3. Testing Procedures

### 3.1 Command-Line Testing

```bash
# Basic health check
curl http://localhost:8081/health

# Complete health check with formatted output
curl -s http://localhost:8081/health/full | jq

# Component-specific checks
curl http://localhost:8081/health/frontend
curl http://localhost:8081/health/backend
curl http://localhost:8080/api/health/db
```

### 3.2 Automated Script Testing

```bash
# Test local environment
./scripts/test-health.sh local

# Test staging environment
./scripts/test-health.sh staging

# Test production environment
./scripts/test-health.sh prod
```

**Expected Response:**

```json
{
  "status": "healthy",
  "checks": {
    "api": {
      "status": "ok",
      "message": "API is responding"
    },
    "database": {
      "status": "ok",
      "message": "Database connection is healthy",
      "type": "PostgreSQL"
    }
  },
  "timestamp": 1699012345678
}
```

---

## 4. Docker Health Checks

### 4.1 Container Status Verification

```bash
docker-compose -f docker-compose.yml -f docker-compose.local.yml ps
```

**Expected Output:**

```
NAME                       STATUS
portfolio-frontend-local   Up (healthy)
portfolio-backend-local    Up (healthy)
portfolio-db-local         Up (healthy)
portfolio-nginx-local      Up
```

### 4.2 Health Check Configuration

**Frontend:**
```yaml
healthcheck:
  test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:4200/health.json"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

**Backend:**
```yaml
healthcheck:
  test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

**Database:**
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres_app -d portfolio_local"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 40s
```

### 4.3 Health Check Parameters

| Parameter | Description | Typical Value |
|-----------|-------------|---------------|
| `interval` | Time between health checks | 30 seconds |
| `timeout` | Maximum wait time for response | 10 seconds |
| `retries` | Consecutive failures before unhealthy | 3 |
| `start_period` | Grace period before first check | 60 seconds |

---

## 5. CI/CD Integration

### 5.1 GitHub Actions Workflow

**Workflow file:** `.github/workflows/health-check.yml`

**Steps:**
1. Build Docker containers
2. Start all services
3. Wait for containers to reach healthy state
4. Execute health check script
5. Verify individual endpoints
6. Display logs on failure

### 5.2 Manual Workflow Trigger

Navigate to GitHub Actions and trigger manually:

```
Repository → Actions → Health Check CI → Run workflow
```

---

## 6. System Architecture

### 6.1 Health Check Flow

```
USER
  │
  │ http://localhost:8081
  ▼
NGINX (Port 8081)
  • /health          → Simple check
  • /health/full     → Proxy to backend
  • /health/frontend → Proxy to frontend:4200
  • /health/backend  → Proxy to backend:8080
  │
  ├─→ Frontend (Port 4200)
  │   /health.json
  │
  └─→ Backend (Port 8080)
      /actuator/health
      /api/health/ping
      /api/health/db
      /api/health/status
      │
      └─→ PostgreSQL (Port 5432)
          pg_isready
```

---

## 7. Pre-Deployment Checklist

- [ ] All Docker containers show status `(healthy)`
- [ ] Script `./test-health.sh local` executes successfully
- [ ] Endpoint `/health/full` returns status `healthy`
- [ ] GitHub Actions workflow completes without errors
- [ ] No critical errors in application logs

---

## Change History

| Version | Date       | Changes |
|---------|------------|---------|
| 1.0.0   | 2025-11-09 | Initial release |

---

**Document Type:** Operational Guide
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active
