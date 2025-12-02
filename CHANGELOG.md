# Changelog

### Added
- CV management system with versioning
  - Upload, download, and version control for CV files
  - Single "current" CV designation with database constraint
  - Admin UI for CV management
  - Public endpoint for current CV download
- Project image upload with optimization
  - WebP conversion with Thumbnailator library (quality 0.85)
  - Automatic thumbnail generation (400x300px, quality 0.8)
  - Magic bytes validation for security
  - Old image cleanup on update
- Complete API documentation suite
  - 6 API reference files (authentication, projects, skills, cv, files)
  - 3 security documentation files (authentication, RBAC, password management)
  - 3 features documentation files (CV management, image processing, file storage)
  - Medium detail level following project conventions
- Complete documentation reorganization in `/docs`
  - Documentation standards with detail level guidelines
  - Professional structure without emojis
  - Hierarchical numbering and concise content

### Changed
- Test naming convention standardized to `should_when` pattern
- Documentation detail level reduced to medium (200-400 lines per file)
- Removed overly detailed implementation sections from documentation

### Fixed
- Angular proxy configuration to point to correct backend container
- Nginx configuration to maintain `/api/` prefix
- Spring Boot Actuator `/actuator/info` configuration

---

## [0.3.0] - 2025-11-19

### Added
- JWT authentication system with Spring Security
  - Dual-token system (access token: 15min, refresh token: 7 days)
  - Login, logout, refresh token, and change password endpoints
  - Role-based access control (ADMIN, USER)
  - BCrypt password hashing (strength 10)
- Frontend authentication UI
  - Login page with Angular reactive forms
  - Navbar with login/logout functionality
  - Auth guard and admin guard for route protection
  - HTTP interceptor for automatic token refresh
  - Token storage service (localStorage)
- File upload system
  - Generic file upload endpoint (`/api/admin/upload`)
  - Multipart file upload with validation (10MB limit)
  - Support for PNG, JPG, GIF, WebP formats
  - Magic bytes validation for security
  - File storage service with timestamped filenames
  - Admin UI components for file management
- Authentication unit tests
  - AuthService tests with 80%+ coverage
  - AuthController tests
  - JwtTokenProvider tests
  - Role mapping validation tests

### Changed
- Skill model simplified (removed deprecated level system)
  - Database migration V7 for skill table cleanup
  - Updated SkillResponse DTO
  - Updated frontend skill components
- Admin endpoints now require JWT authentication
  - `/api/admin/projects` protected with ROLE_ADMIN
  - `/api/admin/skills` protected with ROLE_ADMIN
  - Security configuration with method-level authorization
- Cross-platform configuration
  - Updated Prettier config for Windows/Unix compatibility
  - Updated ESLint config for cross-platform paths

### Fixed
- Checkstyle configuration for entity suppressions
- CI/CD pipeline improvements
  - Backend tests workflow optimization
  - Correct Gradle task execution order

### Security
- JWT token security
  - Short-lived access tokens (15 minutes)
  - Secure refresh token rotation
  - Token invalidation on logout and password change
- Password security
  - BCrypt hashing with configurable strength
  - Minimum 8 character requirement
  - Secure password change workflow
- CORS configuration for authentication endpoints
- CSRF protection maintained in staging/production

### Breaking Changes
- Skill entity schema updated (removed `level` column)
  - Migration: Run `./gradlew flywayMigrate`
  - Frontend: Update skill components if custom level logic exists
- Admin endpoints require authentication
  - All `/api/admin/**` endpoints now require valid JWT with ROLE_ADMIN
  - Update API clients to include Authorization header
- Authentication required for file upload
  - `/api/admin/upload` now requires authentication

### Migration Notes
1. Run database migrations: `./gradlew flywayMigrate`
2. Set JWT_SECRET environment variable (min 256 bits)
3. Update environment configuration for auth endpoints
4. Existing admin users need to login with new auth system
5. Update API clients to handle JWT authentication

---

## [0.2.0] - 2025-11-15

### Added
- Complete unit test suite
  - Backend: 24 tests (HealthService, HealthController, CustomHealthIndicator)
  - Frontend: 11 tests (AppComponent)
  - Test coverage: 100% of critical components
- Comprehensive documentation restructure
  - Documentation standards guide (`docs/contributing.md`)
  - Separate testing guides (testing.md, backend-tests.md, frontend-tests.md)
  - Condensed CI/CD, health checks, and versioning documentation
  - Architecture documentation (`docs/architecture/architecture.md`)
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
- Test infrastructure configuration
  - Karma and Jasmine setup for Angular 20
  - Security filters disabled in controller tests
  - Lenient mocking for optional dependencies

---

## [0.1.0] - 2025-11-10

### Added
- Backend clean architecture implementation
  - MapStruct for DTO mapping
  - Lombok for code generation
  - Service layer with transactional support
- Code quality tools
  - Checkstyle for code style verification
  - SpotBugs for static analysis
  - JaCoCo for code coverage (80% minimum)
- Frontend portfolio UI components
  - Project list and detail pages
  - Skill display components
  - Responsive Bootstrap 5 layout
- Skills management system
  - Full CRUD operations for skills
  - Skill categories (PROGRAMMING, FRAMEWORK, DATABASE, etc.)
  - Backend and frontend integration

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

