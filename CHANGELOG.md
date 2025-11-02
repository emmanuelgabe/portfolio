# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- Complete unit test suite
  - Backend: 24 tests (HealthService, HealthController, CustomHealthIndicator)
  - Frontend: 11 tests (AppComponent)
  - Test coverage: 100% of critical components
- Comprehensive documentation restructure
  - Documentation standards guide (`docs/contributing.md`)
  - Separate testing guides (testing.md, backend-tests.md, frontend-tests.md)
  - Testing quick reference and troubleshooting guides
  - Condensed CI/CD, health checks, and versioning documentation
  - Architecture documentation (`docs/architecture/architecture.md`)
- Complete documentation reorganization in `/docs`
- System-wide health checks
  - `/health/full` endpoint for full chain verification
  - Docker healthchecks for all services
  - Automated `test-health.sh` script
- Automatic versioning system based on Git tags
- CI/CD with GitHub Actions
  - Automated health check workflow
  - Staging/production deployment pipeline
- Environment-specific Nginx configuration (local, staging, prod)

### Changed
- Documentation now follows professional standards
  - Removed emojis from all documentation
  - Applied hierarchical numbering
  - Condensed redundant content
  - Separated troubleshooting into dedicated guides
- Test infrastructure configuration
  - Karma and Jasmine setup for Angular 20
  - Security filters disabled in controller tests
  - Lenient mocking for optional dependencies

### Fixed
- Angular proxy configuration to point to correct backend container
- Nginx configuration to maintain `/api/` prefix
- Spring Boot Actuator `/actuator/info` configuration
- Vite allowedHosts to accept requests from Nginx

---

## [0.0.1] - 2025-11-07

### Added
- Initial project architecture
  - Frontend Angular 20
  - Backend Spring Boot 3.5.5
  - Database PostgreSQL 17.6
  - Nginx reverse proxy
- Multi-environment Docker Compose configuration
  - Local (port 8081)
  - Staging (port 3000)
  - Production (port 80)
- Backend features
  - Environment-specific Spring Security configuration
  - Spring Boot Actuator for monitoring
  - Custom health check controller (`/api/health/*`)
  - Version endpoint (`/api/version`)
- Frontend features
  - Angular health check service
  - Proxy configuration for development
  - Automatic version generation script
- Infrastructure
  - Environment-specific Nginx configuration
  - Docker healthchecks for all services
  - Secure environment variables

### Security
- CSRF disabled in dev mode only
- CSRF enabled with cookie tokens in staging/production
- Environment-specific Spring Security configuration
- Production security headers (X-Frame-Options, CSP, etc.)

---

## Version Notes

### How to Read This Changelog

- **Added:** New features
- **Changed:** Changes to existing features
- **Deprecated:** Features to be removed in future releases
- **Removed:** Removed features
- **Fixed:** Bug fixes
- **Security:** Security vulnerability fixes

### Links

- [Unreleased]: Changes not yet deployed
- [0.0.1]: Initial version (2025-11-07)
