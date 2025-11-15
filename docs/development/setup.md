# Development Setup Guide

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Initial Setup](#2-initial-setup)
3. [Running the Application](#3-running-the-application)
4. [Development Workflow](#4-development-workflow)
5. [Troubleshooting](#5-troubleshooting)

---

## 1. Prerequisites

### 1.1 Required Software

| Software | Minimum Version | Purpose |
|----------|----------------|---------|
| **Docker** | 20.10+ | Container runtime |
| **Docker Compose** | 2.0+ | Multi-container orchestration |
| **Git** | 2.30+ | Version control |

### 1.2 Optional Tools (for local development without Docker)

| Software | Version | Purpose |
|----------|--------|---------|
| Java (JDK) | 21 | Backend runtime |
| Node.js | 20+ | Frontend development |
| PostgreSQL | 17+ | Database |
| Gradle | 8.14+ | Backend build tool |

### 1.3 System Requirements

- **RAM**: 8GB minimum, 16GB recommended
- **Disk Space**: 10GB free space
- **OS**: Windows 10+, macOS 11+, or Linux

---

## 2. Initial Setup

### 2.1 Clone Repository

```bash
git clone <repository-url>
cd portfolio
```

### 2.2 Environment Configuration

Create `.env` file at project root:

```bash
# On Linux/macOS
cat > .env << EOF
DB_USER_PASSWORD=your_secure_password
EOF

# On Windows (PowerShell)
echo "DB_USER_PASSWORD=your_secure_password" > .env
```

**Important:** Replace `your_secure_password` with a secure password.

### 2.3 Verify Docker Installation

```bash
# Check Docker version
docker --version
# Expected: Docker version 20.10.0 or higher

# Check Docker Compose version
docker-compose --version
# Expected: Docker Compose version 2.0.0 or higher

# Verify Docker is running
docker ps
# Should return empty list or running containers (no error)
```

---

## 3. Running the Application

### 3.1 Start All Services (Recommended)

**Local Environment:**
```bash
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build -d
```

**Staging Environment:**
```bash
docker-compose -f docker-compose.yml -f docker-compose.staging.yml up --build -d
```

**Production Environment:**
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d
```

### 3.2 Access Points

Once services are running:

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:8081 | Angular application |
| **Backend API** | http://localhost:8081/api | REST API endpoints |
| **Swagger UI** | http://localhost:8081/api/swagger-ui.html | API documentation |
| **Health Check** | http://localhost:8081/health | Simple health status |
| **Full Health** | http://localhost:8081/health/full | Detailed health status |
| **Database** | localhost:5432 | PostgreSQL (local env) |

### 3.3 Verify Installation

```bash
# Check all containers are running
docker ps

# Expected output: 4 containers
# - portfolio-nginx-local
# - portfolio-frontend-local
# - portfolio-backend-local
# - portfolio-db-local

# Check health status
curl http://localhost:8081/health

# Expected: {"status":"ok"}
```

---

## 4. Development Workflow

### 4.1 View Logs

```bash
# All services
docker-compose -f docker-compose.yml -f docker-compose.local.yml logs -f

# Specific service
docker-compose logs -f portfolio-backend-local
docker-compose logs -f portfolio-frontend-local
docker-compose logs -f portfolio-db-local
```

### 4.2 Rebuild After Code Changes

**Backend changes:**
```bash
# Rebuild and restart backend only
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build -d portfolio-backend-local
```

**Frontend changes:**
```bash
# Rebuild and restart frontend only
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build -d portfolio-frontend-local
```

**All services:**
```bash
# Rebuild everything
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build -d
```

### 4.3 Run Tests

**Quick start:**
```bash
# Backend
docker exec portfolio-backend-local gradle test --no-daemon

# Frontend
docker exec portfolio-frontend-local npm test -- --watch=false
```

**For more testing commands and options, see:**
- [Testing Guide](./testing.md)
- `scripts/testing/` directory (if available)

### 4.4 Access Database

```bash
# Connect to PostgreSQL
docker exec -it portfolio-db-local psql -U portfolio_user -d portfolio_local

# Common commands:
# \dt          - List tables
# \d projects  - Describe projects table
# SELECT * FROM projects;
# \q           - Exit
```

### 4.5 Clean Up

**Stop services:**
```bash
docker-compose -f docker-compose.yml -f docker-compose.local.yml down
```

**Stop and remove volumes (CAUTION: deletes database data):**
```bash
docker-compose -f docker-compose.yml -f docker-compose.local.yml down -v
```

**Remove all Docker resources:**
```bash
# Remove stopped containers
docker container prune -f

# Remove unused images
docker image prune -a -f

# Remove unused volumes
docker volume prune -f
```

---

## 5. Common Issues

### Port Already in Use

**Error:** `bind: address already in use`

**Solution:**
```bash
# Find and kill process using the port
# Linux/macOS: lsof -i :8081
# Windows: netstat -ano | findstr :8081
```

### Containers Not Starting

**Solutions:**
1. Check logs: `docker logs portfolio-backend-local`
2. Verify `.env` file exists with `DB_USER_PASSWORD`
3. Clean rebuild: `docker-compose down && docker-compose up --build -d`

### Backend Returns 502

**Cause:** Backend not ready yet or crashed

**Solution:**
```bash
# Wait 30-60 seconds for services to become healthy
# Check status: docker ps
# Check logs: docker logs portfolio-backend-local
```

### Build Errors

**Solution:**
```bash
# Clean and rebuild from scratch
docker-compose build --no-cache
```

**For more troubleshooting, check:**
- Container logs: `docker logs <container-name>`
- GitHub Issues: Common problems and solutions

---

## Additional Resources

- [Architecture Guide](../architecture/architecture.md) - System design
- [Testing Guide](./testing.md) - Running tests
- [CI/CD Guide](../deployment/ci-cd.md) - Deployment process
