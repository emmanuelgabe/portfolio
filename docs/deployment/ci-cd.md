# CI/CD Deployment Guide

**Document Type:** Deployment Guide
**Version:** 2.2.0
**Last Updated:** 2025-11-10
**Status:** Active

---

## Table of Contents

1. [Overview](#1-overview)
2. [Pipeline Architecture](#2-pipeline-architecture)
3. [Prerequisites](#3-prerequisites)
4. [Initial Configuration](#4-initial-configuration)
5. [Deployment Procedures](#5-deployment-procedures)
6. [Environment Configuration](#6-environment-configuration)
7. [Version Management](#7-version-management)
8. [Useful Commands](#8-useful-commands)

---

## 1. Overview

CI/CD infrastructure using GitHub Actions with self-hosted runner to automate build, test, and deployment processes.

### 1.1 Supported Environments

| Environment | Branch | Port | Database | Deployment |
|-------------|--------|------|----------|------------|
| Production | `main` | 80 | `portfolio_prod` | Automatic |
| Staging | `staging` | 3000 | `portfolio_staging` | Automatic |
| Development | `dev/**` | N/A | N/A | Build only |

### 1.2 Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Backend | Spring Boot (Java) | 3.5.5 (Java 24) |
| Frontend | Angular | 20.0.0 |
| Database | PostgreSQL | 17.6 |
| Reverse Proxy | Nginx | 1.27 |
| Container Platform | Docker & Docker Compose | 20.10+ |

---

## 2. Pipeline Architecture

### 2.1 Active Workflows

The CI/CD pipeline consists of five automated workflows:

| Workflow | File | Trigger | Purpose |
|----------|------|---------|---------|
| **CI/CD Pipeline** | `ci-cd.yml` | Push/PR on main/staging/dev/** | Build and deploy to environments |
| **Backend Tests** | `backend-tests.yml` | Push/PR on backend changes | Run Spring Boot unit tests |
| **Frontend Tests** | `frontend-tests.yml` | Push/PR on frontend changes | Run Angular unit tests |
| **Documentation Quality** | `vale-docs.yml` | Push/PR on `.md` changes | Validate documentation style |
| **Health Check** | `health-check.yml` | Push/PR on main/develop | Full stack integration tests |

### 2.2 Main CI/CD Pipeline

**Primary deployment workflow that builds and deploys the application to production and staging environments.**

**Build-and-Test Job:**
1. Checkout code with full Git history
2. Extract version from Git tags using `git describe`
3. Set up JDK 24 with Gradle caching
4. Grant execute permission for gradlew
5. Build backend JAR with `./gradlew clean bootJar -x test`
6. Output version for deployment job

**Deploy Job:**
1. **Set deployment variables** - Determine environment (prod/stage) and paths
2. **Clean deployment directory** - Remove old directory to avoid permission conflicts
3. **Copy artifacts** - Copy source code, .git, nginx config, and Docker Compose files
4. **Cleanup old containers** - Stop and remove existing containers for the environment
5. **Deploy with Docker Compose** - Build and start all containers in detached mode
6. **Wait for healthy status** - Monitor container health (max 5 min, check every 5s)
7. **Run smoke tests** - Validate frontend, backend, database, and full stack integration
8. **Tag Docker images** - Tag with version, latest, and environment-specific tags
9. **Cleanup old images** - Prune dangling images and unused volumes
10. **Verify deployment** - Display running containers and health status
11. **Deployment summary** - Output environment, version, branch, and commit info

**Configuration:**
- **Triggers:**
  - Push to `main` → Production deployment
  - Push to `staging` → Staging deployment
  - Push to `develop` → Build only (no deployment)
  - Push to `dev/**` → Build only (no deployment)
  - Pull requests to `main` or `staging` → Build only
- **Runner:** self-hosted
- **Java Version:** 24 (Temurin distribution)
- **Environments:** production (main branch), staging (staging branch)
- **Blocking:** Yes - deployment fails if any step fails

**Critical Safety Features:**
- Automatic container cleanup before deployment
- Real-time health monitoring with timeout
- Comprehensive smoke tests (4 tests)
- Automatic logs display on failure
- Image versioning with multiple tags

### 2.3 Workflow Triggers & Execution Order

**Trigger Matrix:**

| Event | Branch | CI/CD Pipeline | Backend Tests | Frontend Tests | Health Check | Vale Docs |
|-------|--------|----------------|---------------|----------------|--------------|-----------|
| Push | `main` | ✅ Build + Deploy | ✅ (if backend/*) | ✅ (if frontend/*) | ❌ | ✅ (if *.md) |
| Push | `staging` | ✅ Build + Deploy | ✅ (if backend/*) | ✅ (if frontend/*) | ❌ | ✅ (if *.md) |
| Push | `develop` | ✅ Build only | ✅ (if backend/*) | ✅ (if frontend/*) | ✅ | ✅ (if *.md) |
| Push | `dev/**` | ✅ Build only | ✅ (if backend/*) | ✅ (if frontend/*) | ❌ | ✅ (if *.md) |
| PR | `main` | ✅ Build only | ✅ (if backend/*) | ✅ (if frontend/*) | ❌ | ✅ (if *.md) |
| PR | `staging` | ✅ Build only | ✅ (if backend/*) | ✅ (if frontend/*) | ❌ | ✅ (if *.md) |

**Execution Sequence:**

```
Push to main/staging
│
├─→ CI/CD Pipeline (ci-cd.yml)
│   ├─→ Job 1: build-and-test (runs immediately)
│   │   ├─ Extract version
│   │   ├─ Build backend JAR
│   │   └─ Output: VERSION
│   │
│   └─→ Job 2: deploy (waits for Job 1, only on main/staging)
│       ├─ Clean & prepare deployment directory
│       ├─ Copy artifacts
│       ├─ Cleanup old containers
│       ├─ Deploy with Docker Compose
│       ├─ Wait for containers to be healthy (max 5 min)
│       ├─ Run smoke tests (4 tests)
│       ├─ Tag Docker images
│       └─ Verify deployment
│
├─→ Backend Tests (backend-tests.yml) - Runs in parallel if backend/** changed
│   ├─→ Job 1: test
│   │   ├─ Run unit tests
│   │   ├─ Generate coverage
│   │   └─ Publish results
│   │
│   └─→ Job 2: build (waits for Job 1)
│       └─ Build without tests
│
├─→ Frontend Tests (frontend-tests.yml) - Runs in parallel if frontend/** changed
│   ├─→ Job 1: test
│   │   ├─ Run Karma tests
│   │   └─ Upload coverage
│   │
│   ├─→ Job 2: lint (runs in parallel with test)
│   │   └─ Run ESLint
│   │
│   └─→ Job 3: build (waits for test + lint)
│       └─ Build production bundle
│
├─→ Health Check (health-check.yml) - Runs in parallel on develop only
│   └─→ Job: health-check
│       ├─ Build Docker containers (local dev environment)
│       ├─ Start full stack
│       ├─ Wait for healthy status
│       └─ Test all endpoints
│
└─→ Vale Docs (vale-docs.yml) - Runs in parallel if *.md changed
    └─→ Job: vale-linting
        └─ Validate documentation style
```

### 2.4 Backend Tests Workflow

**Test Job:**
- Checkout source code
- Set up JDK 24 with Gradle caching
- Run JUnit tests with `./gradlew test`
- Generate test reports and coverage
- Publish test results to GitHub

**Build Job:**
- Build application with `./gradlew build -x test`
- Verify compilation without executing tests

**Configuration:**
- **Trigger:** Push/PR to `main`, `develop`, `dev` with backend changes
- **Java Version:** 24 (Temurin distribution)
- **Test Framework:** JUnit 5 + Spring Boot Test
- **Blocking:** Yes - tests must pass to merge

### 2.5 Frontend Tests Workflow

**Test Job:**
- Checkout source code
- Set up Node.js 20 with npm caching
- Install dependencies with `npm ci`
- Run Karma tests with ChromeHeadless
- Generate code coverage reports
- Upload coverage artifacts

**Lint Job:**
- Run ESLint (if configured in package.json)
- Report code quality issues

**Build Job:**
- Build production bundle with `npm run build --configuration production`
- Verify build output in `dist/` folder

**Configuration:**
- **Trigger:** Push/PR to `main`, `develop`, `dev` with frontend changes
- **Node Version:** 20
- **Test Framework:** Karma + Jasmine
- **Browser:** ChromeHeadless
- **Blocking:** Yes - tests must pass to merge

### 2.6 Documentation Quality Workflow

**Vale Linting Job:**
- Download Google Developer Documentation Style Guide
- Run Vale on all `.md` files
- Annotate PRs with style suggestions
- Upload validation results

**Configuration:**
- **Trigger:** Push/PR with documentation changes
- **Style Guide:** Google Developer Documentation
- **Blocking:** No - warnings don't block merge

### 2.7 Health Check Workflow

**Health Check Job:**
- Build Docker containers using `docker-compose.local.yml` (development environment)
- Start full stack (frontend with `ng serve`, backend, database, nginx)
- Wait for all containers to be healthy
- Test individual endpoints
- Test full health chain
- Show logs on failure

**Configuration:**
- **Trigger:** Push/PR to `develop` only
- **Timeout:** 15 minutes
- **Blocking:** Yes - all services must be healthy
- **Environment:** Local development setup (not production/staging)

> **Note:** This workflow tests the development environment only and is NOT triggered on `staging` or `main` branches. Production and staging deployments have their own health checks integrated in the main CI/CD Pipeline workflow. The frontend health check is disabled in local dev mode because the Angular dev server (`ng serve`) takes too long to start reliably in CI environments.

### 2.8 Key Features

| Feature | Benefit |
|---------|---------|
| **Isolated Test Workflows** | Faster feedback on specific changes |
| **Path-based Triggers** | Only run relevant tests (e.g., backend tests only on backend changes) |
| **Parallel Execution** | Backend and frontend tests run simultaneously |
| **Test Result Publishing** | Detailed reports in GitHub PR checks |
| **Coverage Reports** | Track code coverage over time |
| **Artifact Retention** | Test reports kept for 7 days |
| **Build Verification** | Ensure code compiles for production |
| **Documentation Validation** | Automated style guide enforcement |

### 2.9 Critical Deployment Steps

The main CI/CD pipeline includes several critical safety steps that ensure reliable deployments:

#### Container Cleanup (Step 4)
```bash
# Stops and removes old containers for the environment
docker ps -a --filter "name=portfolio.*-$ENV_NAME" | xargs -r docker stop
docker ps -a --filter "name=portfolio.*-$ENV_NAME" | xargs -r docker rm
docker image prune -f
```

**Purpose:** Prevents port conflicts, resource leaks, and ensures clean state.

#### Health Check Monitoring (Step 6)
```bash
# Monitors container health status in real-time
MAX_ATTEMPTS=60  # 5 minutes max (60 × 5s)
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  # Check each container's health status
  DB_HEALTH=$(docker inspect --format='{{.State.Health.Status}}' portfolio-db-$ENV_NAME)
  BACKEND_HEALTH=$(docker inspect --format='{{.State.Health.Status}}' portfolio-backend-$ENV_NAME)
  FRONTEND_HEALTH=$(docker inspect --format='{{.State.Health.Status}}' portfolio-frontend-$ENV_NAME)

  # Exit when all are healthy
  if all healthy; then exit 0; fi

  # Fail fast if any become unhealthy
  if any unhealthy; then show logs and exit 1; fi

  sleep 5
done
```

**Features:**
- Real-time monitoring every 5 seconds
- 5-minute timeout to prevent infinite waiting
- Automatic log display on failure
- Fail-fast on unhealthy status

#### Smoke Tests (Step 7)

**Test 1 - Frontend Health:**
```bash
curl http://localhost:$PORT/health.json
# Expected: HTTP 200
```
Validates: Nginx routing, Angular build, static file serving

**Test 2 - Backend Actuator:**
```bash
docker exec portfolio-backend-$ENV_NAME curl http://localhost:8080/actuator/health
# Expected: HTTP 200
```
Validates: Spring Boot startup, application context, actuator endpoints

**Test 3 - Database Connectivity:**
```bash
docker exec portfolio-db-$ENV_NAME psql -U postgres_app -d $DB_NAME -c "SELECT 1;"
# Expected: 1 row returned
```
Validates: PostgreSQL running, database exists, credentials valid

**Test 4 - Full Stack Integration:**
```bash
curl http://localhost:$PORT/
# Expected: HTTP 200 or 304
```
Validates: End-to-end flow (Nginx → Angular → Spring Boot → PostgreSQL)

**All tests must pass for deployment to succeed.**

---

## 3. Prerequisites

### 3.1 Development Machine

- Git 2.30+
- GitHub repository access
- Push permissions for `main` and `staging` branches

### 3.2 Deployment Server

**Software:**
- Docker Engine 20.10+
- Docker Compose 1.29+
- Java 21 (Temurin distribution)
- Git 2.30+

**Network:**
- Outbound internet access
- Inbound access on ports 80, 3000, 8081

**Verification:**
```bash
docker --version
docker-compose --version
java -version
git --version
```

---

## 4. Initial Configuration

### 4.1 GitHub Actions Runner Setup

```bash
# Create runner directory
mkdir -p ~/actions-runner
cd ~/actions-runner

# Download and configure runner
# Follow: GitHub Repository → Settings → Actions → Runners → New self-hosted runner

# Install as system service
sudo ./svc.sh install
sudo ./svc.sh start
sudo ./svc.sh status
```

### 4.2 GitHub Secrets Configuration

Navigate to: `GitHub Repository → Settings → Secrets and variables → Actions`

| Secret Name | Description |
|-------------|-------------|
| `DB_PASSWORD` | PostgreSQL database password |

### 4.3 Directory Structure

```
/home/manu/projects/portfolio/
├── prod/                      # Production environment
│   ├── portfolio-backend/
│   ├── portfolio-frontend/
│   ├── nginx/
│   ├── docker-compose.yml
│   └── docker-compose.prod.yml
└── stage/                     # Staging environment
    ├── portfolio-backend/
    ├── portfolio-frontend/
    ├── nginx/
    ├── docker-compose.yml
    └── docker-compose.staging.yml
```

### 4.4 Docker Configuration

**Backend Dockerfile:**
- Multi-stage build (~250 MB, reduced from ~800 MB)
- Gradle dependency caching
- Non-root user for security
- Built-in health check
- JVM memory optimization

**Frontend Dockerfile:**
- Multi-stage build (Node + Nginx)
- Git support for version generation
- Custom Nginx configuration
- Optimized for production

**Dockerignore Files:**
- 80% reduction in build context size
- 33% faster build times

---

## 5. Deployment Procedures

### 5.1 Production Deployment

**Standard Process:**
```bash
git checkout -b feature/new-feature
git add .
git commit -m "feat: add new feature"
git push origin feature/new-feature
# Create Pull Request to main branch → Review and merge
```

**Emergency Hotfix:**
```bash
git checkout main
git add .
git commit -m "fix: critical bug fix"
git push origin main
```

### 5.2 Staging Deployment

```bash
# Option 1: Direct push
git checkout staging
git merge feature/new-feature
git push origin staging

# Option 2: Pull Request to staging
```

### 5.3 Development Branch

```bash
git checkout -b dev/experimental-feature
git push origin dev/experimental-feature
# Build job executes, deploy job skipped
```

---

## 6. Environment Configuration

### 6.1 Production Environment

| Parameter | Value |
|-----------|-------|
| HTTP Port | 80 |
| Database Port | 5432 |
| Database Name | `portfolio_prod` |
| Spring Profile | `prod` |
| Container Prefix | `portfolio-*-prod` |

**Access Commands:**
```bash
docker ps | grep prod
docker logs portfolio-backend-prod
docker logs portfolio-frontend-prod
docker exec -it portfolio-db-prod psql -U postgres_app -d portfolio_prod
```

### 6.2 Staging Environment

| Parameter | Value |
|-----------|-------|
| HTTP Port | 3000 |
| Database Port | 5434 |
| Database Name | `portfolio_staging` |
| Spring Profile | `staging` |
| Container Prefix | `portfolio-*-staging` |

**Access Commands:**
```bash
docker ps | grep staging
docker logs portfolio-backend-staging
curl http://localhost:3000
```

### 6.3 Local Development

```bash
DB_USER_PASSWORD=your_password docker-compose \
  -f docker-compose.yml \
  -f docker-compose.local.yml \
  up -d
```

---

## 7. Version Management

### 7.1 Version Generation

```bash
git describe --tags --always --dirty
```

**Version Formats:**

| Scenario | Output |
|----------|--------|
| Exact tag | `v1.2.3` |
| No tags | `0.0.1-SNAPSHOT` |
| After tag | `v1.2.3-5-g2a3b4c5` |
| Uncommitted changes | `v1.2.3-dirty` |

### 7.2 Creating New Version

```bash
git tag -a v1.2.3 -m "Release version 1.2.3"
git push origin v1.2.3
git checkout main
git push origin main
```

### 7.3 Docker Image Tags

After deployment:
```
portfolio-backend:v1.2.3          # Specific version
portfolio-backend:latest           # Latest overall
portfolio-backend:prod-latest      # Latest for production
```

---

## 8. Useful Commands

### 8.1 Running Tests Locally

**Backend Tests:**
```bash
cd portfolio-backend
./gradlew test                    # Run all tests
./gradlew test --tests HealthControllerTest  # Run specific test
./gradlew test jacocoTestReport   # Generate coverage report
```

**Frontend Tests:**
```bash
cd portfolio-frontend
npm test                          # Run tests in watch mode
npm test -- --watch=false         # Run once and exit
npm test -- --code-coverage       # Generate coverage
npm test -- --browsers=ChromeHeadlessCI  # Headless mode (like CI)
```

**Documentation Validation:**
```bash
./scripts/validate-docs.sh        # Validate all documentation
./scripts/validate-docs.sh docs/  # Validate specific directory
```

### 8.2 Container Management

```bash
docker ps -a
docker-compose -f docker-compose.yml -f docker-compose.prod.yml down
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d
docker logs -f portfolio-backend-prod
docker exec -it portfolio-backend-prod bash
```

### 8.3 Viewing CI/CD Results

**GitHub Actions:**
- Navigate to repository → "Actions" tab
- Select workflow run to view details
- Download test artifacts for detailed reports

**Test Reports Location:**
- Backend: `portfolio-backend/build/reports/tests/test/index.html`
- Frontend: `portfolio-frontend/coverage/index.html`

**Logs:**
```bash
# View GitHub Actions runner logs
sudo journalctl -u actions.runner.* -f
```

### 8.4 Image Management

```bash
docker images | grep portfolio
docker rmi portfolio-backend:v1.0.0
docker image prune -a
docker system df
```

### 8.5 Volume Management

```bash
docker volume ls | grep portfolio
docker exec portfolio-db-prod pg_dump -U postgres_app portfolio_prod > backup.sql
cat backup.sql | docker exec -i portfolio-db-prod psql -U postgres_app -d portfolio_prod
```

### 8.6 Monitoring

```bash
docker stats
docker ps --format "table {{.Names}}\t{{.Status}}"
curl http://localhost/health
curl http://localhost:3000/health
```

---

## 9. Troubleshooting

### 9.1 Common Deployment Errors

#### Error: "Port already allocated"
```
Error response from daemon: driver failed programming external connectivity:
Bind for 0.0.0.0:80 failed: port is already allocated
```

**Cause:** Old containers still running on the same port.

**Solution:**
```bash
# Find containers using the port
docker ps | grep portfolio

# Stop and remove old containers
docker stop $(docker ps -a --filter "name=portfolio" --format "{{.Names}}")
docker rm $(docker ps -a --filter "name=portfolio" --format "{{.Names}}")
```

#### Error: "Permission denied" during file copy
```
cp: cannot create directory '/home/manu/projects/portfolio/stage/.git': Permission denied
```

**Cause:** Old deployment directory has wrong permissions.

**Solution:** The CI/CD pipeline now automatically removes the old directory before copying. If this still occurs:
```bash
# Manual cleanup
sudo rm -rf /home/manu/projects/portfolio/stage
```

#### Error: "Container is unhealthy"
```
❌ One or more containers are unhealthy!
```

**Cause:** Service failed health check (database not ready, backend crashed, etc.).

**Solution:**
```bash
# Check container logs
docker logs portfolio-backend-staging
docker logs portfolio-db-staging
docker logs portfolio-frontend-staging

# Check container health status
docker inspect portfolio-backend-staging --format='{{.State.Health}}'

# Common fixes:
# - Database: Check password in secrets
# - Backend: Check Spring Boot logs for startup errors
# - Frontend: Check nginx config syntax
```

#### Error: Smoke tests failing
```
❌ Frontend health check failed (HTTP 000)
```

**Cause:** Container started but service not responding.

**Solution:**
```bash
# Test manually
curl http://localhost:3000/health.json
curl http://localhost:3000/

# Check if nginx is running
docker exec portfolio-nginx-staging nginx -t

# Check frontend build
docker exec portfolio-frontend-staging ls -la /usr/share/nginx/html/
```

### 9.2 GitHub Actions Runner Issues

#### Runner offline
```bash
# Check runner service status
sudo systemctl status actions.runner.*

# Restart runner
cd ~/actions-runner
sudo ./svc.sh stop
sudo ./svc.sh start

# Check logs
sudo journalctl -u actions.runner.* -f
```

#### Build failing on self-hosted runner
```bash
# Check disk space
df -h

# Clean up Docker resources
docker system prune -a -f
docker volume prune -f

# Check runner logs
cd ~/actions-runner
tail -f _diag/Runner_*.log
```

### 9.3 Version Issues

#### Wrong version displayed
```bash
# Check Git tags
git describe --tags --always --dirty

# Verify frontend version
docker exec portfolio-frontend-staging cat /usr/share/nginx/html/health.json

# Verify backend version
curl http://localhost:8080/actuator/info | jq '.build.version'
```

#### No version generated
**Cause:** .git directory not copied to deployment location.

**Solution:** The CI/CD pipeline automatically copies .git. Verify:
```bash
ls -la /home/manu/projects/portfolio/stage/.git
```

### 9.4 Database Issues

#### Database container won't start
```bash
# Check database logs
docker logs portfolio-db-staging

# Common issues:
# - Volume corruption: docker volume rm portfolio_db_staging_data
# - Port conflict: lsof -i :5434
# - Wrong password: Check GitHub secrets
```

#### Connection refused from backend
```bash
# Verify database is healthy
docker inspect portfolio-db-staging --format='{{.State.Health.Status}}'

# Test connection manually
docker exec portfolio-backend-staging curl -v telnet://portfolio-db-staging:5432

# Check Spring Boot datasource config
docker exec portfolio-backend-staging env | grep SPRING_DATASOURCE
```

### 9.5 Debugging Commands

**View real-time deployment logs:**
```bash
# GitHub Actions runner logs
sudo journalctl -u actions.runner.* -f

# Container logs during deployment
watch -n 1 'docker ps --format "table {{.Names}}\t{{.Status}}"'
```

**Check container health progression:**
```bash
# Watch health status change
watch -n 2 'docker inspect portfolio-backend-staging --format="{{.State.Health.Status}}"'
```

**Full diagnostic:**
```bash
# Container status
docker ps -a --filter "name=portfolio"

# Resource usage
docker stats --no-stream

# Network connectivity
docker network inspect portfolio-network

# Volume status
docker volume ls | grep portfolio

# Disk usage
docker system df
```

### 9.6 Rollback Procedure

If deployment fails and you need to rollback:

```bash
# Option 1: Redeploy previous version
git checkout <previous-commit>
git push origin staging --force

# Option 2: Use previous Docker image
docker stop portfolio-backend-staging
docker rm portfolio-backend-staging
docker run -d --name portfolio-backend-staging \
  --network portfolio-network \
  portfolio-backend:v1.0.0  # Previous version tag

# Option 3: Restore from backup
docker-compose -f docker-compose.yml -f docker-compose.staging.yml down
# Restore database backup
cat backup.sql | docker exec -i portfolio-db-staging psql -U postgres_app -d portfolio_staging
docker-compose -f docker-compose.yml -f docker-compose.staging.yml up -d
```

---

## Change History

| Version | Date       | Changes |
|---------|------------|---------|
| 2.2.0   | 2025-11-10 | Restricted Health Check CI workflow to `develop` branch only (removed from main/staging), disabled frontend health check in local dev mode, increased health check timeouts (90s start_period, 5 retries) for production/staging |
| 2.1.0   | 2025-11-09 | Added `develop` branch to CI/CD pipeline (build-only), added `staging` branch to health check workflow, improved healthcheck script to handle containers without healthcheck configuration |
| 2.0.0   | 2025-11-09 | Major update: Added main CI/CD pipeline documentation, workflow triggers matrix, execution sequence diagrams, critical deployment steps, smoke tests details, and comprehensive troubleshooting guide |
| 1.1.0   | 2025-11-09 | Added automated testing workflows (backend, frontend, docs) |
| 1.0.0   | 2025-11-09 | Initial release |

---

**Document Type:** Deployment Guide
**Version:** 2.2.0
**Last Updated:** 2025-11-10
**Status:** Active
