# Scripts Organization Reference

---

## Table of Contents

1. [Overview](#1-overview)
2. [Directory Structure](#2-directory-structure)
3. [Script Categories](#3-script-categories)
4. [Usage Guide](#4-usage-guide)
5. [Adding New Scripts](#5-adding-new-scripts)

---

## 1. Overview

Reference guide for the scripts directory organization and usage patterns.

### 1.1 Purpose

Scripts are organized by function to facilitate:
- Automated deployment validation
- Health check monitoring
- Maintenance operations
- Documentation validation
- Development workflow automation

---

## 2. Directory Structure

```
scripts/
├── deployment/          # Deployment and validation scripts
│   ├── validate-deployment.sh
│   └── sync-prod-to-staging.sh
├── maintenance/         # Cleanup and maintenance scripts
│   └── cleanup-staging.sh
├── testing/            # Health checks and testing scripts
│   ├── test-health.sh
│   └── run-all-tests.sh
├── docs/               # Documentation validation scripts
│   └── validate-docs.sh
└── setup-permissions.sh # Initial setup script
```

---

## 3. Script Categories

### 3.1 Deployment Scripts

**Location:** `scripts/deployment/`

#### validate-deployment.sh

**Purpose:** Pre-deployment validation for all environments

**Usage:**
```bash
./scripts/deployment/validate-deployment.sh [local|staging|prod]
```

**Steps:**
1. Validates Docker Compose YAML syntax
2. Checks environment variables
3. Cleans existing containers
4. Builds and starts containers
5. Monitors health checks (max 180s)
6. Runs functional smoke tests
7. Displays deployment summary

#### sync-prod-to-staging.sh

**Purpose:** Synchronize production database to staging

**Usage:**
```bash
./scripts/deployment/sync-prod-to-staging.sh
```

**Safety Features:**
- Confirmation prompt before execution
- Automatic backup with timestamp
- Retention of last 5 backups

---

### 3.2 Maintenance Scripts

**Location:** `scripts/maintenance/`

#### cleanup-staging.sh

**Purpose:** Emergency cleanup for stuck staging containers

**Usage:**
```bash
./scripts/maintenance/cleanup-staging.sh
```

**When to use:**
- Deployment failed and containers are stuck
- Need to force cleanup before redeployment
- Containers in unhealthy state

---

### 3.3 Testing Scripts

**Location:** `scripts/testing/`

#### test-health.sh

**Purpose:** Comprehensive health check for all environments

**Usage:**
```bash
./scripts/testing/test-health.sh [local|staging|prod]
```

**Tests:**
- Nginx health endpoint
- Frontend static health
- Backend ping endpoint
- Backend actuator health
- Database connectivity
- Full health chain

#### run-all-tests.sh

**Purpose:** Run all test suites (backend + frontend + integration)

**Usage:**
```bash
./scripts/testing/run-all-tests.sh
```

---

### 3.4 Documentation Scripts

**Location:** `scripts/docs/`

#### validate-docs.sh

**Purpose:** Validate documentation with Vale

**Usage:**
```bash
./scripts/docs/validate-docs.sh [path]
```

---

### 3.5 Setup Scripts

**Location:** `scripts/`

#### setup-permissions.sh

**Purpose:** Make all scripts executable

**Usage:**
```bash
./scripts/setup-permissions.sh
```

**When to use:**
- After cloning repository
- After adding new scripts
- If permission denied errors occur

---

## 4. Usage Guide

### 4.1 Common Workflows

**Before committing:**
```bash
./scripts/deployment/validate-deployment.sh local
./scripts/docs/validate-docs.sh docs/
```

**Before merging to staging:**
```bash
./scripts/testing/run-all-tests.sh
./scripts/deployment/validate-deployment.sh staging
```

**After deployment:**
```bash
./scripts/testing/test-health.sh staging
```

### 4.2 Integration with Makefile

```bash
# Using Make (recommended)
make validate-local     # Calls validate-deployment.sh local
make validate-staging   # Calls validate-deployment.sh staging
make clean-staging      # Calls cleanup-staging.sh
```

### 4.3 Integration with CI/CD

Scripts used in GitHub Actions workflows:
- `ci-cd.yml` - Uses health check logic similar to validate-deployment.sh
- `health-check.yml` - Calls test-health.sh
- `vale-docs.yml` - Uses Vale configuration

---

## 5. Adding New Scripts

### 5.1 Guidelines

1. **Choose appropriate directory:**
   - Deployment: `scripts/deployment/`
   - Testing: `scripts/testing/`
   - Maintenance: `scripts/maintenance/`
   - Documentation: `scripts/docs/`

2. **Naming convention:**
   - Use kebab-case: `my-script.sh`
   - Be descriptive: `cleanup-staging.sh` not `clean.sh`

3. **Script template:**

```bash
#!/bin/bash
# Brief description of script purpose
# Usage: ./script-name.sh [arguments]

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

# Main logic here
echo -e "${GREEN}Script completed${NC}"
exit 0
```

4. **Make executable:**
```bash
chmod +x scripts/category/my-script.sh
```

### 5.2 Best Practices

- Use color coding (green=success, red=errors)
- Add help text when called without arguments
- Validate inputs
- Exit codes: 0 for success, non-zero for errors
- Use `set -e` for error handling
- Scripts should be idempotent (safe to run multiple times)

---

## 6. Script Dependencies

### 6.1 Required Tools

| Tool | Purpose | Scripts Using It |
|------|---------|------------------|
| bash | Shell execution | All scripts |
| docker | Container management | Deployment/testing |
| docker-compose | Multi-container orchestration | Deployment |
| curl | HTTP requests | Health check |
| vale | Documentation validation | validate-docs.sh |

### 6.2 Optional Tools

| Tool | Purpose |
|------|---------|
| jq | JSON parsing |
| make | Task automation |

---

## 7. Quick Reference

### 7.1 Command Cheat Sheet

```bash
# Setup
./scripts/setup-permissions.sh

# Validation
./scripts/deployment/validate-deployment.sh local
./scripts/deployment/validate-deployment.sh staging
./scripts/deployment/validate-deployment.sh prod

# Testing
./scripts/testing/test-health.sh local
./scripts/testing/run-all-tests.sh

# Maintenance
./scripts/maintenance/cleanup-staging.sh

# Documentation
./scripts/docs/validate-docs.sh docs/
```

### 7.2 Make Commands

```bash
make validate-local
make validate-staging
make validate-prod
make test-local
make clean-local
make status
```

---

## Related Documentation

- [Development: Setup](../development/setup.md) - Development environment setup
- [Deployment: CI/CD](../deployment/ci-cd.md) - CI/CD pipeline
- [Reference: Environments](./environments.md) - Environment configuration
