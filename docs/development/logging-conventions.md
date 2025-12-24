# Logging Conventions

Logging standards for all components of the Portfolio application.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Log Format](#2-log-format)
3. [Log Levels](#3-log-levels)
4. [Backend Logging](#4-backend-logging)
5. [Frontend Logging](#5-frontend-logging)
6. [Script Logging](#6-script-logging)

---

## 1. Overview

### 1.1 General Principles

1. **Professional Format**: No emojis, only text-based indicators
2. **Structured Messages**: Parseable by log aggregation tools
3. **Consistent Naming**: Same patterns across all components
4. **Contextual Information**: Include relevant parameters
5. **English Only**: All log messages in English
6. **Security**: Never log sensitive information (passwords, tokens, PII)

For context categories and examples, see [Logging Categories](./logging-categories.md).

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
[2025-01-12 14:30:45] [INF] [CREATE_PROJECT] Success - id=123, title=MyProject
```

---

## 3. Log Levels

| Level | Code | When to Use | Example |
|-------|------|-------------|---------|
| **TRACE** | TRC | Very detailed debugging | Variable values, internal state |
| **DEBUG** | DBG | Development debugging | Method parameters, flow control |
| **INFO** | INF | General information | Service started, operation completed |
| **WARN** | WRN | Abnormal but handled | Retry attempts, fallback used |
| **ERROR** | ERR | Errors requiring attention | Failed operations, exceptions |
| **FATAL** | FTL | Critical errors | Database connection lost |

### 3.1 Level Selection by Environment

- **Production**: `INFO` and above
- **Staging**: `DEBUG` and above
- **Development**: `TRACE` and above

---

## 4. Backend Logging

### 4.1 Setup with Lombok

```gradle
dependencies {
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

### 4.2 Controller Usage

```java
@Slf4j
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        log.info("[CREATE_PROJECT] Request received - title={}", request.getTitle());

        ProjectResponse response = projectService.createProject(request);
        log.info("[CREATE_PROJECT] Success - id={}, title={}",
            response.getId(), response.getTitle());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### 4.3 Service Layer Usage

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

### 4.4 Exception Handling

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex) {
        log.warn("[EXCEPTION] Resource not found - resource={}, id={}",
            ex.getResourceName(), ex.getId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        log.error("[EXCEPTION] Unexpected error - message={}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(500, "An unexpected error occurred"));
    }
}
```

---

## 5. Frontend Logging

### 5.1 LoggerService

Located at `src/app/services/logger.service.ts`:

```typescript
@Injectable({ providedIn: 'root' })
export class LoggerService {
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
    if (level < this.minLevel) return;

    const timestamp = new Date().toISOString();
    const levelName = LogLevel[level];
    const contextStr = context ? ` ${JSON.stringify(context)}` : '';
    const formattedMessage = `[${timestamp}] [${levelName}] ${message}${contextStr}`;

    this.logToConsole(level, formattedMessage);
  }
}
```

### 5.2 Service Usage

```typescript
@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly logger = inject(LoggerService);

  create(request: CreateProjectRequest): Observable<ProjectResponse> {
    this.logger.info('[HTTP] POST /api/projects', { title: request.title });

    return this.http.post<ProjectResponse>(this.apiUrl, request).pipe(
      catchError(error => {
        this.logger.error('[HTTP_ERROR] Failed to create project', {
          title: request.title,
          status: error.status
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

Source the logging library:

```bash
source "$(dirname "$0")/../lib/logging.sh"
```

### 6.2 Usage

```bash
#!/bin/bash
source "$(dirname "$0")/../lib/logging.sh"

ENV=${1:-staging}

log_section "Deployment to $ENV"
log_info "[DEPLOYMENT] Starting - env=$ENV"

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

log_success "[DEPLOYMENT] Complete - env=$ENV"
```

### 6.3 Simple Format

For simple scripts without helper library:

```bash
echo "[INFO] Starting process"
echo "[ERROR] Failed" >&2
```

---

## Related Documentation

- [Logging Categories](./logging-categories.md) - Context categories and examples
- [Code Quality](./code-quality.md) - Quality standards
- [Testing Guide](./testing-guide.md) - Testing conventions
