# CI/CD Deployment Guide

**Document Type:** Deployment Guide
**Version:** 1.0.0
**Last Updated:** 2025-11-09
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

The CI/CD pipeline consists of four automated workflows:

| Workflow | File | Trigger | Purpose |
|----------|------|---------|---------|
| **Backend Tests** | `backend-tests.yml` | Push/PR on backend changes | Run Spring Boot unit tests |
| **Frontend Tests** | `frontend-tests.yml` | Push/PR on frontend changes | Run Angular unit tests |
| **Documentation Quality** | `vale-docs.yml` | Push/PR on `.md` changes | Validate documentation style |
| **Health Check** | `health-check.yml` | Push/PR on main/develop | Full stack integration tests |

### 2.2 Backend Tests Workflow

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

### 2.3 Frontend Tests Workflow

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

### 2.4 Documentation Quality Workflow

**Vale Linting Job:**
- Download Google Developer Documentation Style Guide
- Run Vale on all `.md` files
- Annotate PRs with style suggestions
- Upload validation results

**Configuration:**
- **Trigger:** Push/PR with documentation changes
- **Style Guide:** Google Developer Documentation
- **Blocking:** No - warnings don't block merge

### 2.5 Health Check Workflow

**Health Check Job:**
- Build Docker containers
- Start full stack (frontend, backend, database, nginx)
- Wait for all containers to be healthy
- Test individual endpoints
- Test full health chain
- Show logs on failure

**Configuration:**
- **Trigger:** Push/PR to `main`, `develop`
- **Timeout:** 15 minutes
- **Blocking:** Yes - all services must be healthy

### 2.6 Key Features

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

## Change History

| Version | Date       | Changes |
|---------|------------|---------|
| 1.1.0   | 2025-11-09 | Added automated testing workflows (backend, frontend, docs) |
| 1.0.0   | 2025-11-09 | Initial release |

---

**Document Type:** Deployment Guide
**Version:** 1.1.0
**Last Updated:** 2025-11-09
**Status:** Active
