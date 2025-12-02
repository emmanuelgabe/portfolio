# Reference Documentation

Technical reference documentation for configuration, dependencies, and error codes.

---

## Contents

| Document | Description |
|----------|-------------|
| [Configuration Properties](./configuration-properties.md) | JWT, rate limiting, file storage, database, Redis, email |
| [Environments](./environments.md) | URLs, ports, Docker Compose commands by environment |
| [Dependencies](./dependencies.md) | Tech stack versions and dependency management |
| [Error Codes](./error-codes.md) | API error codes and messages reference |
| [Versioning](./versioning.md) | Semantic versioning strategy |
| [Scripts Organization](./scripts-organization.md) | Scripts structure and usage |

---

## Quick Reference

### Environments

| Environment | Backend URL | Frontend URL |
|-------------|-------------|--------------|
| Local | http://localhost:8080 | http://localhost:4200 |
| Staging | https://staging.example.com | https://staging-app.example.com |
| Production | https://api.example.com | https://app.example.com |

### Key Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `jwt.expiration` | Access token TTL | 15 minutes |
| `jwt.refresh-expiration` | Refresh token TTL | 7 days |
| `rate-limit.contact` | Contact form limit | 3/hour |

---

## Related Documentation

- [Development Guide](../development/README.md) - Development setup
- [Deployment](../deployment/README.md) - CI/CD and deployment
- [Security](../security/README.md) - Security configuration
