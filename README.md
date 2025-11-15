# Portfolio Application

Full-stack web application with Angular, Spring Boot, and PostgreSQL.

## Build & Quality Status

[![Backend Tests](https://github.com/emmanuelgabe/portfolio/actions/workflows/backend-tests.yml/badge.svg)](https://github.com/emmanuelgabe/portfolio/actions/workflows/backend-tests.yml)
[![Frontend Tests](https://github.com/emmanuelgabe/portfolio/actions/workflows/frontend-tests.yml/badge.svg)](https://github.com/emmanuelgabe/portfolio/actions/workflows/frontend-tests.yml)
[![CI/CD](https://github.com/emmanuelgabe/portfolio/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/emmanuelgabe/portfolio/actions/workflows/ci-cd.yml)
[![Health Check](https://github.com/emmanuelgabe/portfolio/actions/workflows/health-check.yml/badge.svg)](https://github.com/emmanuelgabe/portfolio/actions/workflows/health-check.yml)
[![Docs](https://github.com/emmanuelgabe/portfolio/actions/workflows/vale-docs.yml/badge.svg)](https://github.com/emmanuelgabe/portfolio/actions/workflows/vale-docs.yml)

![License](https://img.shields.io/github/license/emmanuelgabe/portfolio)
![Last Commit](https://img.shields.io/github/last-commit/emmanuelgabe/portfolio)
![Issues](https://img.shields.io/github/issues/emmanuelgabe/portfolio)

## Tech Stack

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?logo=springboot)
![Angular](https://img.shields.io/badge/Angular-18%20LTS-red?logo=angular)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)

---

## Quick Start

**Prerequisites:** Docker, Docker Compose, Git

```bash
# 1. Clone repository
git clone https://github.com/emmanuelgabe/portfolio.git
cd portfolio

# 2. Create environment file
echo "DB_USER_PASSWORD=your_secure_password" > .env

# 3. Start all services
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build -d

```

For detailed setup instructions, see [Setup Guide](./docs/development/setup.md).

---

## Architecture

```mermaid
flowchart TB
 subgraph Production["Production"]
        CF["Cloudflare Tunnel<br>SSL/TLS + DDoS Protection"]
        NGINX["NGINX<br>Reverse Proxy"]
        Frontend["Angular SPA<br>User Interface"]
        Backend["Spring Boot API<br>Business Logic"]
        Auth["Authentication Service<br>Coming Soon"]
        DB[("PostgreSQL<br>Database")]
        Storage["Persistent Storage"]
  end
    User["Web Browser"] -- HTTPS --> CF
    CF -- Secure Connection --> NGINX
    NGINX -- /api/* --> Backend
    NGINX -- /* --> Frontend
    Backend -- Auth<br>Future --> Auth
    Backend -- Persistence --> DB
    DB -. Volume .-> Storage

    style CF fill:#f38020,color:#fff
    style NGINX fill:#2d5c88,color:#fff
    style Frontend fill:#dd0031,color:#fff
    style Backend fill:#6db33f,color:#fff
    style Auth fill:#4285f4,color:#fff,stroke-dasharray: 5 5
    style DB fill:#336791,color:#fff
    style Storage fill:#ffd700,color:#000

```

### Technology Stack

**Frontend:**
- Angular 18 LTS
- Bootstrap 5
- RxJS
- TypeScript 5

**Backend:**
- Spring Boot 3
- Java 21 LTS
- Spring Data JPA
- Spring Security
- Spring Boot Actuator

**Database:**
- PostgreSQL 17

**Infrastructure:**
- Docker & Docker Compose
- Nginx
- GitHub Actions (CI/CD)

---

## Changelog

See [CHANGELOG.md](./CHANGELOG.md) for version history.

---

## License

This project is licensed under the MIT License.

---

## Contact

**Emmanuel Gabe** - Lead Developer