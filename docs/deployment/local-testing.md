# Local CI/CD Testing Guide

**Document Type:** Testing Guide
**Version:** 1.0.0
**Last Updated:** 2025-11-10
**Status:** Active

---

## Table of Contents

1. [Overview](#1-overview)
2. [Why Test Locally](#2-why-test-locally)
3. [Prerequisites](#3-prerequisites)
4. [Testing Strategies](#4-testing-strategies)
5. [Validation Scripts](#5-validation-scripts)
6. [Professional Best Practices](#6-professional-best-practices)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Overview

This guide explains how to test CI/CD operations locally before committing changes, helping you catch configuration errors early and avoid failed deployments in staging/production environments.

### 1.1 Purpose

Testing locally before pushing allows you to:
- ✅ Detect Docker Compose configuration errors early
- ✅ Validate service connectivity (backend ↔ database, nginx ↔ frontend)
- ✅ Avoid time-consuming CI/CD failures
- ✅ Reduce "fix CI" commits in Git history
- ✅ Ensure each environment (local, staging, prod) works correctly

### 1.2 Scope

This guide covers:
- Manual validation commands
- Automated validation scripts
- Git pre-commit hooks
- GitHub Actions local testing with `act`
- Professional workflows and best practices

---

## 2. Why Test Locally

### 2.1 Common Issues Caught by Local Testing

| Issue Type | Example | Impact |
|------------|---------|--------|
| **Configuration Errors** | Wrong JDBC URL in compose file | Backend fails to connect to database |
| **Service Naming** | Using container name instead of service name | DNS resolution fails |
| **Port Conflicts** | Multiple environments using same port | Containers fail to start |
| **Missing Environment Variables** | `DB_USER_PASSWORD` not set | Deployment hangs indefinitely |
| **Health Check Failures** | Incorrect health endpoint | Containers marked unhealthy |
| **Build Context Issues** | Missing .dockerignore | Slow builds, large images |

### 2.2 Time and Cost Savings

**Without local testing:**
```
Edit config → Commit → Push → Wait 5-10 min → CI/CD fails → Check logs → Fix → Repeat
Total time: 15-30 minutes per iteration
```

**With local testing:**
```
Edit config → Test locally (2-5 min) → Fix issues → Commit → Push → CI/CD succeeds
Total time: 5-10 minutes once
```

**Benefits:**
- 66% faster iteration cycle
- Fewer failed deployments
- Cleaner Git history
- Lower CI/CD runner costs (if using paid runners)

---

## 3. Prerequisites

### 3.1 Required Software

| Software | Minimum Version | Purpose |
|----------|----------------|---------|
| Docker | 20.10+ | Run containers locally |
| Docker Compose | 1.29+ | Orchestrate multi-container environments |
| Git | 2.30+ | Version control |
| Make | 4.0+ (optional) | Simplified commands |
| Bash | 4.0+ | Run validation scripts |

### 3.2 Environment Setup

**Windows (PowerShell):**
```powershell
$env:DB_USER_PASSWORD="your_password"
```

**Linux/Mac (Bash):**
```bash
export DB_USER_PASSWORD="your_password"
```

**Persistent configuration (recommended):**
```bash
# Add to ~/.bashrc or ~/.zshrc
export DB_USER_PASSWORD="your_password"
```

### 3.3 Verification

```bash
docker --version
docker-compose --version
git --version
make --version  # Optional
bash --version
```

---

## 4. Testing Strategies

### 4.1 Strategy 1: Quick Syntax Validation

**Use case:** Verify YAML syntax after editing Docker Compose files

```bash
# Validate compose file syntax
docker-compose -f docker-compose.yml -f docker-compose.staging.yml config
```

**What it checks:**
- ✅ YAML syntax correctness
- ✅ Service definitions validity
- ✅ Environment variable interpolation
- ✅ Volume and network configurations

**Time:** < 5 seconds

### 4.2 Strategy 2: Manual Environment Testing

**Use case:** Full deployment test before committing

```bash
# Set environment variable
export DB_USER_PASSWORD="test"

# Deploy staging environment locally
docker-compose -p portfolio-stage \
  -f docker-compose.yml \
  -f docker-compose.staging.yml \
  up --build -d

# Wait 2-3 minutes for containers to become healthy
docker ps --filter "name=portfolio-.*-stage"

# Check health status
docker inspect portfolio-db-stage --format='{{.State.Health.Status}}'
docker inspect portfolio-backend-stage --format='{{.State.Health.Status}}'
docker inspect portfolio-frontend-stage --format='{{.State.Health.Status}}'

# Test endpoints
curl http://localhost:3000/health.json
docker exec portfolio-backend-stage curl http://localhost:8080/actuator/health

# Check logs for errors
docker logs portfolio-backend-stage
docker logs portfolio-frontend-stage

# Clean up
docker-compose -p portfolio-stage \
  -f docker-compose.yml \
  -f docker-compose.staging.yml \
  down -v
```

**What it checks:**
- ✅ Container build success
- ✅ Container startup
- ✅ Health check endpoints
- ✅ Service connectivity
- ✅ Port mapping

**Time:** 3-5 minutes

### 4.3 Strategy 3: Automated Validation Script

**Use case:** Comprehensive pre-commit validation

See [Section 5](#5-validation-scripts) for the full validation script.

```bash
# Make script executable (first time only)
chmod +x scripts/validate-deployment.sh

# Validate local environment
./scripts/validate-deployment.sh local

# Validate staging environment
./scripts/validate-deployment.sh staging

# Validate production environment
./scripts/validate-deployment.sh prod
```

**What it checks:**
- ✅ File existence
- ✅ YAML syntax
- ✅ Environment variables
- ✅ Container build and startup
- ✅ Health check monitoring
- ✅ Functional smoke tests (4 tests)
- ✅ End-to-end integration

**Time:** 3-5 minutes

### 4.4 Strategy 4: Makefile Commands

**Use case:** Quick, memorizable commands

```bash
# Validate staging before commit
make validate-staging

# Quick start without validation
make test-staging

# View logs
make logs-staging

# Clean up
make clean-staging

# Check all running containers
make status
```

**What it provides:**
- ✅ Simplified command interface
- ✅ Consistent naming
- ✅ Easy to remember
- ✅ Tab completion support

**Time:** 3-5 minutes (validate), < 1 minute (start/stop)

### 4.5 Strategy 5: Git Pre-Commit Hooks

**Use case:** Automatic validation on every commit

Pre-commit hooks run automatically before each commit and can prevent commits if validation fails.

See [Section 5.2](#52-git-pre-commit-hook) for implementation.

**What it checks:**
- ✅ Automatically triggered on commit
- ✅ Only runs if Docker files changed
- ✅ Interactive (ask user to validate)
- ✅ Prevents broken commits

**Time:** 3-5 minutes (if Docker files changed)

### 4.6 Strategy 6: GitHub Actions Local Testing (act)

**Use case:** Test full CI/CD workflow locally

`act` allows you to run GitHub Actions workflows on your local machine.

```bash
# Install act (Windows with Chocolatey)
choco install act-cli

# Install act (Mac with Homebrew)
brew install act

# Install act (Linux)
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# List available jobs
act -l

# Run build-and-test job
act -j build-and-test

# Simulate push to staging
act push -e .github/workflows/events/push-staging.json

# Run with secrets
act -s DB_PASSWORD=test
```

**Limitations:**
- ⚠️ Does not fully support self-hosted runners
- ⚠️ Some GitHub-specific actions may fail
- ⚠️ Can be slow on Windows
- ⚠️ Requires Docker-in-Docker (large images)

**What it checks:**
- ✅ Full workflow execution
- ✅ Multi-job dependencies
- ✅ Environment variables and secrets
- ✅ Build artifacts

**Time:** 10-15 minutes

---

## 5. Validation Scripts

### 5.1 Complete Validation Script

Save as `scripts/validate-deployment.sh`:

```bash
#!/bin/bash
# Pre-commit validation script for Docker Compose configurations
# Usage: ./scripts/validate-deployment.sh [local|staging|prod]

set -e

ENV=${1:-local}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "========================================="
echo "Validating environment: $ENV"
echo "========================================="

# Environment-specific configuration
case $ENV in
  local)
    COMPOSE_FILE="docker-compose.local.yml"
    PROJECT_NAME="portfolio-local"
    PORT=8081
    DB_PORT=5435
    DB_NAME="portfolio_local"
    ;;
  staging)
    COMPOSE_FILE="docker-compose.staging.yml"
    PROJECT_NAME="portfolio-stage"
    PORT=3000
    DB_PORT=5434
    DB_NAME="portfolio_staging"
    ;;
  prod)
    COMPOSE_FILE="docker-compose.prod.yml"
    PROJECT_NAME="portfolio-prod"
    PORT=80
    DB_PORT=5432
    DB_NAME="portfolio_prod"
    ;;
  *)
    echo "❌ Invalid environment: $ENV (local|staging|prod)"
    exit 1
    ;;
esac

cd "$PROJECT_ROOT"

# Check 1: Docker Compose files exist
echo ""
echo "✓ Checking Docker Compose files..."
if [ ! -f "docker-compose.yml" ]; then
  echo "❌ docker-compose.yml not found"
  exit 1
fi
if [ ! -f "$COMPOSE_FILE" ]; then
  echo "❌ $COMPOSE_FILE not found"
  exit 1
fi
echo "✅ Docker Compose files present"

# Check 2: Validate YAML syntax
echo ""
echo "✓ Validating YAML syntax..."
docker-compose -f docker-compose.yml -f "$COMPOSE_FILE" config > /dev/null
echo "✅ YAML syntax valid"

# Check 3: Verify environment variables
echo ""
echo "✓ Checking environment variables..."
if [ -z "$DB_USER_PASSWORD" ]; then
  echo "⚠️  DB_USER_PASSWORD not set, using 'test' for validation"
  export DB_USER_PASSWORD="test"
fi
echo "✅ Environment variables configured"

# Check 4: Clean existing containers
echo ""
echo "✓ Cleaning existing containers..."
docker ps -a --filter "name=portfolio-.*-${ENV}" --format "{{.Names}}" | xargs -r docker stop 2>/dev/null || true
docker ps -a --filter "name=portfolio-.*-${ENV}" --format "{{.Names}}" | xargs -r docker rm 2>/dev/null || true
echo "✅ Environment cleaned"

# Check 5: Build and start containers
echo ""
echo "✓ Building and starting containers..."
docker-compose -p "$PROJECT_NAME" -f docker-compose.yml -f "$COMPOSE_FILE" up --build -d

# Check 6: Wait for containers to be healthy
echo ""
echo "✓ Waiting for containers to be healthy (max 180s)..."
MAX_ATTEMPTS=36
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  ATTEMPT=$((ATTEMPT + 1))

  # Get health status
  DB_HEALTH=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}running{{end}}' portfolio-db-${ENV} 2>/dev/null || echo "not_running")
  BACKEND_HEALTH=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}running{{end}}' portfolio-backend-${ENV} 2>/dev/null || echo "not_running")
  FRONTEND_HEALTH=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}running{{end}}' portfolio-frontend-${ENV} 2>/dev/null || echo "not_running")
  NGINX_RUNNING=$(docker ps --filter "name=portfolio-nginx-${ENV}" --format "{{.Names}}" 2>/dev/null)

  if [ -n "$NGINX_RUNNING" ]; then
    NGINX_HEALTH="running"
  else
    NGINX_HEALTH="not_running"
  fi

  echo "[$ATTEMPT/$MAX_ATTEMPTS] DB: $DB_HEALTH | Backend: $BACKEND_HEALTH | Frontend: $FRONTEND_HEALTH | Nginx: $NGINX_HEALTH"

  # Check if all are healthy
  if [ "$DB_HEALTH" = "healthy" ] && [ "$BACKEND_HEALTH" = "healthy" ] && [ "$FRONTEND_HEALTH" = "healthy" ] && [ "$NGINX_HEALTH" = "running" ]; then
    echo "✅ All containers are healthy!"
    break
  fi

  # Check for unhealthy status
  if [ "$DB_HEALTH" = "unhealthy" ] || [ "$BACKEND_HEALTH" = "unhealthy" ] || [ "$FRONTEND_HEALTH" = "unhealthy" ]; then
    echo "❌ One or more containers are unhealthy!"
    echo ""
    echo "Container logs:"
    [ "$DB_HEALTH" = "unhealthy" ] && docker logs --tail=50 portfolio-db-${ENV}
    [ "$BACKEND_HEALTH" = "unhealthy" ] && docker logs --tail=50 portfolio-backend-${ENV}
    [ "$FRONTEND_HEALTH" = "unhealthy" ] && docker logs --tail=50 portfolio-frontend-${ENV}
    exit 1
  fi

  if [ "$DB_HEALTH" = "not_running" ] || [ "$BACKEND_HEALTH" = "not_running" ] || [ "$FRONTEND_HEALTH" = "not_running" ] || [ "$NGINX_HEALTH" = "not_running" ]; then
    echo "⚠️ Some containers are not running yet, waiting..."
    docker ps -a --filter "name=portfolio-.*-${ENV}"
  fi

  sleep 5
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
  echo "❌ Timeout: containers did not become healthy"
  echo ""
  echo "Final status - DB: $DB_HEALTH | Backend: $BACKEND_HEALTH | Frontend: $FRONTEND_HEALTH | Nginx: $NGINX_HEALTH"
  echo ""
  echo "Container status:"
  docker ps -a --filter "name=portfolio-.*-${ENV}"
  echo ""
  echo "Container logs:"
  docker logs --tail=50 portfolio-db-${ENV} 2>&1 || true
  docker logs --tail=50 portfolio-backend-${ENV} 2>&1 || true
  docker logs --tail=50 portfolio-frontend-${ENV} 2>&1 || true
  docker logs --tail=50 portfolio-nginx-${ENV} 2>&1 || true
  exit 1
fi

# Check 7: Functional smoke tests
echo ""
echo "✓ Running functional tests..."

# Test 1: Frontend health endpoint
echo "  - Testing frontend health endpoint..."
FRONTEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/health.json || echo "000")
if [ "$FRONTEND_STATUS" = "200" ]; then
  echo "    ✅ Frontend health OK (HTTP $FRONTEND_STATUS)"
else
  echo "    ❌ Frontend health FAILED (HTTP $FRONTEND_STATUS)"
  exit 1
fi

# Test 2: Backend actuator health endpoint
echo "  - Testing backend actuator health endpoint..."
BACKEND_STATUS=$(docker exec portfolio-backend-${ENV} curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || echo "000")
if [ "$BACKEND_STATUS" = "200" ]; then
  echo "    ✅ Backend health OK (HTTP $BACKEND_STATUS)"
else
  echo "    ❌ Backend health FAILED (HTTP $BACKEND_STATUS)"
  exit 1
fi

# Test 3: Database connectivity
echo "  - Testing database connectivity..."
DB_TEST=$(docker exec portfolio-db-${ENV} psql -U postgres_app -d $DB_NAME -c "SELECT 1;" 2>&1)
if echo "$DB_TEST" | grep -q "1 row"; then
  echo "    ✅ Database connectivity OK"
else
  echo "    ❌ Database connectivity FAILED"
  echo "$DB_TEST"
  exit 1
fi

# Test 4: Full stack integration
echo "  - Testing full stack integration..."
STACK_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/ || echo "000")
if [ "$STACK_STATUS" = "200" ] || [ "$STACK_STATUS" = "304" ]; then
  echo "    ✅ Full stack OK (HTTP $STACK_STATUS)"
else
  echo "    ❌ Full stack FAILED (HTTP $STACK_STATUS)"
  exit 1
fi

# Check 8: Final summary
echo ""
echo "========================================="
echo "✅ Validation successful for $ENV!"
echo "========================================="
echo ""
echo "Running containers:"
docker ps --filter "name=portfolio-.*-${ENV}" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "To clean up:"
echo "  docker-compose -p $PROJECT_NAME -f docker-compose.yml -f $COMPOSE_FILE down"
echo ""
echo "To view logs:"
echo "  docker logs -f portfolio-backend-${ENV}"
echo "  docker logs -f portfolio-frontend-${ENV}"
echo ""
```

**Make it executable:**
```bash
chmod +x scripts/validate-deployment.sh
```

### 5.2 Git Pre-Commit Hook

Save as `.git/hooks/pre-commit`:

```bash
#!/bin/bash
# Git pre-commit hook to validate Docker Compose changes
# Runs automatically before each commit

# Check if any Docker Compose files were modified
DOCKER_FILES=$(git diff --cached --name-only | grep -E "(docker-compose|Dockerfile)")

if [ -z "$DOCKER_FILES" ]; then
  # No Docker files modified, skip validation
  exit 0
fi

echo "========================================="
echo "Modified Docker files detected:"
echo "$DOCKER_FILES"
echo "========================================="
echo ""

# Ask user if they want to validate
read -p "Validate Docker configuration before commit? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  echo "Validation skipped. Proceeding with commit..."
  exit 0
fi

# Run validation on local environment
echo ""
echo "Running local validation..."
./scripts/validate-deployment.sh local

if [ $? -ne 0 ]; then
  echo ""
  echo "❌ Validation failed!"
  echo "Commit aborted."
  echo ""
  echo "To bypass this check, use:"
  echo "  git commit --no-verify"
  exit 1
fi

echo ""
echo "✅ Validation successful! Proceeding with commit..."
exit 0
```

**Make it executable:**
```bash
chmod +x .git/hooks/pre-commit
```

### 5.3 Makefile for Simplified Commands

Save as `Makefile` in project root:

```makefile
.PHONY: help validate-local validate-staging validate-prod test-local test-staging test-prod clean-local clean-staging clean-prod status

help:
	@echo "Available commands for local deployment testing:"
	@echo ""
	@echo "  make validate-local     - Validate local configuration"
	@echo "  make validate-staging   - Validate staging configuration"
	@echo "  make validate-prod      - Validate production configuration"
	@echo ""
	@echo "  make test-local         - Start local environment"
	@echo "  make test-staging       - Start staging environment"
	@echo "  make test-prod          - Start production environment"
	@echo ""
	@echo "  make clean-local        - Clean local environment"
	@echo "  make clean-staging      - Clean staging environment"
	@echo "  make clean-prod         - Clean production environment"
	@echo ""
	@echo "Examples:"
	@echo "  make validate-staging   # Validate staging before commit"
	@echo "  make test-staging       # Start staging locally"
	@echo "  make clean-staging      # Clean staging"

# Full validation with tests
validate-local:
	@chmod +x scripts/validate-deployment.sh
	@scripts/validate-deployment.sh local

validate-staging:
	@chmod +x scripts/validate-deployment.sh
	@scripts/validate-deployment.sh staging

validate-prod:
	@chmod +x scripts/validate-deployment.sh
	@scripts/validate-deployment.sh prod

# Quick start without validation
test-local:
	@echo "Starting local environment..."
	@DB_USER_PASSWORD=${DB_USER_PASSWORD:-test} docker-compose -p portfolio-local -f docker-compose.yml -f docker-compose.local.yml up --build -d
	@echo "✅ Local environment started on http://localhost:8081"

test-staging:
	@echo "Starting staging environment..."
	@DB_USER_PASSWORD=${DB_USER_PASSWORD:-test} docker-compose -p portfolio-stage -f docker-compose.yml -f docker-compose.staging.yml up --build -d
	@echo "✅ Staging environment started on http://localhost:3000"

test-prod:
	@echo "Starting production environment..."
	@DB_USER_PASSWORD=${DB_USER_PASSWORD:-test} docker-compose -p portfolio-prod -f docker-compose.yml -f docker-compose.prod.yml up --build -d
	@echo "✅ Production environment started on http://localhost:80"

# Cleanup
clean-local:
	@echo "Cleaning local environment..."
	@docker-compose -p portfolio-local -f docker-compose.yml -f docker-compose.local.yml down -v
	@echo "✅ Local environment cleaned"

clean-staging:
	@echo "Cleaning staging environment..."
	@docker-compose -p portfolio-stage -f docker-compose.yml -f docker-compose.staging.yml down -v
	@echo "✅ Staging environment cleaned"

clean-prod:
	@echo "Cleaning production environment..."
	@docker-compose -p portfolio-prod -f docker-compose.yml -f docker-compose.prod.yml down -v
	@echo "✅ Production environment cleaned"

# Clean all environments
clean-all: clean-local clean-staging clean-prod
	@echo "✅ All environments cleaned"

# View logs
logs-local:
	@docker-compose -p portfolio-local -f docker-compose.yml -f docker-compose.local.yml logs -f

logs-staging:
	@docker-compose -p portfolio-stage -f docker-compose.yml -f docker-compose.staging.yml logs -f

logs-prod:
	@docker-compose -p portfolio-prod -f docker-compose.yml -f docker-compose.prod.yml logs -f

# Check container status
status:
	@echo "Running containers:"
	@docker ps --filter "name=portfolio-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

---

## 6. Professional Best Practices

### 6.1 Recommended Development Workflow

```
Local Development → Local Testing → Commit → CI/CD on Staging → Review → Merge to Main → Production Deploy
```

**Before every commit:**

1. **Validate configuration**
   ```bash
   make validate-staging
   ```

2. **Test locally with staging environment**
   ```bash
   make test-staging
   ```

3. **Check logs for errors**
   ```bash
   docker logs -f portfolio-backend-stage
   ```

4. **Run smoke tests manually**
   ```bash
   curl http://localhost:3000/health.json
   curl http://localhost:3000/
   ```

5. **Clean up**
   ```bash
   make clean-staging
   ```

6. **Commit only if everything works**
   ```bash
   git add .
   git commit -m "fix: update backend configuration"
   ```

### 6.2 Environment Testing Matrix

| Change Type | Test Local | Test Staging | Test Production |
|-------------|------------|--------------|-----------------|
| Backend code | ✅ | ✅ | Optional (rarely needed) |
| Frontend code | ✅ | ✅ | Optional |
| Docker Compose files | ✅ | ✅ | ✅ (read-only validation) |
| Database migrations | ✅ | ✅ | ✅ (with backup!) |
| Nginx configuration | ✅ | ✅ | ✅ |
| Environment variables | ✅ | ✅ | ✅ |

### 6.3 Git Branching Strategy

```
main (production)
  ↑
  └── staging (pre-production)
       ↑
       └── develop (integration)
            ↑
            ├── feature/user-auth
            ├── feature/dashboard
            └── bugfix/login-error
```

**Testing at each level:**

1. **Feature branch** → Test locally before creating PR
2. **Develop branch** → CI/CD builds (no deploy)
3. **Staging branch** → CI/CD deploys to staging, test manually
4. **Main branch** → CI/CD deploys to production, run smoke tests

### 6.4 Commit Message Best Practices

When committing configuration changes:

```bash
# Good commit messages
git commit -m "fix: correct JDBC URL in staging compose file"
git commit -m "feat: add health check to frontend container"
git commit -m "refactor: consolidate environment variables"

# Bad commit messages
git commit -m "fix ci"
git commit -m "update docker"
git commit -m "wip"
```

### 6.5 Team Workflow Recommendations

**For solo developers:**
- Test staging locally before every commit touching Docker files
- Use pre-commit hooks for automatic validation
- Keep staging and main branches clean

**For small teams (2-5 developers):**
- Require local validation before PR creation
- Use pull request template with testing checklist
- Designate one person as deployment owner per sprint
- Review Docker Compose changes carefully

**For larger teams (6+ developers):**
- Implement required CI/CD checks on all PRs
- Use branch protection rules
- Require 1-2 reviewers for infrastructure changes
- Maintain separate staging environment per team/feature
- Schedule regular deployment windows

### 6.6 Documentation Standards

**Always document:**
- New environment variables in README
- Port changes in deployment docs
- Database schema changes in migrations docs
- Breaking changes in CHANGELOG

**Example:**
```markdown
## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DB_USER_PASSWORD` | Yes | - | PostgreSQL user password |
| `SPRING_PROFILES_ACTIVE` | No | `dev` | Active Spring profile |
```

### 6.7 Monitoring and Alerting

**Set up local monitoring:**

```bash
# Watch container status in real-time
watch -n 2 'docker ps --filter "name=portfolio-" --format "table {{.Names}}\t{{.Status}}"'

# Monitor resource usage
docker stats --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"

# Tail all logs
docker-compose -p portfolio-stage logs -f
```

**Production monitoring checklist:**
- [ ] Health check endpoints configured
- [ ] Prometheus metrics exposed (if applicable)
- [ ] Log aggregation configured
- [ ] Alert rules defined for critical services
- [ ] Uptime monitoring enabled

### 6.8 Security Best Practices

**Never commit secrets:**
```bash
# Add to .gitignore
.env
.env.local
.env.production
secrets/
*.pem
*.key
```

**Use environment-specific secrets:**
- Local: `.env.local` (not in Git)
- Staging: GitHub Secrets
- Production: GitHub Secrets + Secret Manager

**Validate security before deploy:**
```bash
# Check for exposed secrets
git diff --cached | grep -E "(password|secret|key|token)"

# Scan Docker images
docker scan portfolio-backend:latest
```

---

## 7. Troubleshooting

### 7.1 Validation Script Fails

#### Issue: "docker-compose.yml not found"

**Cause:** Script executed from wrong directory

**Solution:**
```bash
# Always run from project root
cd /path/to/portfolio
./scripts/validate-deployment.sh staging
```

#### Issue: "YAML syntax invalid"

**Cause:** Malformed YAML in compose files

**Solution:**
```bash
# Identify syntax error
docker-compose -f docker-compose.yml -f docker-compose.staging.yml config

# Common issues:
# - Incorrect indentation (use spaces, not tabs)
# - Missing colons after keys
# - Unquoted special characters
```

#### Issue: "DB_USER_PASSWORD not set"

**Cause:** Environment variable not exported

**Solution:**
```bash
# Set temporarily
export DB_USER_PASSWORD="test"

# Set permanently (add to ~/.bashrc)
echo 'export DB_USER_PASSWORD="test"' >> ~/.bashrc
source ~/.bashrc
```

### 7.2 Containers Won't Start

#### Issue: "Port already allocated"

**Cause:** Another container or process using the port

**Solution:**
```bash
# Find process using port 3000 (staging)
lsof -i :3000

# Stop conflicting containers
docker stop $(docker ps -q --filter "name=portfolio")

# Or kill the process
kill -9 <PID>
```

#### Issue: "Container startup timeout"

**Cause:** Build is slow or container failing to start

**Solution:**
```bash
# Check build progress
docker-compose -p portfolio-stage -f docker-compose.yml -f docker-compose.staging.yml up --build

# View real-time logs
docker logs -f portfolio-backend-stage

# Common causes:
# - Slow npm install → Add .dockerignore
# - Gradle downloads dependencies → Check internet connection
# - Database migration fails → Check database logs
```

### 7.3 Health Checks Failing

#### Issue: "Container is unhealthy"

**Cause:** Health check endpoint not responding

**Solution:**
```bash
# Test health check manually
docker exec portfolio-backend-stage curl http://localhost:8080/actuator/health

# Check if service is listening
docker exec portfolio-backend-stage netstat -tlnp

# Common causes:
# - Wrong health check URL
# - Service crashed on startup
# - Service listening on wrong port
```

### 7.4 Smoke Tests Failing

#### Issue: "Frontend health check failed (HTTP 000)"

**Cause:** Nginx not routing correctly or frontend not built

**Solution:**
```bash
# Test from host
curl -v http://localhost:3000/health.json

# Test nginx config
docker exec portfolio-nginx-stage nginx -t

# Check if frontend files exist
docker exec portfolio-frontend-stage ls -la /usr/share/nginx/html/

# Common causes:
# - health.json not generated
# - Nginx config pointing to wrong directory
# - Frontend build failed
```

#### Issue: "Backend health check failed (HTTP 503)"

**Cause:** Spring Boot not fully started or database connection failed

**Solution:**
```bash
# Check Spring Boot logs
docker logs portfolio-backend-stage | grep -i error

# Test database connection from backend
docker exec portfolio-backend-stage env | grep SPRING_DATASOURCE

# Verify database is healthy
docker inspect portfolio-db-stage --format='{{.State.Health.Status}}'
```

### 7.5 Git Pre-Commit Hook Issues

#### Issue: "Hook not executing"

**Cause:** Hook file not executable

**Solution:**
```bash
chmod +x .git/hooks/pre-commit
```

#### Issue: "Validation runs even when I don't change Docker files"

**Cause:** Git detecting changes in Docker files unintentionally

**Solution:**
```bash
# Check what Git sees as changed
git diff --cached --name-only

# If Docker files shouldn't be in staging area
git reset docker-compose.yml
```

#### Issue: "Want to bypass hook temporarily"

**Solution:**
```bash
# Skip all hooks for one commit
git commit --no-verify -m "urgent hotfix"
```

### 7.6 Make Command Issues

#### Issue: "make: command not found"

**Cause:** Make not installed (common on Windows)

**Solution:**
```bash
# Windows (with Chocolatey)
choco install make

# Or use docker-compose commands directly
docker-compose -p portfolio-stage -f docker-compose.yml -f docker-compose.staging.yml up -d
```

#### Issue: "No rule to make target"

**Cause:** Makefile syntax error or missing target

**Solution:**
```bash
# List available targets
make help

# Check Makefile syntax (use tabs, not spaces for indentation)
cat -A Makefile  # Should show ^I for tabs
```

---

## Environment Port Reference

| Environment | Nginx | Backend (internal) | Frontend (internal) | Database (host) |
|-------------|-------|-------------------|---------------------|----------------|
| Local | 8081 | 8080 | 4200 | 5435 |
| Staging | 3000 | 8080 | 80 | 5434 |
| Production | 80 | 8080 | 80 | 5432 |

---

## Change History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-11-10 | Initial release: Complete local testing guide with validation scripts, best practices, and troubleshooting |

---

**Document Type:** Testing Guide
**Version:** 1.0.0
**Last Updated:** 2025-11-10
**Status:** Active