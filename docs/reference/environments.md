# Environments Configuration

Configuration reference for Local, Staging, and Production environments.

---

## Table of Contents

1. [Overview](#overview)
2. [Docker Compose](#docker-compose)
3. [Environments](#environments)
4. [URLs by Environment](#urls-by-environment)
5. [Quick Commands](#quick-commands)

---

## Overview

The Portfolio project uses Docker Compose with environment-specific override files:

**File Structure:**
- `docker-compose.yml` - Base configuration (shared across all environments)
- `docker-compose.local.yml` - Local development overrides
- `docker-compose.staging.yml` - Staging environment overrides
- `docker-compose.prod.yml` - Production environment overrides

---

## Docker Compose

### Start Services

```bash
# Local Environment
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build -d

# Staging Environment
docker-compose -f docker-compose.yml -f docker-compose.staging.yml up --build -d

# Production Environment
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d
```

### Stop Services

```bash
# Local
docker-compose -f docker-compose.yml -f docker-compose.local.yml down

# Staging
docker-compose -f docker-compose.yml -f docker-compose.staging.yml down

# Production
docker-compose -f docker-compose.yml -f docker-compose.prod.yml down
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service (Local)
docker-compose logs -f portfolio-backend-local
docker-compose logs -f portfolio-frontend-local
docker-compose logs -f portfolio-db-local

# Specific service (Staging)
docker-compose logs -f portfolio-backend-staging
docker-compose logs -f portfolio-frontend-staging

# Specific service (Production)
docker-compose logs -f portfolio-backend-prod
docker-compose logs -f portfolio-frontend-prod
```

---

## Environments

### Environment Comparison

| Environment | Docker Compose | Spring Profile | Frontend Port | Backend Port | Database |
|-------------|----------------|----------------|---------------|--------------|----------|
| **Local** | `docker-compose.local.yml` | `dev` | 4200 | 8080 | localhost:5432 |
| **Staging** | `docker-compose.staging.yml` | `staging` | 3000 | 3001 | Internal |
| **Production** | `docker-compose.prod.yml` | `prod` | 80/443 | Internal | Internal |

### Local Development

**Characteristics:**
- Hot reload enabled (frontend and backend)
- Debug logging (DEBUG level)
- Local database on port 5432
- Swagger UI enabled
- No HTTPS (HTTP only)

**Use Case:**
- Active development
- Testing new features
- Debugging
- Running unit and integration tests

---

### Staging (Pre-Production)

**Characteristics:**
- Production-like configuration
- INFO logging level
- Internal database (Docker network)
- Swagger UI disabled
- HTTPS optional

**Use Case:**
- Pre-production validation
- Smoke testing
- User acceptance testing (UAT)
- Performance testing
- Final validation before production

---

### Production

**Characteristics:**
- Optimized builds
- WARN logging level (errors only)
- Internal database (Docker network)
- Swagger UI disabled
- HTTPS required (Let's Encrypt certificates)
- Health checks enabled

**Use Case:**
- Live production environment
- Serving real users
- High availability
- Monitoring and alerting enabled

---

## URLs by Environment

### Local Development

**Access Points:**
- **Frontend**: http://localhost:4200 ou http://127.0.0.1:4200
- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs (JSON)**: http://localhost:8080/api-docs
- **Database**: localhost:5432
  - **User**: `postgres`
  - **Password**: `postgres`
  - **Database**: `portfolio_local`
- **Health Check**: http://localhost:8080/actuator/health

**Nginx:** Reverse proxy actif sur port 4200 (route vers frontend et API)

---

### Staging (Internal Network)

**Access Points:**
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:3001
- **Health Check**: http://localhost:3001/actuator/health
- **Database**: Internal (not exposed)

**Nginx:** Routes requests between frontend and backend

**Notes:**
- Swagger UI disabled
- API docs disabled
- Database accessible only within Docker network

---

### Production

**Access Points:**
- **Frontend**: https://yoursite.com (port 80/443)
- **Backend API**: https://api.yoursite.com (internal routing)
- **Health Check**: https://api.yoursite.com/actuator/health
- **Database**: Internal (not exposed)

**Nginx:**
- Handles HTTPS termination
- Routes requests
- Serves static files
- Load balancing (if scaled)

**Security:**
- HTTPS enforced (automatic redirect from HTTP)
- Let's Encrypt SSL certificates
- HSTS headers enabled
- Security headers (CSP, X-Frame-Options, etc.)

**Notes:**
- Swagger UI disabled
- API docs disabled
- All logs in JSON format (Logstash)
- Database accessible only within Docker network

---

## Quick Commands

### Health Checks

```bash
# Local
curl http://localhost:8080/actuator/health

# Staging
curl http://localhost:3001/actuator/health

# Production
curl https://api.yoursite.com/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

---

### Database Access

**Local Only:**
```bash
# Connect to PostgreSQL
docker exec -it portfolio-db-local psql -U portfolio_user -d portfolio_local

# Common commands:
# \dt          - List tables
# \d projects  - Describe projects table
# SELECT * FROM projects;
# \q           - Exit
```

**Staging/Production:**
Database is not exposed externally. Access only via application or Docker network.

---

### Service Status

```bash
# Check all running containers
docker ps

# Expected output (Local):
# - portfolio-nginx-local
# - portfolio-frontend-local
# - portfolio-backend-local
# - portfolio-db-local
```

---

### Rebuild Specific Service

```bash
# Rebuild and restart backend only (Local)
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build -d portfolio-backend-local

# Rebuild and restart frontend only (Local)
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build -d portfolio-frontend-local

# Rebuild everything (Local)
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build -d
```

---

### Clean Up

```bash
# Stop and remove containers (Local)
docker-compose -f docker-compose.yml -f docker-compose.local.yml down

# Stop and remove volumes (CAUTION: deletes database data)
docker-compose -f docker-compose.yml -f docker-compose.local.yml down -v

# Remove all unused Docker resources
docker system prune -a -f
```

---

## Environment Variables

### Required Variables

**.env file (root directory):**
```bash
# Database
DB_USER_PASSWORD=your_secure_password

# JWT (Production/Staging only)
JWT_SECRET=your_jwt_secret_key

# Optional: Custom ports
BACKEND_PORT=8080
FRONTEND_PORT=4200
```

### Spring Profiles

**application.yml:**
- `spring.profiles.active=dev` (Local)
- `spring.profiles.active=staging` (Staging)
- `spring.profiles.active=prod` (Production)

Each profile has its own configuration file:
- `application-dev.yml`
- `application-staging.yml`
- `application-prod.yml`

---

## Port Reference

| Service | Local | Staging | Production |
|---------|-------|---------|------------|
| **Frontend** | 4200 | 3000 | 80/443 |
| **Backend** | 8080 | 3001 | Internal (8080) |
| **Database** | 5432 | Internal | Internal |
| **Nginx** | N/A | 80/443 | 80/443 |

**Internal Ports:** Accessible only within Docker network, not exposed to host.

---

## Related Documentation

- [Setup Guide](../development/setup.md) - Initial development environment setup
- [CI/CD Guide](../deployment/ci-cd.md) - Deployment automation
- [Health Checks](../operations/health-checks.md) - Monitoring and health endpoints
