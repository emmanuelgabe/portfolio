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

Reference guide for the scripts directory organization and usage patterns in the portfolio project.

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

**What it does:**
1. Validates Docker Compose YAML syntax
2. Checks environment variables
3. Cleans existing containers
4. Builds and starts containers
5. Monitors health checks (max 180s)
6. Runs functional smoke tests
7. Displays deployment summary

**Exit codes:**
- 0: Success
- 1: Validation failed

**Example:**
```bash
# Validate staging before commit
./scripts/deployment/validate-deployment.sh staging

# Validate all environments
for env in local staging prod; do
  ./scripts/deployment/validate-deployment.sh $env
done
```

#### sync-prod-to-staging.sh

**Purpose:** Synchronize production database to staging

**Usage:**
```bash
./scripts/deployment/sync-prod-to-staging.sh
```

**What it does:**
1. Checks that prod and staging containers are running
2. Prompts for confirmation
3. Exports production database to SQL backup
4. Terminates staging database connections
5. Drops and recreates staging database
6. Restores production data to staging
7. Displays statistics and cleanup info

**Safety features:**
- Confirmation prompt before execution
- Automatic backup with timestamp
- Retention of last 5 backups
- Connection termination before drop

**Example:**
```bash
# Sync production data to staging
./scripts/deployment/sync-prod-to-staging.sh

# Backups saved in: /tmp/portfolio-backups/
```

---

### 3.2 Maintenance Scripts

**Location:** `scripts/maintenance/`

#### cleanup-staging.sh

**Purpose:** Emergency cleanup for stuck staging containers

**Usage:**
```bash
./scripts/maintenance/cleanup-staging.sh
```

**What it does:**
1. Stops all staging containers
2. Removes all staging containers
3. Cleans up containers with old naming patterns
4. Prunes dangling images
5. Displays current container status

**When to use:**
- Deployment failed and containers are stuck
- Need to force cleanup before redeployment
- Containers in unhealthy state

**Example:**
```bash
# Clean up staging before manual deployment
./scripts/maintenance/cleanup-staging.sh

# Then redeploy
make validate-staging
```

---

### 3.3 Testing Scripts

**Location:** `scripts/testing/`

#### test-health.sh

**Purpose:** Comprehensive health check for all environments

**Usage:**
```bash
./scripts/testing/test-health.sh [local|staging|prod]
```

**What it tests:**
1. Nginx health endpoint
2. Frontend static health
3. Backend ping endpoint
4. Backend actuator health
5. Database connectivity
6. Full health chain

**Output:**
- Color-coded results (green=pass, red=fail)
- Test summary with pass/fail count
- Exit code 0 if all pass, 1 if any fail

**Example:**
```bash
# Test local environment
./scripts/testing/test-health.sh local

# Test after deployment
./scripts/testing/test-health.sh staging
./scripts/testing/test-health.sh prod
```

#### run-all-tests.sh

**Purpose:** Run all test suites (backend + frontend + integration)

**Usage:**
```bash
./scripts/testing/run-all-tests.sh
```

**What it runs:**
1. Backend unit tests (Gradle)
2. Backend integration tests
3. Frontend unit tests (Karma)
4. Frontend e2e tests (Cypress - if configured)
5. Health checks

---

### 3.4 Documentation Scripts

**Location:** `scripts/docs/`

#### validate-docs.sh

**Purpose:** Validate documentation with Vale

**Usage:**
```bash
./scripts/validate-docs.sh [path]
```

**What it does:**
1. Checks if Vale is installed
2. Verifies .vale.ini exists
3. Downloads Google style guide if missing
4. Runs Vale on specified path
5. Reports suggestions and warnings

**Example:**
```bash
# Validate all documentation
./scripts/docs/validate-docs.sh docs/

# Validate specific file
./scripts/docs/validate-docs.sh README.md

# Validate before commit
./scripts/docs/validate-docs.sh .
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

**What it does:**
1. Finds all .sh files in scripts directory
2. Makes them executable (chmod +x)
3. Sets permissions on pre-commit hook
4. Displays script structure

**When to use:**
- After cloning repository
- After adding new scripts
- If permission denied errors occur

---

## 4. Usage Guide

### 4.1 Common Workflows

**Before committing:**
```bash
# Validate deployment
./scripts/deployment/validate-deployment.sh local

# Validate documentation
./scripts/docs/validate-docs.sh docs/
```

**Before merging to staging:**
```bash
# Run all tests
./scripts/testing/run-all-tests.sh

# Validate staging deployment
./scripts/deployment/validate-deployment.sh staging
```

**After deployment:**
```bash
# Check health
./scripts/testing/test-health.sh staging

# If issues, clean up
./scripts/maintenance/cleanup-staging.sh
```

**Sync data to staging:**
```bash
# Copy prod data to staging
./scripts/deployment/sync-prod-to-staging.sh
```

### 4.2 Integration with Makefile

Scripts are integrated into Makefile for convenience:

```bash
# Using Make (recommended)
make validate-local     # Calls validate-deployment.sh local
make validate-staging   # Calls validate-deployment.sh staging
make clean-staging      # Calls cleanup-staging.sh (simplified)

# Direct script usage (more control)
./scripts/deployment/validate-deployment.sh local
./scripts/maintenance/cleanup-staging.sh
```

### 4.3 Integration with CI/CD

Scripts used in GitHub Actions workflows:

**ci-cd.yml:**
- Uses health check logic similar to validate-deployment.sh
- Runs smoke tests (frontend, backend, database, full stack)

**health-check.yml:**
- Calls test-health.sh for comprehensive validation

**vale-docs.yml:**
- Uses Vale configuration from .vale.ini

---

## 5. Adding New Scripts

### 5.1 Guidelines

1. **Choose appropriate directory:**
   - Deployment → `scripts/deployment/`
   - Testing → `scripts/testing/`
   - Maintenance → `scripts/maintenance/`
   - Documentation → `scripts/docs/`

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
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Main logic here
echo -e "${BLUE}Script started${NC}"

# Exit with appropriate code
exit 0
```

4. **Make executable:**

```bash
chmod +x scripts/category/my-script.sh
```

5. **Add to documentation:**
   - Update this file (scripts-organization.md)
   - Add usage example to README.md if user-facing

6. **Add to Makefile (optional):**

```makefile
my-command:
	@chmod +x scripts/category/my-script.sh
	@scripts/category/my-script.sh
```

### 5.2 Best Practices

- **Use color coding:** Green for success, red for errors, yellow for warnings
- **Add help text:** Show usage when script is called without arguments
- **Validate inputs:** Check for required arguments and environment variables
- **Exit codes:** 0 for success, non-zero for errors
- **Error handling:** Use `set -e` or manual error checking
- **Comments:** Explain non-obvious logic
- **Idempotency:** Scripts should be safe to run multiple times
- **Logging:** Echo important steps and results

### 5.3 Testing New Scripts

Before committing new scripts:

1. Test locally
2. Test in Docker container if applicable
3. Test all error paths
4. Add to CI/CD if needed
5. Update documentation

---

## 6. Script Dependencies

### 6.1 Required Tools

Scripts assume these tools are available:

| Tool | Purpose | Scripts Using It |
|------|---------|------------------|
| bash | Shell execution | All scripts |
| docker | Container management | All deployment/testing scripts |
| docker-compose | Multi-container orchestration | All deployment scripts |
| curl | HTTP requests | Health check scripts |
| psql | Database operations | Database scripts |
| vale | Documentation validation | validate-docs.sh |

### 6.2 Optional Tools

| Tool | Purpose | Benefit |
|------|---------|---------|
| jq | JSON parsing | Better health check parsing |
| make | Task automation | Simplified commands |
| git | Version control | Version extraction |

---

## 7. Troubleshooting Scripts

### 7.1 Permission Denied

```bash
# Fix permissions
./scripts/setup-permissions.sh

# Or manually
chmod +x scripts/**/*.sh
```

### 7.2 Script Not Found

```bash
# Ensure running from project root
cd /path/to/portfolio

# Check script exists
ls -la scripts/deployment/validate-deployment.sh
```

### 7.3 Environment Variables

```bash
# Check required variables
echo $DB_USER_PASSWORD

# Set if missing
export DB_USER_PASSWORD=your_password

# Or use .env file
echo "DB_USER_PASSWORD=your_password" > .env
```

---

## 8. Security Considerations

### 8.1 Sensitive Data

- Never hardcode passwords in scripts
- Use environment variables or .env files
- Add .env to .gitignore

### 8.2 Script Execution

- Review scripts before running with sudo
- Validate inputs to prevent injection
- Use quotes around variables: `"$VAR"`

---

## 9. Quick Reference

### 9.1 Command Cheat Sheet

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

# Database
./scripts/deployment/sync-prod-to-staging.sh
```

### 9.2 Make Commands

```bash
make validate-local
make validate-staging
make validate-prod
make test-local
make clean-local
make status
```
