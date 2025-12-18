# Portfolio Documentation

Welcome to the Portfolio project documentation. This documentation covers all aspects of the application from API reference to deployment guides.

---

## Quick Navigation

| Section | Description |
|---------|-------------|
| [API Reference](./api/README.md) | REST API endpoints, authentication, request/response formats |
| [Architecture](./architecture/README.md) | System design, database schema, frontend architecture |
| [Features](./features/README.md) | Feature documentation: blog, CV, contact, images |
| [Security](./security/README.md) | Authentication, authorization, rate limiting |
| [Development](./development/README.md) | Setup, testing, code quality, logging |
| [Reference](./reference/README.md) | Configuration, environments, error codes |
| [Deployment](./deployment/README.md) | CI/CD pipelines, local testing |
| [Operations](./operations/README.md) | Health checks, monitoring |

---

## Getting Started

### Prerequisites

- Java 21 LTS
- Node.js 20 LTS
- Docker Desktop
- PostgreSQL 15+

### Quick Start

```bash
# Clone repository
git clone https://github.com/username/portfolio.git
cd portfolio

# Backend
cd portfolio-backend
./gradlew bootRun

# Frontend (new terminal)
cd portfolio-frontend
npm install
npm start
```

### Access Points

| Service | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health Check | http://localhost:8080/actuator/health |

---

## Project Structure

```
portfolio/
├── portfolio-backend/     # Spring Boot API
├── portfolio-frontend/    # Angular SPA
├── docs/                  # Documentation
├── nginx/                 # Nginx configuration
├── scripts/               # Utility scripts
└── docker-compose*.yml    # Docker configurations
```

---

## Documentation Categories

### API Reference

Complete REST API documentation organized by resource:

- [Authentication](./api/authentication.md) - Login, token refresh, logout
- [Projects](./api/projects.md) - Portfolio projects CRUD
- [Skills](./api/skills.md) - Skills management
- [Experiences](./api/experiences.md) - Timeline experiences
- [Articles](./api/articles-api.md) - Blog articles
- [Tags](./api/tags.md) - Tag management
- [Contact](./api/contact.md) - Contact form
- [CV](./api/cv.md) - CV management
- [Site Configuration](./api/site-configuration.md) - Site-wide configuration
- [Article Images](./api/article-images.md) - Article image uploads

### Architecture

System design and technical architecture:

- [System Architecture](./architecture/architecture.md) - High-level design
- [Frontend Architecture](./architecture/frontend-architecture.md) - Angular structure
- [Database Schema](./architecture/database-schema.md) - PostgreSQL schema
- [Error Handling](./architecture/error-handling.md) - Exception handling

### Features

Detailed feature documentation:

- [Blog Articles](./features/blog-articles.md) - Markdown, slugs, reading time
- [Site Configuration](./features/site-configuration.md) - Centralized site settings
- [Experience Management](./features/experience-management.md) - Timeline
- [CV Management](./features/cv-management.md) - CV versioning
- [Contact Form](./features/contact-form.md) - Rate-limited contact
- [Image Processing](./features/image-processing.md) - WebP, thumbnails
- [File Storage](./features/file-storage.md) - File upload system

### Security

Security implementation details:

- [Authentication](./security/authentication.md) - JWT system overview
- [JWT Implementation](./security/jwt-implementation.md) - Token details
- [RBAC](./security/rbac.md) - Role-based access control
- [Password Management](./security/password-management.md) - BCrypt
- [Rate Limiting](./security/rate-limiting.md) - Request limits
- [SVG Sanitization](./security/svg-sanitization.md) - XSS prevention

### Development

Development guides and conventions:

- [Setup Guide](./development/setup.md) - Environment setup
- [Testing Guide](./development/testing-guide.md) - Test strategy
- [Git Workflow](./development/git-workflow.md) - Branch strategy
- [Code Quality](./development/code-quality.md) - Static analysis
- [Logging Conventions](./development/logging-conventions.md) - Log format

### Reference

Configuration and reference material:

- [Configuration](./reference/configuration-properties.md) - App properties
- [Environments](./reference/environments.md) - Env-specific settings
- [Dependencies](./reference/dependencies.md) - Tech stack
- [Error Codes](./reference/error-codes.md) - API error reference

---

## Contributing

See [Contributing Guide](./contributing.md) for contribution guidelines.
