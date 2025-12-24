# Frontend Architecture

---

## Table of Contents
1. [Overview](#1-overview)
2. [Technology Stack](#2-technology-stack)
3. [Project Structure](#3-project-structure)
4. [Routing Architecture](#4-routing-architecture)
5. [Service Layer](#5-service-layer)
6. [Guards and Interceptors](#6-guards-and-interceptors)
7. [Component Organization](#7-component-organization)
8. [State Management](#8-state-management)

---

## 1. Overview

The frontend is built with **Angular 18** using **standalone components** architecture.

**Key Architectural Decisions**:
- Standalone components (no NgModules)
- Functional route guards (`CanActivateFn`)
- HTTP interceptors for cross-cutting concerns
- Service-based state management
- Separation of public and admin areas
- Token-based authentication with auto-refresh

**Application Type**: Single Page Application (SPA)

---

## 2. Technology Stack

### Core Framework

**Angular 18 LTS**:
- Standalone components
- Signals (reactive primitives)
- HttpClient for API communication
- Router for navigation
- RxJS for reactive programming

### UI Framework

**Bootstrap 5.3**:
- Responsive grid system
- Pre-built components
- Bootstrap Icons for iconography
- Custom SCSS styling

### Build Tools

- **TypeScript 5.5** (strict mode)
- **Vite** (development server)
- **ESLint + Prettier** (code quality)
- **Husky + lint-staged** (pre-commit hooks)

---

## 3. Project Structure

```
src/app/
├── components/          # Reusable UI components
│   ├── article-card/
│   ├── contact-form/
│   ├── footer/
│   ├── home/
│   ├── navbar/
│   ├── project-card/
│   ├── project-detail/
│   ├── project-list/
│   └── shared/
│       ├── language-selector/
│       ├── search-input/
│       ├── session-expiry-modal/
│       ├── skeleton/
│       └── visitors-chart/
├── pages/               # Page-level components
│   ├── admin/           # Admin area (protected)
│   │   ├── admin-layout/
│   │   ├── dashboard/
│   │   ├── articles/
│   │   ├── projects/
│   │   └── ...
│   ├── blog/
│   ├── contact/
│   ├── error/
│   └── login/
├── services/            # Business logic and API
├── guards/              # Route protection
├── interceptors/        # HTTP middleware
├── models/              # TypeScript interfaces
└── app.routes.ts        # Route configuration
```

### File Naming Conventions

- `component-name.component.ts` - Component class
- `component-name.component.html` - Template
- `component-name.component.scss` - Styles
- `service-name.service.ts` - Service class

---

## 4. Routing Architecture

### Public vs Admin Routes

**Public Routes** (unauthenticated access):
- `/` - Homepage with featured projects
- `/projects` - Projects listing
- `/projects/:id` - Project details
- `/blog` - Blog articles listing
- `/blog/:slug` - Article reading view
- `/contact` - Contact form
- `/login` - Authentication
- `/privacy-policy` - Privacy policy (GDPR)
- `/legal` - Legal notice (mentions legales)
- `/error` - Dynamic error page (404, 500)

**Admin Routes** (requires `ROLE_ADMIN`):
- `/admin` - Dashboard
- `/admin/projects` - Manage projects
- `/admin/skills` - Manage skills
- `/admin/tags` - Manage tags
- `/admin/cv` - Upload/manage CV
- `/admin/experiences` - Manage experiences
- `/admin/articles` - Manage blog articles
- `/admin/audit` - Audit logs viewer

**Demo Routes** (no authentication required):
- `/admindemo` - Demo admin with mock data (read-only)

### Nested Admin Routes

Admin routes use nested routing with `AdminLayoutComponent` as shell:
- Shared admin navigation and header
- Single guard protection for all admin routes
- Consistent admin UI layout

---

## 5. Service Layer

### Services Overview

| Service | Purpose |
|---------|---------|
| `AuthService` | Authentication management |
| `TokenStorageService` | JWT token storage |
| `ProjectService` | Project CRUD operations |
| `ArticleService` | Article management |
| `ExperienceService` | Experience management |
| `SkillService` | Skills management |
| `TagService` | Tag management |
| `ContactService` | Contact form submission |
| `CvService` | CV management |
| `LoggerService` | Centralized logging |
| `SearchService` | Full-text search (Elasticsearch) |
| `BatchService` | Batch jobs management |
| `CircuitBreakerService` | Circuit breaker status |
| `ActiveUsersService` | Real-time active users (SSE) |
| `VisitorTrackerService` | Visitor tracking |
| `SeoService` | Dynamic SEO management |

### Service Pattern

- `providedIn: 'root'` for singleton services
- `inject()` function for dependency injection
- RxJS operators for error handling
- Centralized logging via `LoggerService`

---

## 6. Guards and Interceptors

### Route Guards

#### authGuard

**Purpose**: Protect routes requiring authentication

**Behavior**:
- Allows access if authenticated
- Redirects to `/login` with `returnUrl` parameter if not

#### adminGuard

**Purpose**: Protect admin-only routes

**Behavior**:
- Allows access if authenticated AND has `ROLE_ADMIN`
- Redirects to `/login` if not authenticated
- Redirects to `/` (home) if authenticated but not admin

### HTTP Interceptors

#### JWT Interceptor

**Responsibilities**:
- Attach `Authorization: Bearer {token}` header
- Handle 401 Unauthorized errors
- Attempt automatic token refresh
- Retry failed requests with new token

**Token Refresh Flow**:
```
1. Request fails with 401
2. Attempt token refresh
3. If refresh succeeds → Retry original request
4. If refresh fails → Logout user, redirect to login
```

#### Retry Interceptor

**Purpose**: Retry failed HTTP requests with exponential backoff

**Configuration**:
- Max retries: 3
- Delay strategy: Exponential backoff
- Retryable errors: 5xx server errors, network failures

#### Logging Interceptor

**Purpose**: Log HTTP requests and responses for debugging

---

## 7. Component Organization

### Public Components

**Reusable Components** (`components/`):
- `HomeComponent` - Homepage with featured projects
- `NavbarComponent` - Navigation bar with auth state
- `ProjectCardComponent` - Project preview card
- `ArticleCardComponent` - Article preview card
- `ContactFormComponent` - Contact form widget

**Page Components** (`pages/`):
- `LoginComponent` - Authentication page
- `ContactComponent` - Contact page
- `ArticleListComponent` - Blog listing
- `ArticleDetailComponent` - Article reading

### Admin Components

**Admin Layout** (`pages/admin/admin-layout/`):
- Shell component for all admin pages
- Shared admin navigation sidebar
- Session monitoring

**Admin Pages Pattern**:
- **List Component**: Display, filter, delete operations
- **Form Component**: Create and edit operations (reused for both)

| Module | List Component | Form Component |
|--------|----------------|----------------|
| Projects | `AdminProjectListComponent` | `ProjectFormComponent` |
| Skills | `SkillListComponent` | `SkillFormComponent` |
| Tags | `TagListComponent` | `TagFormComponent` |
| Articles | `AdminArticleListComponent` | `ArticleFormComponent` |
| Experiences | `ExperienceListComponent` | `ExperienceFormComponent` |

---

## 8. State Management

### Token Storage

**Service**: `TokenStorageService`

**Storage Mechanism**: `localStorage`

**Stored Data**:
- Access token (JWT)
- Refresh token (UUID)
- Token expiration timestamp

### Authentication State

**Service**: `AuthService`

**State Methods**:
- `isAuthenticated()` - Check if user has valid token
- `isAdmin()` - Check if user has ROLE_ADMIN
- `getToken()` - Retrieve access token

**No Global State Library**: Uses service-based state management instead of Redux/NgRx

---

## 9. Directives

Custom directives for reusable DOM behaviors.

| Directive | Purpose | Usage |
|-----------|---------|-------|
| `LazyImageDirective` | Lazy load images when entering viewport | Uses Intersection Observer API |
| `AsyncImageDirective` | Load processing images with retry | Exponential backoff for images being processed |
| `ClickOutsideDirective` | Detect clicks outside element | Close dropdowns, modals |
| `DemoDisabledDirective` | Disable controls in demo mode | Prevent mutations in demo admin |

### LazyImageDirective

**Location**: `directives/lazy-image.directive.ts`

Delays image loading until the element enters the viewport, improving initial page load performance.

### AsyncImageDirective

**Location**: `directives/async-image.directive.ts`

Handles images that may still be processing on the backend (WebP conversion). Implements exponential backoff retry logic until the image is available.

---

## 10. Skeleton Loaders

Loading state components displaying shimmer animations while content loads.

| Component | Purpose |
|-----------|---------|
| `SkeletonArticleCardComponent` | Article card loading state |
| `SkeletonArticleDetailComponent` | Article detail page loading state |
| `SkeletonProjectCardComponent` | Project card loading state |
| `SkeletonProjectDetailComponent` | Project detail page loading state |
| `SkeletonSkillCardComponent` | Skill card loading state |
| `SkeletonTableRowComponent` | Table row loading state (audit logs) |
| `SkeletonTimelineItemComponent` | Timeline item loading state (experiences) |

**Location**: `components/shared/skeleton/`

**Usage Pattern**:
```html
@if (loading) {
  <app-skeleton-project-card />
} @else {
  <app-project-card [project]="project" />
}
```

---

## Related Documentation

- [Security: JWT Implementation](../security/jwt-implementation.md) - JWT token system
- [Security: Authentication](../security/authentication.md) - Auth architecture
- [API Overview](../api/README.md) - Backend API endpoints
- [Development: Setup](../development/setup.md) - Development environment
