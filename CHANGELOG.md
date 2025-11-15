# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [0.2.0] - 2025-11-16

### Added
- Skills management system
  - Complete CRUD API for skills (`/api/skills`)
  - SkillCategory enum (LANGUAGE, FRAMEWORK, DATABASE, TOOL, CLOUD, TESTING)
  - Skill entity with proficiency level tracking
  - Skills repository and service layer
  - Skills controller with full REST endpoints
  - Database migration for skills table
- Projects and tags management system
  - Complete CRUD API for projects (`/api/projects`)
  - Tags system for categorizing projects
  - Many-to-many relationship between projects and tags
  - Project entity with title, description, technologies, and links
  - Database migrations for projects and tags tables
  - Project and tag repositories
  - Global exception handler for REST API errors
- Frontend portfolio UI components
  - Home component with skills and projects display
  - Navbar component with responsive design
  - Project list component with filtering
  - Project detail component with full information display
  - Project card component for grid view
  - Angular routing configuration
  - Project and skill services with HTTP client
  - TypeScript models for Project, Skill, and Tag
- Clean architecture refactoring
  - Service layer separation (interface + implementation)
  - Mapper layer for DTO/Entity conversions
  - Improved separation of concerns
  - OpenAPI documentation configuration
  - Structured logging with Logback configuration
- Code quality tools
  - Checkstyle integration with custom rules
  - SpotBugs static analysis
  - Checkstyle suppressions configuration
- Frontend development tools
  - ESLint configuration
  - Prettier code formatter
  - Husky pre-commit hooks
  - lint-staged for staged files checking
- Comprehensive test suite
  - Entity tests (ProjectEntityTest, SkillEntityTest, TagEntityTest)
  - Service tests (SkillServiceTest, TagServiceTest, enhanced ProjectServiceTest)
  - Controller tests (enhanced SkillControllerTest, ProjectControllerTest)
  - Global exception handler tests
  - Frontend component tests
- Enhanced documentation
  - Local testing guide (`docs/deployment/local-testing.md`)
  - Code quality guide (`docs/development/code-quality.md`)
  - Logging conventions (`docs/development/logging-conventions.md`)
  - Setup guide (`docs/development/setup.md`)
  - Dependencies reference (`docs/reference/dependencies.md`)
  - Scripts organization guide (`docs/reference/scripts-organization.md`)
  - Updated architecture documentation
- Additional scripts
  - Cleanup staging script (`scripts/maintenance/cleanup-staging.sh`)
  - Setup permissions script (`scripts/setup-permissions.sh`)
  - Run all tests script (`scripts/testing/run-all-tests.sh`)
  - Testing documentation (`scripts/testing/README.md`)
- HTTP interceptors
  - Logging interceptor for request/response tracking
  - Retry interceptor for failed requests
- Logger service for frontend
- Data seeding configuration for development

### Changed
- Backend architecture refactored to follow clean architecture principles
- Service layer split into interfaces and implementations
- DTO mapping centralized in dedicated mapper classes
- Enhanced ProjectService with improved error handling
- Updated CI/CD pipeline with quality checks
  - Checkstyle validation
  - SpotBugs analysis
  - Enhanced test reporting
- Improved deployment validation script
- Enhanced sync-prod-to-staging script with better error handling
- Updated documentation structure and organization
- Frontend services with enhanced error handling and retry logic
- Home component with dynamic data loading
- Angular application configuration updated
- Docker Compose configuration improvements
- Vale documentation vocabulary expanded
- README.md restructured with improved architecture diagram

### Fixed
- Checkstyle configuration issues
- CI/CD pipeline reliability improvements
- Code style violations across backend codebase
- Test coverage gaps in controllers and services
- Entity relationships and cascade operations
- Frontend component template formatting

### Removed
- Redundant documentation files
  - `docs/development/backend-tests.md` (merged into testing.md)
  - `docs/development/frontend-tests.md` (merged into testing.md)
  - `docs/development/testing-quick-ref.md` (integrated into testing.md)
  - `docs/operations/health-checks.md` (consolidated)
  - `docs/reference/versioning.md` (simplified)

---

## [0.1.0] - 2025-11-16

### Added
- Complete project infrastructure
  - Docker and Docker Compose setup
  - Multi-environment configuration (local, staging, production)
  - GitHub Actions CI/CD pipelines
- Backend application (Spring Boot 3.5.5)
  - Health check system with custom indicators
  - Database health monitoring
  - Complete health endpoint (`/api/health/full`)
  - Version endpoint (`/api/version`)
  - Environment-specific Spring Security configuration
  - Spring Boot Actuator for monitoring
  - JPA/Hibernate with PostgreSQL integration
  - Gradle build configuration
  - Dockerized backend with multi-stage builds
- Frontend application (Angular 18)
  - Basic application structure
  - Health check integration
  - Proxy configuration for development
  - Automatic version generation script
  - Bootstrap 5 integration
  - Dockerized frontend with Nginx
  - Karma/Jasmine test configuration
- Database (PostgreSQL 17)
  - Docker container configuration
  - Health check integration
  - Persistent volume configuration
- Nginx reverse proxy
  - Environment-specific configurations (local, staging, prod)
  - API routing with `/api/` prefix preservation
  - Static file serving
  - Production security headers
- GitHub Actions workflows
  - Backend tests workflow
  - Frontend tests workflow
  - CI/CD workflow with multi-environment support
  - Health check workflow
  - Vale documentation linting workflow
- Comprehensive documentation
  - Architecture documentation (`docs/architecture/architecture.md`)
  - Contributing guide (`docs/contributing.md`)
  - CI/CD documentation (`docs/deployment/ci-cd.md`)
  - Testing guides (`docs/development/`)
  - Health checks documentation (`docs/operations/health-checks.md`)
  - Versioning documentation (`docs/reference/versioning.md`)
- Scripts for automation
  - Health check testing (`scripts/testing/test-health.sh`)
  - Documentation validation (`scripts/docs/validate-docs.sh`)
  - Sync production to staging (`scripts/deployment/sync-prod-to-staging.sh`)
  - Deployment validation (`scripts/deployment/validate-deployment.sh`)
- Vale documentation linting
  - Custom vocabulary for portfolio project
  - Accept/reject word lists
- Complete test suite
  - Backend: HealthService, HealthController, CustomHealthIndicator
  - Frontend: AppComponent tests
  - 100% coverage of critical components
- Environment files examples
  - `.env.example` for production
  - `.env.local.example` for local development
- Makefile for common development tasks

### Changed
- README.md with comprehensive project information
- .gitignore with comprehensive exclusions

### Security
- CSRF protection enabled in staging/production
- CSRF disabled in development mode only
- Environment-specific Spring Security configuration
- Production security headers (X-Frame-Options, CSP, HSTS, etc.)
- Secure environment variable handling
- Docker secrets management ready

---

## [0.0.1] - 2025-11-03

### Added
- Initial commit
- Basic project structure
- License file (MIT)
- Initial .gitignore

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

- [Unreleased]: https://github.com/emmanuelgabe/portfolio/compare/v0.2.0...HEAD
- [0.2.0]: https://github.com/emmanuelgabe/portfolio/compare/v0.1.0...v0.2.0
- [0.1.0]: https://github.com/emmanuelgabe/portfolio/compare/5273176...v0.1.0
- [0.0.1]: https://github.com/emmanuelgabe/portfolio/commit/5273176
