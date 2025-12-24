# Local Testing Guide

---

## Table of Contents

1. [Overview](#1-overview)
2. [Prerequisites](#2-prerequisites)
3. [Quick Start](#3-quick-start)
4. [Testing Procedures](#4-testing-procedures)
5. [Access URLs](#5-access-urls)
6. [Development Workflow](#6-development-workflow)
7. [Performance Testing](#7-performance-testing)
8. [Next Steps](#8-next-steps)

---

## 1. Overview

Guide for testing the portfolio application locally before deployment. This ensures that all components work correctly in isolation and integration.

### 1.1 Test Environments

| Environment | Purpose | Port | Database |
|-------------|---------|------|----------|
| Local | Development testing | 8081 | portfolio_local (5435) |
| Staging | Pre-production testing | 3000 | portfolio_staging (5434) |
| Production | Production testing | 80 | portfolio_prod (5432) |

---

## 2. Prerequisites

### 2.1 Required Software

- Docker 24.0+ and Docker Compose 2.20+
- Git 2.40+
- Make (optional but recommended)

### 2.2 Environment Variables

Create a `.env` file in the project root:

```bash
DB_USER_PASSWORD=your_secure_password
```

---

## 3. Quick Start

### 3.1 Using Make (Recommended)

```bash
# Validate and test local environment
make validate-local

# Start local environment
make test-local

# View logs
make logs-local

# Clean up
make clean-local
```

### 3.2 Using Docker Compose Directly

```bash
# Start local environment
DB_USER_PASSWORD=test docker-compose -p portfolio-local \
  -f docker-compose.yml -f docker-compose.local.yml up --build -d

# View logs
docker-compose -p portfolio-local \
  -f docker-compose.yml -f docker-compose.local.yml logs -f

# Stop and clean up
docker-compose -p portfolio-local \
  -f docker-compose.yml -f docker-compose.local.yml down -v
```

---

## 4. Testing Procedures

### 4.1 Manual Health Checks

```bash
# Run health check script
chmod +x scripts/testing/test-health.sh
./scripts/testing/test-health.sh local
```

### 4.2 Individual Service Tests

**Test Frontend:**
```bash
curl http://localhost:4200/health.json
```

**Test Backend:**
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/health/ping
curl http://localhost:8080/api/health/db
```

**Test Nginx:**
```bash
curl http://localhost:8081/health
curl http://localhost:8081/health/full
```

**Test Database:**
```bash
docker exec portfolio-db-local psql -U postgres_app -d portfolio_local -c "SELECT 1;"
```

**Test Monitoring (if enabled):**
```bash
# Test Grafana
curl http://localhost:3001/api/health

# Test RabbitMQ
curl -u guest:guest http://localhost:15672/api/overview
```

### 4.3 Full Stack Validation

Run the complete validation script:

```bash
chmod +x scripts/deployment/validate-deployment.sh
./scripts/deployment/validate-deployment.sh local
```

This script performs:
1. Docker Compose configuration validation
2. Container health monitoring
3. Comprehensive smoke tests
4. Database connectivity verification

---

## 5. Access URLs

### 5.1 Local Environment

| Service | URL |
|---------|-----|
| Frontend (Angular) | http://localhost:4200 |
| Backend API | http://localhost:8080 |
| Backend Swagger UI | http://localhost:8080/swagger-ui.html |
| Backend Actuator | http://localhost:8080/actuator |
| Nginx (Full Stack) | http://localhost:8081 |
| PostgreSQL | localhost:5435 |
| Grafana | http://localhost:3001 |
| Prometheus | http://localhost:9090 |
| RabbitMQ Management | http://localhost:15672 |

### 5.2 Database Access

```bash
# Using psql
docker exec -it portfolio-db-local psql -U postgres_app -d portfolio_local

# Using pgAdmin or DBeaver
Host: localhost
Port: 5435
Database: portfolio_local
Username: postgres_app
Password: (from .env file)
```

---

## 6. Development Workflow

### 6.1 Recommended Testing Flow

1. Make code changes
2. Run validation: `make validate-local`
3. If validation passes, commit changes
4. Test staging: `make validate-staging`
5. If staging passes, merge to staging branch
6. After staging deployment, test production locally: `make validate-prod`

### 6.2 Continuous Integration

The `health-check.yml` workflow runs these same tests on every push to develop:

```yaml
- Builds all containers
- Waits for health checks
- Runs test-health.sh
- Validates all endpoints
```

---

## 7. Performance Testing

### 7.1 Load Testing (Optional)

Using Apache Bench:

```bash
# Test backend endpoint
ab -n 1000 -c 10 http://localhost:8080/api/health/ping

# Test frontend
ab -n 1000 -c 10 http://localhost:8081/
```

### 7.2 Resource Monitoring

```bash
# Monitor resource usage
docker stats portfolio-frontend-local portfolio-backend-local portfolio-db-local
```

---

## 8. Next Steps

After successful local testing:

1. Commit changes to feature branch
2. Open PR to develop
3. CI/CD will run automated tests
4. Merge to staging for staging deployment
5. Test staging environment
6. Merge to main for production deployment
