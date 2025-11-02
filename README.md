# Portfolio Application

Full-stack web application with Angular, Spring Boot, and PostgreSQL.

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│                    NGINX                         │
│              (Reverse Proxy)                     │
│            Port: 8081 (local)                    │
└──────────┬────────────────────┬──────────────────┘
           │                    │
    ┌──────▼──────┐      ┌─────▼──────────┐
    │  Frontend   │      │    Backend     │
    │  Angular 20 │      │ Spring Boot 3  │
    │  Port: 4200 │      │   Port: 8080   │
    └─────────────┘      └────────┬────────┘
                                  │
                         ┌────────▼────────┐
                         │   PostgreSQL    │
                         │   Port: 5432    │
                         └─────────────────┘
```

### Technology Stack

**Frontend:**
- Angular 20
- Bootstrap 5.3
- RxJS
- TypeScript 5.8

**Backend:**
- Spring Boot 3.5.5
- Java 24
- Spring Data JPA
- Spring Security
- Spring Boot Actuator

**Database:**
- PostgreSQL 17.6

**Infrastructure:**
- Docker & Docker Compose
- Nginx 1.27
- GitHub Actions (CI/CD)

---

## Configuration

### Environment Variables

Create a `.env` file at project root:

```env
# Database
DB_USER_PASSWORD=your_secure_password
```

### Important Configuration Files

```
portfolio/
├── .env                              # Environment variables (create manually)
├── docker-compose.yml                # Base Docker configuration
├── docker-compose.local.yml          # Local overrides
├── docker-compose.staging.yml        # Staging overrides
├── docker-compose.prod.yml           # Production overrides
├── nginx/
│   ├── nginx.conf                    # Global Nginx configuration
│   └── conf.d/
│       ├── local.conf                # Nginx local config
│       ├── staging.conf              # Nginx staging config
│       └── prod.conf                 # Nginx production config
├── portfolio-frontend/
│   ├── proxy.conf.json               # Angular proxy for dev
│   └── angular.json                  # Angular configuration
└── portfolio-backend/
    ├── build.gradle                  # Gradle configuration
    └── src/main/resources/
        ├── application-dev.yml       # Spring Boot dev config
        ├── application-staging.yml   # Spring Boot staging config
        └── application-prod.yml      # Spring Boot production config
```

---

## Changelog

See [CHANGELOG.md](./CHANGELOG.md) for version history.

---

## License

This project is licensed under the MIT License

---

## Team

**Lead Developer:** Emmanuel Gabe

---

## Related Links

- [Architecture Guide](./docs/architecture/architecture.md)
- [CI/CD Guide](./docs/deployment/ci-cd.md)
- [Health Checks Guide](./docs/operations/health-checks.md)
- [Versioning Guide](./docs/reference/versioning.md)
- [Testing Guide](./docs/development/testing.md)
- [Troubleshooting](./docs/development/testing-troubleshooting.md)
