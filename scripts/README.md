# Scripts Directory

This directory contains utility scripts for deployment, maintenance, testing, and documentation validation.

## Directory Structure

```
scripts/
├── deployment/              # Deployment and environment synchronization
├── maintenance/             # Cleanup and maintenance tasks
├── testing/                 # Health checks and validation
├── docs/                    # Documentation validation
└── README.md               # This file
```

## Scripts Overview

### 📦 Deployment (`deployment/`)

Scripts related to deployment and environment management.

| Script | Description | Usage |
|--------|-------------|-------|
| `validate-deployment.sh` | Validate Docker Compose configuration and deployment for any environment | `./deployment/validate-deployment.sh [local\|staging\|prod]` |
| `sync-prod-to-staging.sh` | Synchronize production database to staging environment | `./deployment/sync-prod-to-staging.sh` |

**Examples:**
```bash
# Validate staging environment before committing
./deployment/validate-deployment.sh staging

# Sync production data to staging
./deployment/sync-prod-to-staging.sh
```

### 🧹 Maintenance (`maintenance/`)

Scripts for cleanup and maintenance operations.

| Script | Description | Usage |
|--------|-------------|-------|
| `cleanup-staging.sh` | Emergency cleanup of staging containers | `./maintenance/cleanup-staging.sh` |

**Examples:**
```bash
# Clean up stuck staging containers
./maintenance/cleanup-staging.sh
```

### 🧪 Testing (`testing/`)

Scripts for testing and health checks.

| Script | Description | Usage |
|--------|-------------|-------|
| `test-health.sh` | Test health endpoints of all services | `./testing/test-health.sh [local\|staging\|prod]` |

**Examples:**
```bash
# Test staging health endpoints
./testing/test-health.sh staging

# Test local development health
./testing/test-health.sh local
```

### 📚 Documentation (`docs/`)

Scripts for documentation validation and quality checks.

| Script | Description | Usage |
|--------|-------------|-------|
| `validate-docs.sh` | Validate documentation style with Vale | `./docs/validate-docs.sh [path]` |

**Examples:**
```bash
# Validate all documentation
./docs/validate-docs.sh

# Validate specific directory
./docs/validate-docs.sh docs/deployment/
```

## Quick Reference

### Pre-commit Workflow

Before committing changes that affect Docker configurations:

```bash
# 1. Validate the deployment
./deployment/validate-deployment.sh staging

# 2. If successful, commit
git add .
git commit -m "fix: update configuration"
```

## Change History

| Date | Change |
|------|--------|
| 2025-11-10 | Organized scripts into categories (deployment, maintenance, testing, docs) |
| 2025-11-09 | Initial script collection |