# Operations Documentation

Documentation for monitoring, health checks, and operational tasks.

---

## Contents

| Document | Description |
|----------|-------------|
| [Health Checks](./health-checks.md) | Application health endpoints, monitoring configuration |

---

## Health Endpoints

| Endpoint | Description | Auth Required |
|----------|-------------|---------------|
| `GET /actuator/health` | Basic health status | No |
| `GET /actuator/health/liveness` | Kubernetes liveness probe | No |
| `GET /actuator/health/readiness` | Kubernetes readiness probe | No |
| `GET /api/health/ping` | Simple ping endpoint | No |

---

## Quick Health Check

```bash
# Basic health check
curl http://localhost:8080/actuator/health

# Detailed health (with credentials)
curl http://localhost:8080/actuator/health -u admin:password
```

---

## Related Documentation

- [Deployment](../deployment/README.md) - CI/CD and deployment
- [Reference](../reference/README.md) - Configuration
- [Security](../security/README.md) - Authentication
