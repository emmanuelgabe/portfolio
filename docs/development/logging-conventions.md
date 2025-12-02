# Logging Conventions

---

## Table of Contents

1. [Overview](#1-overview)
2. [Log Format](#2-log-format)
3. [Log Levels](#3-log-levels)
4. [Backend Logging](#4-backend-logging)
5. [Frontend Logging](#5-frontend-logging)
6. [Script Logging](#6-script-logging)
7. [Context Categories](#7-context-categories)
8. [Best Practices](#8-best-practices)
9. [Examples](#9-examples)

---

## 1. Overview

This guide defines the logging conventions for all components of the Portfolio application. Consistent logging helps with debugging, monitoring, and maintaining the application in production.

### 1.1 General Principles

1. **Professional Format**: No emojis, only text-based indicators
2. **Structured Messages**: Parseable by log aggregation tools
3. **Consistent Naming**: Same patterns across all components
4. **Contextual Information**: Include relevant parameters
5. **English Only**: All log messages in English
6. **Security**: Never log sensitive information (passwords, tokens, PII)

---

## 2. Log Format

### 2.1 Standard Format

```
[TIMESTAMP] [LEVEL] [CONTEXT] Message - key1=value1, key2=value2
```

### 2.2 Components

| Component | Description | Example |
|-----------|-------------|---------|
| TIMESTAMP | ISO 8601 format | `2025-01-12 14:30:45` |
| LEVEL | Severity code | `INF`, `ERR`, `WRN` |
| CONTEXT | Operation category | `CREATE_PROJECT`, `HTTP_REQUEST` |
| Message | Human-readable description | `Project created successfully` |
| Parameters | Key-value pairs | `id=123, title=MyProject` |

### 2.3 Example

```
[2025-01-12 14:30:45] [INF] [CREATE_PROJECT] Success - id=123, title=MyProject, tags=3
```

---

## 3. Log Levels

| Level | Code | When to Use | Example |
|-------|------|-------------|---------|
| **TRACE** | TRC | Very detailed debugging, variable dumps | Variable values, internal state |
| **DEBUG** | DBG | Development debugging | Method parameters, flow control |
| **INFO** | INF | General information, normal operations | Service started, operation completed |
| **WARN** | WRN | Abnormal but handled situations | Retry attempts, fallback used, deprecated API |
| **ERROR** | ERR | Errors requiring attention | Failed operations, exceptions |
| **FATAL** | FTL | Critical errors, application shutdown | Database connection lost, disk full |

### 3.1 Level Selection Guidelines

- **Production default**: `INFO` and above
- **Staging default**: `DEBUG` and above
- **Development default**: `TRACE` and above

---

## 4. Backend Logging

### 4.1 Setup with Lombok

Add to `build.gradle`:

```gradle
dependencies {
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

### 4.2 Basic Usage

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        log.info("[CREATE_PROJECT] Request received - title={}", request.getTitle());

        try {
            ProjectResponse response = projectService.createProject(request);
            log.info("[CREATE_PROJECT] Success - id={}, title={}", response.getId(), response.getTitle());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ResourceNotFoundException ex) {
            log.warn("[CREATE_PROJECT] Resource not found - resource={}, id={}",
                ex.getResourceName(), ex.getId());
            throw ex;
        } catch (Exception ex) {
            log.error("[CREATE_PROJECT] Unexpected error - title={}, error={}",
                request.getTitle(), ex.getMessage(), ex);
            throw ex;
        }
    }
}
```

### 4.3 Service Layer Logging

```java
@Slf4j
@Service
@Transactional
public class ProjectService {

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        log.debug("[LIST_PROJECTS] Fetching all projects");
        List<Project> projects = projectRepository.findAll();
        log.debug("[LIST_PROJECTS] Found {} projects", projects.size());
        return projects.stream()
            .map(ProjectResponse::fromEntity)
            .collect(Collectors.toList());
    }
}
```

### 4.4 Performance Logging

```java
public List<ProjectResponse> searchProjects(String query) {
    long startTime = System.currentTimeMillis();
    log.debug("[SEARCH_PROJECTS] Query started - query={}", query);

    List<ProjectResponse> results = performSearch(query);

    long duration = System.currentTimeMillis() - startTime;
    log.info("[SEARCH_PROJECTS] Completed - query={}, results={}, duration={}ms",
        query, results.size(), duration);

    if (duration > 1000) {
        log.warn("[SEARCH_PROJECTS] Slow query detected - query={}, duration={}ms", query, duration);
    }

    return results;
}
```

---

## 5. Frontend Logging

### 5.1 LoggerService

Create a centralized logging service at `src/app/services/logger.service.ts`:

```typescript
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export enum LogLevel {
  TRACE = 0,
  DEBUG = 1,
  INFO = 2,
  WARN = 3,
  ERROR = 4,
  FATAL = 5
}

@Injectable({
  providedIn: 'root'
})
export class LoggerService {
  private readonly levelMap: Record<string, LogLevel> = {
    TRACE: LogLevel.TRACE,
    DEBUG: LogLevel.DEBUG,
    INFO: LogLevel.INFO,
    WARN: LogLevel.WARN,
    ERROR: LogLevel.ERROR,
    FATAL: LogLevel.FATAL
  };

  private get minLevel(): LogLevel {
    return this.levelMap[environment.logLevel] || LogLevel.INFO;
  }

  info(message: string, context?: unknown): void {
    this.log(LogLevel.INFO, message, context);
  }

  error(message: string, context?: unknown): void {
    this.log(LogLevel.ERROR, message, context);
  }

  private log(level: LogLevel, message: string, context?: unknown): void {
    if (level < this.minLevel) {
      return;
    }

    const timestamp = new Date().toISOString();
    const levelName = LogLevel[level];
    const contextStr = context ? ` ${JSON.stringify(context)}` : '';
    const formattedMessage = `[${timestamp}] [${levelName}] ${message}${contextStr}`;

    this.logToConsole(level, formattedMessage);

    if (environment.production && level >= LogLevel.ERROR) {
      this.logToServer(level, message, context);
    }
  }

  private logToConsole(level: LogLevel, message: string): void {
    switch (level) {
      case LogLevel.TRACE:
      case LogLevel.DEBUG:
        console.debug(message);
        break;
      case LogLevel.INFO:
        console.info(message);
        break;
      case LogLevel.WARN:
        console.warn(message);
        break;
      case LogLevel.ERROR:
      case LogLevel.FATAL:
        console.error(message);
        break;
    }
  }

  private logToServer(_level: LogLevel, _message: string, _context?: unknown): void {
    // Server-side logging not implemented - use external service (Sentry, etc.) if needed
  }
}
```

### 5.2 Service Usage

```typescript
import { Injectable, inject } from '@angular/core';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private readonly logger = inject(LoggerService);

  create(request: CreateProjectRequest): Observable<ProjectResponse> {
    this.logger.info('[HTTP_REQUEST] Creating project', { title: request.title });

    return this.http.post<ProjectResponse>(this.apiUrl, request).pipe(
      tap(response => {
        this.logger.info('[HTTP_SUCCESS] Project created', {
          id: response.id,
          title: response.title
        });
      }),
      catchError(error => {
        this.logger.error('[HTTP_ERROR] Failed to create project', {
          title: request.title,
          status: error.status,
          message: error.message
        });
        return throwError(() => error);
      })
    );
  }
}
```

---

## 6. Script Logging

### 6.1 Helper Library

Source the logging library at `scripts/lib/logging.sh`:

```bash
source "$(dirname "$0")/../lib/logging.sh"
```

### 6.2 Usage

```bash
#!/bin/bash
source "$(dirname "$0")/../lib/logging.sh"

ENV=${1:-staging}

log_section "Deployment to $ENV"
log_info "[DEPLOYMENT] Starting - env=$ENV, commit=$(git rev-parse --short HEAD)"

log_step "[VALIDATION] Checking prerequisites"
if ! command -v docker &> /dev/null; then
    log_fatal "[VALIDATION] Docker not found"
fi
log_success "[VALIDATION] Prerequisites satisfied"

log_step "[BUILD] Building images"
if docker-compose build; then
    log_success "[BUILD] Images built"
else
    log_fatal "[BUILD] Failed to build images"
fi

log_success "[DEPLOYMENT] Deployment complete - env=$ENV"
```

### 6.3 Simple Format

For simple scripts without helper library:

```bash
echo "[INFO] Starting process"
echo "[ERROR] Failed" >&2
```

---

## 7. Context Categories

### 7.1 Backend Categories

```
[STARTUP]         Application startup
[SHUTDOWN]        Application shutdown
[HTTP_REQUEST]    Incoming HTTP request
[DB_QUERY]        Database query
[DB_ERROR]        Database error
[CREATE_PROJECT]  Project creation
[UPDATE_PROJECT]  Project update
[DELETE_PROJECT]  Project deletion
[CREATE_SKILL]    Skill creation
[UPDATE_SKILL]    Skill update
[DELETE_SKILL]    Skill deletion
[VALIDATION]      Data validation
[AUTH]            Authentication/Authorization
[PERFORMANCE]     Performance metrics
```

### 7.2 Frontend Categories

```
[INIT]            Component initialization
[HTTP_REQUEST]    HTTP request
[HTTP_SUCCESS]    HTTP success
[HTTP_ERROR]      HTTP error
[STATE_CHANGE]    Application state change
[NAVIGATION]      Router navigation
[USER_ACTION]     User interaction
[FORM_SUBMIT]     Form submission
[FORM_ERROR]      Form error
```

### 7.3 Script Categories

```
[DEPLOYMENT]      Deployment operations
[HEALTH_CHECK]    Health verification
[CLEANUP]         Resource cleanup
[VALIDATION]      Configuration validation
[CONTAINER]       Docker container operations
[DATABASE]        Database operations
[STEP]            Process step
```

---

## 8. Best Practices

### 8.1 Do

1. **Log at appropriate levels**
   ```java
   log.info("[CREATE_PROJECT] Success - id={}", id);  // Important business event
   log.debug("[GET_PROJECT] Fetching - id={}", id);   // Development info
   ```

2. **Include context in error logs**
   ```java
   log.error("[DB_ERROR] Failed to save - entity={}, error={}", entity, ex.getMessage(), ex);
   ```

3. **Use structured parameters**
   ```java
   log.info("[UPDATE_PROJECT] Success - id={}, changedFields={}", id, changedFields);
   ```

4. **Log performance metrics**
   ```java
   log.info("[PERFORMANCE] Query completed - query={}, duration={}ms", query, duration);
   ```

### 8.2 Do Not

1. **Never log sensitive data**
   ```java
   // BAD
   log.info("User login - password={}", password);

   // GOOD
   log.info("[AUTH] User login attempt - username={}", username);
   ```

2. **Do not log large payloads in production**
   ```java
   // BAD
   log.info("Request: {}", fullRequestBody);

   // GOOD
   log.debug("[HTTP_REQUEST] Request received - endpoint={}, method={}", endpoint, method);
   ```

3. **Do not use string concatenation**
   ```java
   // BAD
   log.info("[CREATE_PROJECT] Created project with id " + id);

   // GOOD
   log.info("[CREATE_PROJECT] Success - id={}", id);
   ```

4. **Do not use emojis or special characters**
   ```bash
   # BAD
   echo "Success!"

   # GOOD
   log_success "[DEPLOYMENT] Deployment complete"
   ```

---

## 9. Examples

### 9.1 Complete CRUD Operation

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

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable Long id) {
        log.debug("[GET_PROJECT] Request received - id={}", id);
        ProjectResponse project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        log.info("[CREATE_PROJECT] Request received - title={}", request.getTitle());
        ProjectResponse response = projectService.createProject(request);
        log.info("[CREATE_PROJECT] Success - id={}, title={}", response.getId(), response.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request) {
        log.info("[UPDATE_PROJECT] Request received - id={}", id);
        ProjectResponse response = projectService.updateProject(id, request);
        log.info("[UPDATE_PROJECT] Success - id={}, title={}", response.getId(), response.getTitle());
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

### 9.2 Error Handling

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("[EXCEPTION] Resource not found - resource={}, field={}, value={}",
            ex.getResourceName(), ex.getFieldName(), ex.getFieldValue());

        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        log.error("[EXCEPTION] Unexpected error - message={}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

### 9.3 Deployment Script

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

log_step "[DEPLOYMENT] Starting containers"
docker-compose up -d

log_step "[HEALTH_CHECK] Waiting for healthy status"
MAX_ATTEMPTS=60
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    ATTEMPT=$((ATTEMPT + 1))
    BACKEND_HEALTH=$(docker inspect --format='{{.State.Health.Status}}' portfolio-backend-$ENV 2>/dev/null || echo "not_running")

    log_debug "[HEALTH_CHECK] Attempt $ATTEMPT/$MAX_ATTEMPTS - backend=$BACKEND_HEALTH"

    if [ "$BACKEND_HEALTH" = "healthy" ]; then
        log_success "[HEALTH_CHECK] All systems healthy"
        break
    fi

    if [ "$BACKEND_HEALTH" = "unhealthy" ]; then
        log_error "[HEALTH_CHECK] Container unhealthy - backend=$BACKEND_HEALTH"
        docker logs --tail=50 portfolio-backend-$ENV
        exit 1
    fi

    sleep 5
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    log_fatal "[HEALTH_CHECK] Timeout after ${MAX_ATTEMPTS} attempts"
fi

log_success "[DEPLOYMENT] Deployment complete - env=$ENV"
```
