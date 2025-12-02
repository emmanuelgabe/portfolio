# CI/CD Deployment Guide

---

## Table of Contents

1. [Overview](#1-overview)
2. [Pipeline Architecture](#2-pipeline-architecture)
3. [GitHub Secrets Configuration](#3-github-secrets-configuration)
4. [Deployment Procedures](#4-deployment-procedures)
5. [Useful Commands](#5-useful-commands)

---

## 1. Overview

CI/CD infrastructure using GitHub Actions with self-hosted runner to automate build, test, and deployment processes.

### 1.1 Supported Environments

| Environment | Branch | Port | Database | Deployment |
|-------------|--------|------|----------|------------|
| Production | `main` | 80 | `portfolio_prod` | Automatic |
| Staging | `staging` | 3000 | `portfolio_staging` | Automatic |


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
3. Set up JDK 21 with Gradle caching
4. Grant execute permission for gradlew
5. Build backend JAR with `./gradlew clean bootJar -x test`
6. Output version for deployment job

**Deploy Job:**
1. **Set deployment variables** - Determine environment (prod/stage) and paths
2. **Clean deployment directory** - Remove old directory to avoid permission conflicts
3. **Copy artifacts** - Copy source code, .git, nginx config, and Docker Compose files
4. **Cleanup old containers** - Stop and remove existing containers for the environment
5. **Backup database** - Create PostgreSQL dump before deployment (production only)
6. **Deploy with Docker Compose** - Build and start all containers in detached mode
7. **Wait for healthy status** - Monitor container health (max 5 min, check every 5s)
8. **Run smoke tests** - Validate frontend, backend, database, and full stack integration
9. **Tag Docker images** - Tag with version, latest, and environment-specific tags
10. **Cleanup old images** - Prune dangling images and unused volumes
11. **Verify deployment** - Display running containers and health status
12. **Deployment summary** - Output environment, version, branch, and commit info

**Configuration:**
- **Triggers:**
  - Push to `main` → Production deployment
  - Push to `staging` → Staging deployment
  - Push to `develop` → Build only (no deployment)
  - Push to `dev/**` → Build only (no deployment)
  - Pull requests to `main` or `staging` → Build only
- **Runner:** self-hosted
- **Java Version:** 21 (Temurin distribution)
- **Environments:** production (main branch), staging (staging branch)
- **Blocking:** Yes - deployment fails if any step fails

**Critical Safety Features:**
- Automatic database backup before deployment (production only, keeps last 5)
- Automatic container cleanup before deployment
- Real-time health monitoring with timeout
- Comprehensive smoke tests (4 tests)
- Automatic logs display on failure
- Image versioning with multiple tags


### 2.3 Backend Tests Workflow

**Test Job:**
- Checkout source code
- Set up JDK 21 with Gradle caching
- Run JUnit tests with `./gradlew test`
- Generate test reports and coverage
- Publish test results to GitHub

**Build Job:**
- Build application with `./gradlew build -x test`
- Verify compilation without executing tests

**Configuration:**
- **Trigger:** Push/PR to `main`, `develop`, `dev` with backend changes
- **Java Version:** 21 (Temurin distribution)
- **Test Framework:** JUnit 5 + Spring Boot Test
- **Blocking:** Yes - tests must pass to merge

### 2.4 Frontend Tests Workflow

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

### 2.5 Documentation Quality Workflow

**Vale Linting Job:**
- Download Google Developer Documentation Style Guide
- Run Vale on all `.md` files
- Annotate PRs with style suggestions
- Upload validation results

**Configuration:**
- **Trigger:** Push/PR with documentation changes
- **Style Guide:** Google Developer Documentation
- **Blocking:** No - warnings don't block merge

### 2.6 Health Check Workflow

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


## 3. GitHub Secrets Configuration

### 3.2 Required GitHub Secrets


| Secret | Description | Used For |
|--------|-------------|----------|
| `DEPLOY_BASE_PATH` | Base deployment path on server | CI/CD only (not in .env) |
| `DB_PASSWORD` | PostgreSQL password | Passed to .env as `DB_USER_PASSWORD` |
| `MAIL_USERNAME` | SMTP email address | Passed to .env |
| `MAIL_APP_PASSWORD` | SMTP application password | Passed to .env |

---

## 4. Deployment Procedures

### 4.1 Production Deployment

**Standard Process:**
```bash
git checkout -b feature/new-feature
git add .
git commit -m "feat: add new feature"
git push origin feature/new-feature
# Create Pull Request to main branch → Review and merge
```

### 4.2 Staging Deployment

```bash
# Option 1: Direct push
git checkout staging
git merge feature/new-feature
git push origin staging

# Option 2: Pull Request to staging
```

### 4.3 Development Branch

```bash
git checkout -b dev/experimental-feature
git push origin dev/experimental-feature
# Build job executes, deploy job skipped
```

---


## 5. Useful Commands

### 5.1 Running Tests Locally

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

### 5.2 Container Management

```bash
docker ps -a
docker-compose -f docker-compose.yml -f docker-compose.prod.yml down
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d
docker logs -f portfolio-backend-prod
docker exec -it portfolio-backend-prod bash
```

### 5.3 Viewing CI/CD Results

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

### 5.4 Image Management

```bash
docker images | grep portfolio
docker rmi portfolio-backend:v1.0.0
docker image prune -a
docker system df
```

### 5.5 Volume Management

```bash
docker volume ls | grep portfolio
docker volume inspect portfolio_postgres_data_prod
```

### 5.6 Monitoring

```bash
docker stats
docker ps --format "table {{.Names}}\t{{.Status}}"
curl http://localhost/health
curl http://localhost:3000/health
```

### 5.7 Database Backup & Recovery

**Automatic Backups (Production Only):**
- Created automatically before each production deployment
- Location: `$DEPLOY_BASE_PATH/backups/`
- Naming: `portfolio_prod_YYYYMMDD_HHMMSS.sql`
- Retention: Last 5 backups kept

**Manual Backup:**
```bash
docker exec portfolio-db-prod pg_dump -U postgres_app portfolio_prod > backup.sql
```

**Restore from Backup:**
```bash
# Stop backend to prevent connections
docker stop portfolio-backend-prod

# Restore database
docker exec -i portfolio-db-prod psql -U postgres_app portfolio_prod < /path/to/backup.sql

# Restart backend
docker start portfolio-backend-prod
```

**List Available Backups:**
```bash
ls -lt $DEPLOY_BASE_PATH/backups/portfolio_prod_*.sql
```

---
