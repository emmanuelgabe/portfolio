# Logging Categories

Context categories and examples for structured logging.

---

## Table of Contents

1. [Backend Categories](#1-backend-categories)
2. [Frontend Categories](#2-frontend-categories)
3. [Script Categories](#3-script-categories)
4. [Best Practices](#4-best-practices)
5. [Examples](#5-examples)

---

## 1. Backend Categories

### 1.1 Application Lifecycle

```
[STARTUP]         Application startup
[SHUTDOWN]        Application shutdown
[HEALTH_CHECK]    Health verification
```

### 1.2 HTTP Operations

```
[HTTP_REQUEST]    Incoming HTTP request
[HTTP_RESPONSE]   HTTP response sent
```

### 1.3 Entity Operations

```
[CREATE_PROJECT]  Project creation
[UPDATE_PROJECT]  Project update
[DELETE_PROJECT]  Project deletion
[LIST_PROJECTS]   Project listing
[GET_PROJECT]     Project retrieval

[CREATE_ARTICLE]  Article creation
[UPDATE_ARTICLE]  Article update
[DELETE_ARTICLE]  Article deletion
[PUBLISH_ARTICLE] Article published
[UNPUBLISH_ARTICLE] Article unpublished

[CREATE_SKILL]    Skill creation
[UPDATE_SKILL]    Skill update
[DELETE_SKILL]    Skill deletion

[CREATE_TAG]      Tag creation
[UPDATE_TAG]      Tag update
[DELETE_TAG]      Tag deletion
```

### 1.4 Authentication

```
[AUTH]            Authentication/Authorization
[LOGIN]           User login
[LOGOUT]          User logout
[TOKEN_REFRESH]   Token refresh
[ADMIN_SEEDER]    Admin user seeding
```

### 1.5 System Operations

```
[DB_QUERY]        Database query
[DB_ERROR]        Database error
[VALIDATION]      Data validation
[EXCEPTION]       Exception handling
[SECURITY]        Security operations
```

### 1.6 Batch Operations

```
[RUN_BATCH_JOB]      Batch job execution
[GET_BATCH_STATS]    Batch statistics
[GET_LAST_JOB]       Job history retrieval
```

### 1.7 Messaging

```
[EMAIL_SENDER]       Email sending
[IMAGE_PROCESSING]   Image processing
[AUDIT]              Audit logging
[KAFKA_PRODUCER]     Kafka event publishing
[RABBITMQ]           RabbitMQ operations
```

---

## 2. Frontend Categories

### 2.1 Application Lifecycle

```
[INIT]            Component initialization
[DESTROY]         Component destruction
```

### 2.2 HTTP Operations

```
[HTTP]            HTTP request (simplified format)
[HTTP_REQUEST]    HTTP request sent
[HTTP_SUCCESS]    HTTP success response
[HTTP_ERROR]      HTTP error response
```

### 2.3 User Interaction

```
[USER_ACTION]     User interaction
[FORM_SUBMIT]     Form submission
[FORM_ERROR]      Form validation error
[NAVIGATION]      Router navigation
```

### 2.4 State Management

```
[STATE_CHANGE]    Application state change
[AUTH]            Authentication state
```

### 2.5 Batch Operations

```
[BATCH]           Batch job operations
```

---

## 3. Script Categories

### 3.1 Deployment

```
[DEPLOYMENT]      Deployment operations
[BUILD]           Build operations
[CONTAINER]       Docker container operations
```

### 3.2 Validation

```
[VALIDATION]      Configuration validation
[HEALTH_CHECK]    Health verification
```

### 3.3 Operations

```
[CLEANUP]         Resource cleanup
[DATABASE]        Database operations
[STEP]            Process step marker
```

---

## 4. Best Practices

### 4.1 Do

**Log at appropriate levels:**
```java
log.info("[CREATE_PROJECT] Success - id={}", id);  // Important event
log.debug("[GET_PROJECT] Fetching - id={}", id);   // Development info
```

**Include context in error logs:**
```java
log.error("[DB_ERROR] Failed to save - entity={}, error={}",
    entity, ex.getMessage(), ex);
```

**Use structured parameters:**
```java
log.info("[UPDATE_PROJECT] Success - id={}, changedFields={}", id, fields);
```

**Log performance when relevant:**
```java
log.info("[SEARCH] Completed - query={}, results={}, duration={}ms",
    query, results.size(), duration);
```

### 4.2 Do Not

**Never log sensitive data:**
```java
// BAD
log.info("User login - password={}", password);

// GOOD
log.info("[AUTH] Login attempt - username={}", username);
```

**Do not log large payloads:**
```java
// BAD
log.info("Request: {}", fullRequestBody);

// GOOD
log.debug("[HTTP_REQUEST] Received - endpoint={}, method={}", endpoint, method);
```

**Do not use string concatenation:**
```java
// BAD
log.info("[CREATE] Created project with id " + id);

// GOOD
log.info("[CREATE_PROJECT] Success - id={}", id);
```

**Do not use emojis:**
```bash
# BAD
echo "Success!"

# GOOD
log_success "[DEPLOYMENT] Complete"
```

---

## 5. Examples

### 5.1 Controller CRUD

```java
@Slf4j
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        log.debug("[LIST_PROJECTS] Request received");
        List<ProjectResponse> projects = projectService.getAllProjects();
        log.info("[LIST_PROJECTS] Success - count={}", projects.size());
        return ResponseEntity.ok(projects);
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        log.info("[CREATE_PROJECT] Request received - title={}", request.getTitle());
        ProjectResponse response = projectService.createProject(request);
        log.info("[CREATE_PROJECT] Success - id={}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request) {
        log.info("[UPDATE_PROJECT] Request received - id={}", id);
        ProjectResponse response = projectService.updateProject(id, request);
        log.info("[UPDATE_PROJECT] Success - id={}", response.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        log.info("[DELETE_PROJECT] Request received - id={}", id);
        projectService.deleteProject(id);
        log.info("[DELETE_PROJECT] Success - id={}", id);
        return ResponseEntity.noContent().build();
    }
}
```

### 5.2 Frontend HTTP Service

```typescript
@Injectable({ providedIn: 'root' })
export class ArticleService {
  private readonly logger = inject(LoggerService);

  getAll(): Observable<ArticleResponse[]> {
    return this.http.get<ArticleResponse[]>(this.apiUrl).pipe(
      catchError(error => {
        this.logger.error('[HTTP_ERROR] Failed to fetch articles', {
          status: error.status
        });
        return throwError(() => error);
      })
    );
  }

  create(request: CreateArticleRequest): Observable<ArticleResponse> {
    this.logger.info('[HTTP] POST /api/articles', { title: request.title });

    return this.http.post<ArticleResponse>(this.apiUrl, request).pipe(
      tap(response => {
        this.logger.info('[HTTP_SUCCESS] Article created', { id: response.id });
      }),
      catchError(error => {
        this.logger.error('[HTTP_ERROR] Failed to create article', {
          title: request.title,
          status: error.status
        });
        return throwError(() => error);
      })
    );
  }

  publish(id: number): Observable<ArticleResponse> {
    this.logger.info('[HTTP] POST /api/articles/{id}/publish', { id });

    return this.http.post<ArticleResponse>(`${this.apiUrl}/${id}/publish`, {}).pipe(
      tap(() => {
        this.logger.info('[HTTP_SUCCESS] Article published', { id });
      }),
      catchError(error => {
        this.logger.error('[HTTP_ERROR] Failed to publish article', {
          id,
          status: error.status
        });
        return throwError(() => error);
      })
    );
  }
}
```

### 5.3 Deployment Script

```bash
#!/bin/bash
source "$(dirname "$0")/../lib/logging.sh"

set -e

ENV=${1:-staging}
COMMIT=$(git rev-parse --short HEAD)

log_section "Deployment to $ENV"
log_info "[DEPLOYMENT] Starting - env=$ENV, commit=$COMMIT"

log_step "[VALIDATION] Checking prerequisites"
if ! command -v docker &> /dev/null; then
    log_fatal "[VALIDATION] Docker not found"
fi
log_success "[VALIDATION] Prerequisites satisfied"

log_step "[BUILD] Building images"
START_TIME=$(date +%s)
if docker-compose build; then
    BUILD_TIME=$(($(date +%s) - START_TIME))
    log_success "[BUILD] Images built - duration=${BUILD_TIME}s"
else
    log_fatal "[BUILD] Failed to build images"
fi

log_step "[CONTAINER] Starting services"
docker-compose up -d
log_success "[CONTAINER] Services started"

log_step "[HEALTH_CHECK] Verifying health"
ATTEMPT=0
MAX_ATTEMPTS=30

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    ATTEMPT=$((ATTEMPT + 1))
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' portfolio-backend-$ENV 2>/dev/null || echo "not_running")

    if [ "$STATUS" = "healthy" ]; then
        log_success "[HEALTH_CHECK] All services healthy"
        break
    fi

    if [ "$STATUS" = "unhealthy" ]; then
        log_fatal "[HEALTH_CHECK] Service unhealthy"
    fi

    sleep 5
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    log_fatal "[HEALTH_CHECK] Timeout after ${MAX_ATTEMPTS} attempts"
fi

log_success "[DEPLOYMENT] Complete - env=$ENV"
```

---

## Related Documentation

- [Logging Conventions](./logging-conventions.md) - Log format and levels
- [Code Quality](./code-quality.md) - Quality standards
- [Testing Guide](./testing-guide.md) - Testing conventions
