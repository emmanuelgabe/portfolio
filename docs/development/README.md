# Development Guide

Documentation for setting up the development environment and following project conventions.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Contents](#2-contents)
3. [Quick Start](#3-quick-start)
4. [Development Workflow](#4-development-workflow)
5. [Code Quality](#5-code-quality)

---

## 1. Overview

This section documents development processes, conventions, and best practices for the Portfolio application.

**Development Stack**:
- **Backend**: Java 21, Spring Boot 3.4+, Gradle
- **Frontend**: Angular 18, TypeScript 5.5, npm
- **Database**: PostgreSQL 17, Flyway migrations
- **Containers**: Docker, Docker Compose

---

## 2. Contents

| Document | Description |
|----------|-------------|
| [Setup Guide](./setup.md) | Development environment setup, prerequisites, IDE configuration |
| [Testing Guide](./testing-guide.md) | Testing strategy, conventions, execution commands, coverage |
| [Code Quality](./code-quality.md) | Checkstyle, SpotBugs, JaCoCo configuration |
| [Logging Conventions](./logging-conventions.md) | Log format, levels, best practices |
| [Logging Categories](./logging-categories.md) | Context categories and examples |
| [Git Workflow](./git-workflow.md) | Branch strategy, commit conventions, semantic versioning |

---

## 3. Quick Start

### 3.1 Prerequisites

- Java 21 LTS
- Node.js 20 LTS
- Docker Desktop
- PostgreSQL 17+ (or use Docker)

### 3.2 Backend Setup

```bash
cd portfolio-backend

# Build
./gradlew build

# Run
./gradlew bootRun

# Run tests
./gradlew test
```

### 3.3 Frontend Setup

```bash
cd portfolio-frontend

# Install dependencies
npm install

# Start development server
npm start

# Run tests
npm test -- --watch=false --browsers=ChromeHeadlessCI
```

### 3.4 Docker Development

```bash
# Start all services
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build -d

# View logs
docker-compose logs -f backend
```

---

## 4. Development Workflow

### 4.1 Branch Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Production-ready code |
| `staging` | Pre-production testing |
| `dev` | Active development |
| `feature/*` | New features |
| `fix/*` | Bug fixes |

### 4.2 Commit Conventions

**Format**: `type(scope): description`

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `refactor`: Code refactoring
- `test`: Tests
- `chore`: Maintenance

**Example**: `feat(articles): add markdown rendering`

### 4.3 Pull Request Process

1. Create feature branch from `dev`
2. Implement changes
3. Run tests and quality checks
4. Create PR to `dev`
5. Code review
6. Merge after approval

---

## 5. Code Quality

### 5.1 Backend Checks

```bash
# Run all quality checks
./gradlew check

# Individual checks
./gradlew checkstyleMain    # Code style
./gradlew spotbugsMain      # Static analysis
./gradlew test jacocoTestReport  # Coverage
```

### 5.2 Frontend Checks

```bash
# Lint
npm run lint

# Format
npm run format

# Test
npm test
```

### 5.3 Pre-commit Verification

```bash
# Backend
./gradlew check

# Frontend
npm run lint && npm test -- --watch=false
```

---

## Related Documentation

- [Architecture](../architecture/README.md) - System architecture
- [API Reference](../api/README.md) - API documentation
- [Features](../features/README.md) - Feature documentation
- [Deployment](../deployment/README.md) - CI/CD and deployment
- [Operations: Observability](../operations/observability.md) - Monitoring stack
