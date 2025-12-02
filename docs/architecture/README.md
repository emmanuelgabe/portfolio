# Architecture Documentation

---

## Table of Contents
1. [Introduction](#1-introduction)
2. [System Architecture](#2-system-architecture)
3. [Frontend Architecture](#3-frontend-architecture)
4. [Backend Architecture](#4-backend-architecture)
5. [Data Architecture](#5-data-architecture)
6. [Error Handling](#6-error-handling)

---

## 1. Introduction

This section provides comprehensive documentation of the Portfolio application architecture. The application follows a modern three-tier architecture with Angular frontend, Spring Boot backend, and PostgreSQL database.

**Architecture Overview**:
- **Frontend**: Angular 18+ standalone components (SPA)
- **Backend**: Spring Boot 3.4+ REST API (Java 21)
- **Database**: PostgreSQL 17 with Flyway migrations
- **Deployment**: Docker Compose containerization
- **Proxy**: Nginx reverse proxy

**Key Architectural Principles**:
- Separation of concerns (frontend/backend/database)
- Stateless application servers (JWT authentication)
- RESTful API design
- Container-based deployment
- Environment-specific configuration

---

## 2. System Architecture

### 2.1 High-Level Overview

**File**: [architecture.md](./architecture.md)

**Description**: Complete system architecture documentation including infrastructure, deployment, and component interaction.

**Topics Covered**:
- Three-tier architecture diagram
- Component responsibilities
- Technology stack (Angular, Spring Boot, PostgreSQL, Docker)
- Request flow patterns
- Environment configurations
- Health check integration
- Deployment architecture

**Key Components**:
- Nginx reverse proxy (SSL termination, routing)
- Angular SPA (port 4200)
- Spring Boot API (port 8080)
- PostgreSQL database (port 5432)
- Redis cache (rate limiting)

**Environments**:
- Local development (direct execution)
- Docker development (docker-compose)
- Staging (docker-compose with staging profile)
- Production (containerized deployment)

---

## 3. Frontend Architecture

### 3.1 Angular Architecture

**File**: [frontend-architecture.md](./frontend-architecture.md)

**Description**: Detailed Angular 18+ architecture with standalone components, routing, state management, and HTTP communication.

**Topics Covered**:
- Standalone components architecture (no NgModules)
- Route configuration with nested admin routes
- Service-based state management (no Redux/NgRx)
- HTTP interceptors (JWT, Retry, Logging)
- Functional guards (authGuard, adminGuard)
- Component structure and organization
- Form handling (Reactive Forms)
- API integration patterns

**Key Services** (15 documented):
- AuthService - JWT authentication
- ProjectService - Project management
- ArticleService - Blog articles
- ExperienceService - Timeline experiences
- TagService - Tag management
- CvService - CV downloads
- I18nService - Internationalization
- ActivityMonitorService - Session timeout

**Guards**:
```typescript
export const adminGuard: CanActivateFn = (route, state): boolean | UrlTree => {
  const authService = inject(AuthService);
  if (authService.isAuthenticated() && authService.isAdmin()) {
    return true;
  }
  return router.createUrlTree(['/login']);
};
```

**Interceptors**:
- JWT Interceptor: Automatic token attachment and refresh on 401
- Retry Interceptor: Automatic retry for transient failures
- Logging Interceptor: Request/response logging (dev only)

**Component Categories**:
- Public pages (Home, Projects, Blog, CV)
- Admin pages (Dashboard, Projects, Articles, Experiences, Tags, CV)
- Shared components (Navbar, Footer, Modals)
- Feature components (Article cards, Project cards, Experience timeline)

---

## 4. Backend Architecture

### 4.1 Spring Boot Architecture

**Topics**: REST API design, service layer, security, validation

**Key Components**:

1. **Controllers** (REST endpoints):
   - `@RestController` for JSON responses
   - `@Valid` for request validation
   - Public endpoints vs admin endpoints (`/api/admin/**`)

2. **Services** (Business logic):
   - `@Service` annotation
   - Transaction management (`@Transactional`)
   - DTO mapping with MapStruct
   - File operations (uploads, storage)

3. **Repositories** (Data access):
   - JPA repositories (`extends JpaRepository`)
   - Custom query methods
   - Specification-based filtering

4. **Security**:
   - Spring Security configuration
   - JWT authentication filter
   - Role-based access control (RBAC)
   - CORS configuration

5. **Exception Handling**:
   - See [Error Handling](#6-error-handling) section below

**Layers**:
```
Controller Layer → Service Layer → Repository Layer → Database
     ↓                 ↓
  Validation      Business Logic
```

---

## 5. Data Architecture

### 5.1 Database Schema

**File**: [database-schema.md](./database-schema.md)

**Description**: Complete PostgreSQL database schema with Flyway migrations, table structures, relationships, and indexes.

**Topics Covered**:
- 9 Flyway migrations (V1-V9)
- 9 database tables with complete DDL
- Entity relationships (one-to-many, many-to-many)
- Indexes and constraints
- Partial indexes for optimization
- Foreign key cascades
- Unique constraints

**Tables**:

1. **users** (V1) - User accounts with roles
2. **refresh_tokens** (V2) - JWT refresh token storage
3. **projects** (V3) - Portfolio projects
4. **tags** (V3) - Categorization tags
5. **project_tags** (V3) - Many-to-many junction
6. **skills** (V4) - Skills with proficiency
7. **cvs** (V5) - CV/resume management
8. **experiences** (V6) - Timeline experiences
9. **articles** (V8) - Blog articles with Markdown

**Key Relationships**:
- Projects ↔ Tags (many-to-many via project_tags)
- Articles ↔ Tags (many-to-many via article_tags)
- Users → RefreshTokens (one-to-many)
- Users → CVs (one-to-many)

**Indexes**:
- Primary keys on all tables
- Foreign key indexes
- Unique constraints (email, username, slug)
- Partial indexes (ongoing experiences, current CV, published articles)
- Composite indexes (user_id + expiry_date on refresh_tokens)

**Example DDL**:
```sql
CREATE TABLE articles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    content TEXT NOT NULL,
    published BOOLEAN DEFAULT FALSE,
    published_at TIMESTAMP,
    reading_time INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_articles_published ON articles(published);
CREATE INDEX idx_articles_slug ON articles(slug);
```

---

### 5.2 Migration Strategy

**Tool**: Flyway

**Migration Naming**: `V{version}__{description}.sql`

**Examples**:
- `V1__create_users_table.sql`
- `V8__create_articles_table.sql`
- `V9__remove_views_count_from_articles.sql`

**Configuration**:
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

**Best Practices**:
- Never modify existing migrations
- Always create new migration for schema changes
- Test migrations on staging before production
- Include rollback strategy for critical changes

---

## 6. Error Handling

### 6.1 Centralized Exception Handling

**File**: [error-handling.md](./error-handling.md)

**Description**: Comprehensive error handling strategy with GlobalExceptionHandler, custom exceptions, and standardized error responses.

**Topics Covered**:
- GlobalExceptionHandler with `@RestControllerAdvice`
- 14 exception types mapped to HTTP status codes
- Validation error handling with field-level details
- Custom exceptions (ResourceNotFoundException, DuplicateResourceException, etc.)
- Error response format standardization
- Frontend error handling patterns

**Exception Mapping**:

| Exception | HTTP Status | Use Case |
|-----------|-------------|----------|
| `ResourceNotFoundException` | 404 | Entity not found (Project, Article, etc.) |
| `DuplicateResourceException` | 409 | Unique constraint violation |
| `MethodArgumentNotValidException` | 400 | Validation errors (@Valid) |
| `UnauthorizedException` | 401 | Invalid/expired JWT token |
| `AccessDeniedException` | 403 | Insufficient permissions |
| `RateLimitExceededException` | 429 | Rate limit exceeded |
| `FileStorageException` | 400/500 | File upload/storage errors |

**Error Response Format**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Project not found with id: 123",
  "path": "/api/admin/projects/123"
}
```

**Validation Error Response**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "title",
      "rejectedValue": "",
      "message": "must not be blank"
    },
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "must be a valid email address"
    }
  ],
  "path": "/api/admin/projects"
}
```

**GlobalExceptionHandler Implementation**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
        ResourceNotFoundException ex, WebRequest request) {

        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(
        MethodArgumentNotValidException ex) {

        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() != null
                    ? error.getDefaultMessage()
                    : "Invalid value"
            ));

        ValidationErrorResponse response = new ValidationErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            errors
        );

        return ResponseEntity.badRequest().body(response);
    }
}
```

**Frontend Error Handling**:
```typescript
// HTTP Interceptor
return next(request).pipe(
  catchError((error: HttpErrorResponse) => {
    if (error.status === 401) {
      // Refresh token and retry
      return this.authService.refreshToken().pipe(
        switchMap(() => next(cloneRequest))
      );
    }

    if (error.status === 403) {
      // Redirect to unauthorized page
      this.router.navigate(['/unauthorized']);
    }

    // Show error message to user
    this.snackBar.open(error.error.message, 'Close', { duration: 5000 });

    return throwError(() => error);
  })
);
```

---

## Related Documentation

- [API Documentation](../api/README.md) - REST API endpoints
- [Features Documentation](../features/README.md) - Feature implementations
- [Security: JWT Implementation](../security/jwt-implementation.md) - Authentication details
- [Security: RBAC](../security/rbac.md) - Role-based access control
- [Security: Rate Limiting](../security/rate-limiting.md) - Rate limiting implementation
- [Reference: Configuration Properties](../reference/configuration-properties.md) - Configuration
- [Reference: Error Codes](../reference/error-codes.md) - Error codes reference
- [Development: Setup](../development/setup.md) - Development environment setup
- [Deployment: CI/CD](../deployment/ci-cd.md) - Deployment pipeline
