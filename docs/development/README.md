# Development Guide

Documentation for setting up the development environment and following project conventions.

---

## Contents

| Document | Description |
|----------|-------------|
| [Setup Guide](./setup.md) | Development environment setup, prerequisites, IDE configuration |
| [Testing Guide](./testing-guide.md) | Testing strategy, conventions, execution commands, coverage |
| [Code Quality](./code-quality.md) | Checkstyle, SpotBugs, JaCoCo configuration |
| [Logging Conventions](./logging-conventions.md) | Log format, categories, levels, best practices |
| [Articles Pagination](./articles-pagination-cleanup.md) | Articles feature pagination implementation |

---

## Quick Start

### Prerequisites

- Java 21 LTS
- Node.js 20 LTS
- Docker Desktop
- PostgreSQL 15+ (or use Docker)

### Setup Commands

```bash
# Backend
cd portfolio-backend
./gradlew build

# Frontend
cd portfolio-frontend
npm install
npm start
```

### Run Tests

```bash
# Backend tests
./gradlew test

# Frontend tests
npm test -- --watch=false --browsers=ChromeHeadlessCI
```

---

## Related Documentation

- [Architecture](../architecture/README.md) - System architecture
- [API Reference](../api/README.md) - API documentation
