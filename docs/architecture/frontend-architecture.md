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

The frontend is built with **Angular 18** using **standalone components** architecture. Follows modern Angular patterns with functional guards, HTTP interceptors, and dependency injection.

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

### Key Dependencies

```json
{
  "dependencies": {
    "@angular/core": "^18.x",
    "@angular/common": "^18.x",
    "@angular/router": "^18.x",
    "bootstrap": "^5.3.x",
    "bootstrap-icons": "^1.11.x",
    "date-fns": "^3.x",
    "ngx-toastr": "^18.x",
    "rxjs": "^7.x"
  }
}
```

---

## 3. Project Structure

### Directory Organization

```
src/app/
├── components/          # Reusable UI components
│   ├── article-card/    # Article preview card
│   ├── contact-form/    # Contact form widget
│   ├── home/            # Homepage component
│   ├── navbar/          # Navigation bar
│   ├── project-card/    # Project preview card
│   ├── project-detail/  # Project detail view
│   ├── project-list/    # Projects listing
│   └── shared/          # Shared components
│       └── session-expiry-modal/  # Session timeout modal
├── pages/               # Page-level components
│   ├── admin/           # Admin area (protected)
│   │   ├── admin-layout/      # Admin shell layout
│   │   ├── dashboard/         # Admin dashboard
│   │   ├── articles/          # Article management
│   │   ├── experiences/       # Experience management
│   │   ├── projects/          # Project management
│   │   ├── skills/            # Skill management
│   │   ├── tags/              # Tag management
│   │   └── admin-cv/          # CV management
│   ├── blog/            # Blog public area
│   │   ├── article-list/      # Blog listing
│   │   └── article-detail/    # Article reading
│   ├── contact/         # Contact page
│   ├── login/           # Login page
│   └── timeline/        # Timeline page
├── services/            # Business logic and API communication
├── guards/              # Route protection
├── interceptors/        # HTTP middleware
├── models/              # TypeScript interfaces
└── app.routes.ts        # Route configuration
```

### File Naming Conventions

**Components**:
- `component-name.component.ts` - Component class
- `component-name.component.html` - Template
- `component-name.component.scss` - Styles
- `component-name.component.spec.ts` - Unit tests

**Services**:
- `service-name.service.ts` - Service class
- `service-name.service.spec.ts` - Unit tests

---

## 4. Routing Architecture

### Route Configuration

**File**: `app.routes.ts`

**Route Structure**:
```typescript
export const routes: Routes = [
  // Public routes
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'projects', component: ProjectListComponent },
  { path: 'projects/:id', component: ProjectDetailComponent },
  { path: 'contact', component: ContactComponent },
  { path: 'blog', component: ArticleListComponent },
  { path: 'blog/:slug', component: ArticleDetailComponent },

  // Admin routes (protected by adminGuard)
  {
    path: 'admin',
    canActivate: [adminGuard],
    component: AdminLayoutComponent,
    children: [
      { path: '', component: DashboardComponent },
      { path: 'projects', component: AdminProjectListComponent },
      { path: 'projects/new', component: ProjectFormComponent },
      { path: 'projects/:id/edit', component: ProjectFormComponent },
      { path: 'skills', component: SkillListComponent },
      { path: 'skills/new', component: SkillFormComponent },
      { path: 'skills/:id/edit', component: SkillFormComponent },
      { path: 'tags', component: TagListComponent },
      { path: 'tags/new', component: TagFormComponent },
      { path: 'tags/:id/edit', component: TagFormComponent },
      { path: 'cv', component: AdminCvComponent },
      { path: 'experiences', component: ExperienceListComponent },
      { path: 'experiences/new', component: ExperienceFormComponent },
      { path: 'experiences/edit/:id', component: ExperienceFormComponent },
      { path: 'articles', component: AdminArticleListComponent },
      { path: 'articles/new', component: ArticleFormComponent },
      { path: 'articles/:id/edit', component: ArticleFormComponent },
    ],
  },

  // Catch-all route
  { path: '**', redirectTo: '' },
];
```

### Public vs Admin Routes

**Public Routes** (unauthenticated access):
- `/` - Homepage with featured projects
- `/projects` - Projects listing
- `/projects/:id` - Project details
- `/blog` - Blog articles listing
- `/blog/:slug` - Article reading view
- `/contact` - Contact form
- `/login` - Authentication

**Admin Routes** (requires `ROLE_ADMIN`):
- `/admin` - Dashboard
- `/admin/projects` - Manage projects (list/create/edit/delete)
- `/admin/skills` - Manage skills
- `/admin/tags` - Manage tags
- `/admin/cv` - Upload/manage CV
- `/admin/experiences` - Manage timeline experiences
- `/admin/articles` - Manage blog articles

### Nested Admin Routes

**Pattern**: Admin routes use nested routing with `AdminLayoutComponent` as shell

**Benefits**:
- Shared admin navigation and header
- Consistent admin UI layout
- Single guard protection for all admin routes
- Lazy loading potential for admin modules

---

## 5. Service Layer

### Service Architecture

**Services** (`src/app/services/`):

| Service | Purpose | Key Methods |
|---------|---------|-------------|
| `AuthService` | Authentication management | `login()`, `logout()`, `refreshToken()`, `isAuthenticated()`, `isAdmin()` |
| `TokenStorageService` | JWT token storage | `saveToken()`, `getToken()`, `clearToken()`, `isTokenExpired()` |
| `ProjectService` | Project CRUD operations | `getAll()`, `getById()`, `create()`, `update()`, `delete()`, `uploadImage()` |
| `ArticleService` | Article management | `getAllPublished()`, `getBySlug()`, `create()`, `update()`, `publish()` |
| `ExperienceService` | Experience management | `getAll()`, `getByType()`, `getOngoing()`, `getRecent()` |
| `SkillService` | Skills management | `getAll()`, `create()`, `update()`, `delete()` |
| `TagService` | Tag management | `getAll()`, `create()`, `update()`, `delete()` |
| `ContactService` | Contact form submission | `sendMessage()` |
| `CvService` | CV management | `upload()`, `getCurrent()`, `getAll()` |
| `ImageService` | Image upload | `uploadProjectImage()`, `deleteProjectImage()` |
| `ArticleImageService` | Article image upload | `uploadImage()`, `deleteImage()` |
| `LoggerService` | Centralized logging | `debug()`, `info()`, `warn()`, `error()` |
| `ModalService` | Modal management | `openSessionExpiryModal()` |
| `ActivityMonitorService` | User activity tracking | `startMonitoring()`, `resetTimer()` |
| `I18nService` | Internationalization | `translate()`, `setLanguage()` |

### Service Patterns

**HTTP Service Pattern**:
```typescript
@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/projects`;

  getAll(): Observable<ProjectResponse[]> {
    return this.http.get<ProjectResponse[]>(this.apiUrl).pipe(
      catchError(error => {
        this.logger.error('[HTTP] Get projects failed', {
          status: error.status,
          message: error.message
        });
        return throwError(() => error);
      })
    );
  }
}
```

**Key Patterns**:
- `providedIn: 'root'` for singleton services
- `inject()` function for dependency injection
- RxJS operators for error handling
- Centralized logging via `LoggerService`

---

## 6. Guards and Interceptors

### Route Guards

**File**: `guards/auth.guard.ts`

**Guards Implemented**:

#### authGuard
```typescript
export const authGuard: CanActivateFn = (route, state): boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url }
  });
};
```

**Purpose**: Protect routes requiring authentication

**Behavior**:
- Allows access if authenticated
- Redirects to `/login` with `returnUrl` parameter if not

---

#### adminGuard
```typescript
export const adminGuard: CanActivateFn = (route, state): boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated() && authService.isAdmin()) {
    return true;
  }

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url }
    });
  }

  return router.createUrlTree(['/']);
};
```

**Purpose**: Protect admin-only routes

**Behavior**:
- Allows access if authenticated AND has `ROLE_ADMIN`
- Redirects to `/login` if not authenticated
- Redirects to `/` (home) if authenticated but not admin

---

### HTTP Interceptors

**Files**: `interceptors/*.interceptor.ts`

**Interceptors Implemented**:

#### JWT Interceptor
**File**: `jwt.interceptor.ts`

**Responsibilities**:
- Attach `Authorization: Bearer {token}` header to requests
- Handle 401 Unauthorized errors
- Attempt automatic token refresh
- Retry failed requests with new token
- Handle 403 Forbidden for expired tokens

**Token Refresh Flow**:
```
1. Request fails with 401
2. Attempt token refresh
3. If refresh succeeds:
   → Retry original request with new token
4. If refresh fails:
   → Logout user
   → Redirect to login
```

---

#### Retry Interceptor
**File**: `retry.interceptor.ts`

**Purpose**: Retry failed HTTP requests with exponential backoff

**Configuration**:
- Max retries: Configurable (default: 3)
- Delay strategy: Exponential backoff
- Retryable errors: 5xx server errors, network failures

---

#### Logging Interceptor
**File**: `logging.interceptor.ts`

**Purpose**: Log all HTTP requests and responses for debugging

**Logged Information**:
- Request method and URL
- Response status and timing
- Error details

---

## 7. Component Organization

### Public Components

**Reusable Components** (`components/`):
- `HomeComponent` - Homepage with featured projects and skills
- `NavbarComponent` - Navigation bar with authentication state
- `ProjectCardComponent` - Project preview card (used in listings)
- `ProjectListComponent` - Projects grid display
- `ProjectDetailComponent` - Single project detailed view
- `ArticleCardComponent` - Article preview card
- `ContactFormComponent` - Contact form widget

**Page Components** (`pages/`):
- `LoginComponent` - Authentication page
- `ContactComponent` - Contact page with form
- `ArticleListComponent` - Blog listing page
- `ArticleDetailComponent` - Article reading page
- `TimelineComponent` - Career timeline page

---

### Admin Components

**Admin Layout** (`pages/admin/admin-layout/`):
- Shell component for all admin pages
- Shared admin navigation sidebar
- Logout functionality
- Session monitoring

**Admin Pages** (`pages/admin/`):

| Module | List Component | Form Component | Purpose |
|--------|----------------|----------------|---------|
| Dashboard | `DashboardComponent` | N/A | Admin overview |
| Projects | `AdminProjectListComponent` | `ProjectFormComponent` | Manage projects |
| Skills | `SkillListComponent` | `SkillFormComponent` | Manage skills |
| Tags | `TagListComponent` | `TagFormComponent` | Manage tags |
| Experiences | `ExperienceListComponent` | `ExperienceFormComponent` | Manage timeline |
| Articles | `AdminArticleListComponent` | `ArticleFormComponent` | Manage blog |
| CV | `AdminCvComponent` | N/A | Upload/manage CV |

**Component Pattern**:
- **List Component**: Display, filter, delete operations
- **Form Component**: Create and edit operations (reused for both)

---

## 8. State Management

### Token Storage

**Service**: `TokenStorageService`

**Storage Mechanism**: `localStorage`

**Stored Data**:
- Access token (JWT)
- Refresh token (UUID)
- Token expiration timestamp

**Methods**:
```typescript
saveToken(token: string): void
getToken(): string | null
clearToken(): void
isTokenExpired(bufferSeconds: number): boolean
```

### Authentication State

**Service**: `AuthService`

**State Methods**:
```typescript
isAuthenticated(): boolean  // Check if user has valid token
isAdmin(): boolean          // Check if user has ROLE_ADMIN
getToken(): string | null   // Retrieve access token
```

**No Global State Library**: Uses service-based state management instead of Redux/NgRx

---

## Related Documentation

- [Security: JWT Implementation](../security/jwt-implementation.md) - JWT token system
- [Security: Authentication](../security/authentication.md) - Auth architecture
- [API Overview](../api/README.md) - Backend API endpoints
- [Development: Setup](../development/setup.md) - Development environment
