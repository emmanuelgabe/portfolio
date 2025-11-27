# Development Setup Guide

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Initial Setup](#2-initial-setup)
3. [Running the Application](#3-running-the-application)
4. [Development Workflow](#4-development-workflow)
5. [Initial Data Setup](#5-initial-data-setup)

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
- [Testing Guide](./testing-guide.md)
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

## 5. Initial Data Setup

### Data Seeders

The application includes automatic data seeders that run on first startup.

### AdminSeeder

Creates the initial admin user if no users exist in the database.

**Location**: `config/AdminSeeder.java`

**Behavior**:
- Checks if any users exist in the database
- If no users exist, creates admin user with credentials from environment variables
- Only runs once (subsequent startups skip if users exist)

**Configuration**:
```yaml
app:
  admin:
    username: ${ADMIN_USERNAME:admin}
    password: ${ADMIN_PASSWORD}  # Required, no default
```

**Environment Variables**:
```bash
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your_secure_admin_password
```

### DataSeeder

Seeds default data for development environments.

**Location**: `config/DataSeeder.java`

**Seeded Data**:
- Default site configuration
- Sample skill categories
- Initial tags

**Profile**: Only active in `dev` profile

```yaml
spring:
  profiles:
    active: dev  # Enables DataSeeder
```

### First-Time Setup

1. Set required environment variables:
```bash
# .env file
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your_secure_password
JWT_SECRET=your_jwt_secret_key_min_32_chars
```

2. Start the application
3. Admin user is created automatically
4. Login with configured credentials

---

## Additional Resources

- [Architecture Guide](../architecture/architecture.md) - System design
- [Testing Guide](./testing-guide.md) - Running tests
- [CI/CD Guide](../deployment/ci-cd.md) - Deployment process
- [Initial Setup](../security/initial-setup.md) - Security configuration
