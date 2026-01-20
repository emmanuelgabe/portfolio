# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.2.0] - 2026-01-17

### Added

#### Experience Management Enhancements
- New STAGE (internship) experience type for differentiated career entries
- showMonths field to control date display granularity (show months vs year only)
- displayOrder field for manual sorting of experiences
- Admin reorder endpoint (PUT /api/admin/experiences/reorder)
- Repository methods for ordered queries (findByTypeOrderByDisplayOrderAsc, findAllByOrderByDisplayOrderAsc)
- GraphQL schema updates with ExperienceOrderByInput and new fields

#### Project Markdown Enhancements
- Unlimited Markdown description for projects (TEXT column)
- Markdown editor with toolbar in admin project form (bold, italic, lists, links)
- Editor/Preview toggle for real-time Markdown rendering
- Smart description truncation in project cards (strips Markdown syntax)

#### Project Detail Page
- Image lightbox with fullscreen gallery view
- Keyboard navigation (Escape to close, Arrow keys to navigate)
- Social sharing buttons (Twitter, LinkedIn, Copy link)
- Improved image carousel with sorted display order
- Full Markdown-rendered description display

#### Admin Experience UI
- Display order column with up/down arrow buttons
- Status badges (ongoing/completed) in experience list
- Improved form with optional fields handling
- Sorting by type and display order

#### Build Configuration
- Added fileReplacements for production in angular.json
- Added complete staging configuration with fileReplacements and budgets

### Changed
- **Project description**: Changed from VARCHAR(2000) to TEXT for unlimited Markdown content
- Experience entity fields now optional (company, role, startDate, type)
- Only description field remains required for both projects and experiences
- Experience list sorted by displayOrder in public views
- Home component sorts experiences by displayOrder
- Updated i18n translations for STAGE type in 10 languages
- LoggerService now uses Angular isDevMode() for automatic log level detection
- Removed logLevel from all environment files (dev, staging, prod)
- Simplified Sentry message capture in LoggerService
- Project detail page: Removed "Description" heading for cleaner layout

### Removed
- **ProjectDetails entity**: Removed unused ProjectDetails table and related code (entity, repository, mapper, DTOs)
- Removed `markdownSupport` i18n key from all 10 language files (redundant)
- Removed max length validation on project description field

### Fixed
- Docker network configuration for monitoring stack (Prometheus/Grafana "No Data" fix)
- Network name default changed to portfolio-prod_portfolio-net for production
- Added NETWORK_NAME documentation to .env.example
- **Tests**: Updated ProjectEntityTest for unlimited description length
- **ESLint**: Fixed prefer-const error in project-card.component.ts

### Database Migrations
- V27: Add show_months boolean column to experiences table
- V28: Add display_order column, make fields nullable, add STAGE to type constraint
- V29: Create project_details table (later removed in V30)
- V30: Drop project_details table and alter projects.description to TEXT (unlimited Markdown)

---

## [1.1.0] - 2026-01-14

### Added

#### Entity Reordering System
- Display order management for projects, articles, and skills
- Database migration V26 for display_order columns on projects and articles
- ReorderRequest DTO for generic reordering operations
- Reorder endpoints for admin controllers (PUT /api/admin/*/reorder)
- Arrow buttons (up/down) in admin list components for intuitive reordering
- Optimistic UI updates with rollback on error
- Cache eviction on reorder operations

#### Markdown Experience Descriptions
- MarkdownService for client-side Markdown rendering with DOMPurify
- MonthYearPicker component for date selection without day
- EasyMDE integration in experience form with custom alignment buttons
- Markdown rendering in timeline for experience descriptions
- Locale utilities for internationalized date formatting

#### Admin UI Enhancements
- Order column with arrow buttons in skill-list component
- Order column with arrow buttons in project-list component
- Order column with arrow buttons in article-list component
- Buttons disabled during reordering, search active, or demo mode
- Improved experience list with status badges (ongoing/completed)

#### GDPR Compliance
- Feature flags for Sentry, WebVitals, and VisitorTracking
- Flags disabled by default for privacy compliance
- Environment variable configuration for CI/CD enablement
- Privacy policy dynamic content from site configuration

### Changed
- Projects and articles now sorted by displayOrder in public views
- HomeComponent sorts projects and articles by displayOrder
- ProjectListComponent (public) sorts projects by displayOrder
- Updated ProjectResponse and ArticleResponse DTOs with displayOrder field
- Updated frontend models with displayOrder property
- Production log level changed from INFO to WARN

### Security
- Improved SVG sanitization with text/tspan/textpath/style elements whitelist
- Enhanced dangerous pattern detection with word boundary matching
- Safe data URI pattern for embedded images in SVGs
- Case-insensitive element and attribute matching
- DOMPurify sanitization for article detail HTML content

### Dependencies
- Added dompurify ^3.3.1 for XSS protection
- Added @types/dompurify ^3.0.5 for TypeScript support

### Database Migrations
- V26: Add display_order to projects and articles tables with indexes and constraints

---

## [1.0.0] - 2025-12-22

### Added

#### Message Queue & Async Processing (RabbitMQ)
- Async email sending for contact form submissions
- Async image processing (WebP conversion, thumbnail generation)
- Non-blocking audit logging via message queue
- Dead letter queue handling for failed messages
- Event publisher abstraction with NoOp fallback

#### Event Streaming (Kafka)
- Real-time analytics event streaming
- Admin action event sourcing (CREATE, UPDATE, DELETE operations)
- Activity events for session tracking
- Event consumers for processing and storage

#### Batch Processing (Spring Batch)
- Image reprocessing job (reprocess with updated quality settings)
- Audit cleanup job (90-day retention policy)
- Monthly audit report generation (PDF export)
- Daily stats aggregation job
- Sitemap regeneration job
- Search reindex job for Elasticsearch

#### Full-Text Search (Elasticsearch)
- Elasticsearch integration for articles, projects, experiences
- Search documents with automatic indexing
- JPA fallback search when Elasticsearch unavailable
- Admin search management endpoints

#### GraphQL API
- Complete GraphQL schema (`/graphql` endpoint)
- Query resolvers for all entities (articles, projects, experiences, skills, tags)
- Mutation resolvers for CRUD operations
- DataLoaders for N+1 query optimization
- Cursor-based pagination (Relay Connection spec)
- Field resolvers for nested relationships

#### Resilience & Circuit Breaker (Resilience4j)
- Circuit breaker for EmailService (CLOSED/OPEN/HALF_OPEN states)
- @Retry pattern before circuit breaker trips
- Prometheus metrics for circuit breaker state
- Admin dashboard card showing circuit breaker status

#### Audit System
- @Auditable annotation with AOP aspect
- AuditLog entity with comprehensive filtering
- Audit statistics and reporting endpoints
- PDF export for audit reports
- 90-day retention with batch cleanup

#### Visitor Tracking & Analytics
- Redis-based visitor session tracking (TTL)
- Server-Sent Events (SSE) for real-time active users count
- Daily visitor statistics aggregation
- Unique visitors tracking
- Visitors chart (last 7 days) on admin dashboard

#### Caching (Redis)
- Spring Cache integration with Redis backend
- Cache configuration for projects, articles, skills, experiences
- Automatic cache eviction on entity updates

#### Internationalization (i18n)
- Message bundles for 10 languages (EN, FR, ES, DE, PT, RU, ZH, JA, AR, HI)
- Backend validation messages localization

#### Blog System
- Markdown rendering with GitHub Flavored Markdown (GFM)
- Automatic slug generation from title
- Reading time calculation
- Draft/publish workflow
- Article images with async processing
- Tag associations

#### Experience Management
- Timeline experiences (WORK, EDUCATION, CERTIFICATION, VOLUNTEERING)
- Ongoing experience support
- Date range validation
- Sorted by date with type grouping

#### Site Configuration
- Centralized site settings management
- Identity configuration (name, title, description)
- Hero section configuration
- SEO metadata
- Social links

#### Contact Form
- IP-based rate limiting (5 requests per hour)
- Email notification via RabbitMQ
- Form validation with CAPTCHA-ready structure

#### Tag Management
- Tag CRUD with color support
- Tag associations for articles and projects
- Admin UI for tag management

#### Project Multi-Image Carousel
- Up to 10 images per project
- Primary image designation
- Drag-and-drop reordering
- Async image processing with status tracking

#### Legal & Compliance
- Privacy policy page (/privacy-policy)
- Legal notice page (/legal)
- GDPR-ready structure

#### Admin Dashboard
- Circuit breaker status cards
- Real-time active users count (SSE)
- Visitors chart (Chart.js, last 7 days)
- Batch job management (run, history, stats)
- Audit logs viewer with filtering

#### Frontend Components
- Skeleton loaders (7 components: project-card, article-card, timeline, skill, table-row, project-detail, article-detail)
- Language selector component
- Search input component
- Footer component
- Offline banner component
- Error pages (404, 500, etc.)

#### Progressive Web App (PWA)
- Service worker with Angular ngsw
- Web app manifest
- Offline detection and banner
- App icons (192x192, 512x512)

#### Performance Optimizations
- Lazy image loading directive
- Async image loading directive
- OnPush change detection (home, project-form)

#### Monitoring & Observability
- Grafana dashboards (10+):
  - Executive Overview (SLA, Apdex, traffic, services status)
  - API Dashboard (requests, latency, errors)
  - JVM Dashboard (heap, GC, threads)
  - PostgreSQL Dashboard (connections, queries, cache)
  - Spring Batch Dashboard (jobs, duration, items)
  - Security & Auth Dashboard (login attempts, rate limiting)
  - Circuit Breaker Dashboard
  - Kafka Dashboard
  - RabbitMQ Dashboard
  - Logs Explorer (Loki)
- Prometheus metrics and alert rules
- Loki/Promtail log aggregation
- Alertmanager configuration
- Business metrics (MeterRegistry)

#### Documentation
- 81 documentation files in `/docs`
- MkDocs configuration for documentation site
- Vale linting for documentation quality
- API documentation (16 endpoints documented)
- Architecture documentation
- Security documentation
- Feature documentation
- Development guides

#### CI/CD Enhancements
- SonarCloud integration for code quality
- Documentation build workflow
- Vale linting workflow
- Health check workflow improvements

### Changed
- Home page integrated with site configuration
- Admin layout with new navigation routes
- Project and article list components with skeleton loading
- Session monitoring with activity detection
- Demo mode for showcasing admin features

### Fixed
- Project-list tests with missing ProjectResponse properties
- CV service refactoring and test fixes
- Application configuration updates

### Security
- Upload rate limiting (10 uploads per hour per IP)
- Auth rate limiting (5 attempts per minute per IP)
- SVG sanitization for skill icons
- CORS configuration updates
- Admin seeder for initial setup

### Database Migrations
- V20: Create audit_logs table
- V21: Add audit username lower index
- V22: Add status to project_images
- V23: Add status to article_images
- V24: Create daily_stats table
- V25: Add unique_visitors to daily_stats

---

## [0.4.0] - 2025-12-01

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
  - Karma and Jasmine setup for Angular 18
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
  - Frontend Angular 18
  - Backend Spring Boot 3.4
  - Database PostgreSQL 17
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
