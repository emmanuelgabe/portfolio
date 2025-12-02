# Deployment Documentation

Guides for CI/CD pipelines and deployment processes.

---

## Contents

| Document | Description |
|----------|-------------|
| [CI/CD Guide](./ci-cd.md) | GitHub Actions workflows, pipeline stages, deployment process |
| [Local Testing](./local-testing.md) | Local deployment testing with Docker Compose |

---

## Deployment Environments

| Environment | Branch | Trigger | URL |
|-------------|--------|---------|-----|
| Staging | `staging` | Push to staging | staging.example.com |
| Production | `main` | Push to main | app.example.com |

---

## Quick Commands

### Local Deployment

```bash
# Start all services
docker-compose -f docker-compose.local.yml up --build -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

### Production Deployment

```bash
# Build production images
docker-compose -f docker-compose.prod.yml build

# Deploy
docker-compose -f docker-compose.prod.yml up -d
```

---

## Related Documentation

- [Development Guide](../development/README.md) - Development setup
- [Reference](../reference/README.md) - Configuration and environments
- [Operations](../operations/README.md) - Health checks and monitoring
