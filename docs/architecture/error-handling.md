# Error Handling Architecture

---

## Table of Contents
1. [Overview](#1-overview)
2. [Global Exception Handler](#2-global-exception-handler)
3. [Custom Exceptions](#3-custom-exceptions)
4. [Error Response Formats](#4-error-response-formats)
5. [HTTP Status Code Mapping](#5-http-status-code-mapping)
6. [Validation Error Handling](#6-validation-error-handling)
7. [Logging Strategy](#7-logging-strategy)

---

## 1. Overview

The application uses **centralized exception handling** via Spring's `@RestControllerAdvice` for consistent error responses across all REST endpoints.

**Key Features**:
- Centralized error handling with `GlobalExceptionHandler`
- Custom exception types for domain-specific errors
- Structured error response formats (JSON)
- Comprehensive logging of all exceptions
- Differentiation between client errors (4xx) and server errors (5xx)
- Special handling for validation errors with field-level details
- JWT-specific exception handling

**Handler Class**: `com.emmanuelgabe.portfolio.exception.GlobalExceptionHandler`

---

## 2. Global Exception Handler

### GlobalExceptionHandler

**Annotation**: `@RestControllerAdvice`

**Purpose**: Intercept all exceptions thrown by controllers and convert them to appropriate HTTP responses

**Pattern**:
```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExceptionType.class)
    public ResponseEntity<ErrorResponse> handleExceptionType(ExceptionType ex) {
        log.warn("[EXCEPTION] Description - message={}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.STATUS_CODE.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.STATUS_CODE).body(errorResponse);
    }
}
```

### Exception Handlers Implemented

| Exception Type | HTTP Status | Log Level | Description |
|----------------|-------------|-----------|-------------|
| `ResourceNotFoundException` | 404 | WARN | Resource not found (Project, Skill, etc.) |
| `InvalidCredentialsException` | 401 | WARN | Invalid username/password |
| `InvalidTokenException` | 401 | WARN | Invalid JWT token |
| `TokenExpiredException` | 401 | WARN | Expired JWT token |
| `SignatureException` | 401 | ERROR | Invalid JWT signature |
| `ExpiredJwtException` | 401 | WARN | JWT expired (jjwt library) |
| `MalformedJwtException` | 401 | ERROR | Malformed JWT token |
| `UnsupportedJwtException` | 401 | ERROR | Unsupported JWT token |
| `AuthenticationException` | 401 | WARN | Spring Security authentication failure |
| `AccessDeniedException` | 403 | WARN | Insufficient permissions |
| `IllegalArgumentException` | 400 | WARN | Invalid method argument |
| `FileStorageException` | 400/500 | WARN/ERROR | File upload/storage error (context-dependent) |
| `EmailException` | 500 | ERROR | Email sending failure |
| `MethodArgumentNotValidException` | 400 | WARN | Validation error (field-level) |
| `Exception` | 500 | ERROR | Unexpected errors (catch-all) |

---

## 3. Custom Exceptions

### ResourceNotFoundException

**Purpose**: Indicate requested resource does not exist

**Usage**:
```java
throw new ResourceNotFoundException("Project not found with id: " + id);

// Or with structured message:
throw new ResourceNotFoundException("Project", "id", id);
// → "Project not found with id : '123'"
```

**HTTP Status**: 404 Not Found

**Use Cases**:
- Project not found
- Skill not found
- Tag not found
- Experience not found
- Article not found
- User not found

---

### InvalidCredentialsException

**Purpose**: Indicate authentication failure due to incorrect credentials

**Usage**:
```java
throw new InvalidCredentialsException("Invalid username or password");
```

**HTTP Status**: 401 Unauthorized

**Use Cases**:
- Login with incorrect username
- Login with incorrect password

---

### InvalidTokenException

**Purpose**: Indicate JWT token is invalid

**Usage**:
```java
throw new InvalidTokenException("Invalid refresh token");
```

**HTTP Status**: 401 Unauthorized

**Use Cases**:
- Invalid refresh token
- Token manipulation detected

---

### TokenExpiredException

**Purpose**: Indicate JWT token has expired

**Usage**:
```java
throw new TokenExpiredException("Refresh token has expired");
```

**HTTP Status**: 401 Unauthorized

**Use Cases**:
- Expired access token
- Expired refresh token

---

### FileStorageException

**Purpose**: Indicate file upload or storage failure

**Usage**:
```java
throw new FileStorageException("File type not allowed");
throw new FileStorageException("Failed to store file", cause);
```

**HTTP Status**: **Context-dependent**
- **400 Bad Request** - Validation errors (client's fault):
  - Invalid file type
  - File size exceeds limit
  - Empty file
  - Invalid file name
- **500 Internal Server Error** - Storage errors (server's fault):
  - Failed to create directory
  - Failed to write file
  - Disk space issues

**Detection Logic**:
```java
boolean isValidationError = ex.getMessage().contains("File type not allowed")
    || ex.getMessage().contains("exceeds maximum")
    || ex.getMessage().contains("empty")
    || ex.getMessage().contains("Invalid file name");
```

---

### EmailException

**Purpose**: Indicate email sending failure

**Usage**:
```java
throw new EmailException("Failed to send email: " + e.getMessage());
```

**HTTP Status**: 500 Internal Server Error

**User-Facing Message**: "Failed to send email. Please try again later."
- Hides internal error details for security

**Use Cases**:
- SMTP server unavailable
- Invalid email configuration
- Network failure

---

## 4. Error Response Formats

### Standard Error Response

**Class**: `ErrorResponse`

**Structure**:
```json
{
  "status": 404,
  "message": "Project not found with id: 123",
  "timestamp": "2025-11-19T14:23:45.123"
}
```

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| `status` | integer | HTTP status code |
| `message` | string | Human-readable error message |
| `timestamp` | datetime | Error occurrence timestamp (ISO 8601) |

---

### Validation Error Response

**Class**: `ValidationErrorResponse` (extends `ErrorResponse`)

**Structure**:
```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2025-11-19T14:23:45.123",
  "errors": {
    "title": "must not be blank",
    "description": "must not be blank",
    "githubUrl": "must be a valid URL"
  }
}
```

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| `status` | integer | HTTP status code (400) |
| `message` | string | "Validation failed" |
| `timestamp` | datetime | Error occurrence timestamp |
| `errors` | object | Field-level validation errors (field → error message) |

**Benefits**:
- Client can display field-specific error messages
- Clear indication of which fields failed validation
- Supports multiple field errors in single response

---

## 5. HTTP Status Code Mapping

### 400 Bad Request

**Scenarios**:
- Validation errors (`MethodArgumentNotValidException`)
- Invalid method arguments (`IllegalArgumentException`)
- File validation errors (invalid type, size exceeded, etc.)

**Client Action**: Fix request data and retry

---

### 401 Unauthorized

**Scenarios**:
- Invalid credentials (`InvalidCredentialsException`)
- Invalid JWT token (`InvalidTokenException`, `MalformedJwtException`)
- Expired JWT token (`TokenExpiredException`, `ExpiredJwtException`)
- JWT signature invalid (`SignatureException`)
- Authentication failure (`AuthenticationException`)

**Client Action**: Re-authenticate (login again)

---

### 403 Forbidden

**Scenarios**:
- Insufficient permissions (`AccessDeniedException`)
- Authenticated but lacks required role (ROLE_ADMIN)

**Client Action**: Request access upgrade or navigate to allowed area

---

### 404 Not Found

**Scenarios**:
- Resource not found (`ResourceNotFoundException`)

**Client Action**: Verify resource ID and try again or navigate elsewhere

---

### 500 Internal Server Error

**Scenarios**:
- File storage failure (`FileStorageException` - server errors)
- Email sending failure (`EmailException`)
- Unexpected exceptions (`Exception` catch-all)

**Client Action**: Retry later or contact support

---

## 6. Validation Error Handling

### Jakarta Validation Integration

**Trigger**: `@Valid` annotation on controller method parameters

**Example Controller**:
```java
@PostMapping
public ResponseEntity<ProjectResponse> createProject(
    @Valid @RequestBody CreateProjectRequest request
) {
    // Validation performed automatically before method execution
}
```

**Validation Annotations**:
```java
public class CreateProjectRequest {
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Email(message = "Email must be valid")
    private String email;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
             message = "Color must be a valid hex color code")
    private String color;
}
```

### Validation Error Response

**Handler**:
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(
    MethodArgumentNotValidException ex
) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
        String fieldName = ((FieldError) error).getField();
        String errorMessage = error.getDefaultMessage();
        errors.put(fieldName, errorMessage);
    });

    log.warn("[EXCEPTION] Validation failed - errorCount={}, fields={}",
        errors.size(), errors.keySet());

    ValidationErrorResponse errorResponse = new ValidationErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        "Validation failed",
        LocalDateTime.now(),
        errors
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
}
```

**Example Response**:
```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2025-11-19T14:23:45.123",
  "errors": {
    "title": "Title is required",
    "description": "Description must be between 10 and 5000 characters",
    "email": "Email must be valid"
  }
}
```

---

## 7. Logging Strategy

### Log Levels by Exception Type

**WARN Level** (client errors, expected scenarios):
- `ResourceNotFoundException`
- `InvalidCredentialsException`
- `InvalidTokenException`
- `TokenExpiredException`
- `ExpiredJwtException`
- `AuthenticationException`
- `AccessDeniedException`
- `IllegalArgumentException`
- `FileStorageException` (validation errors)
- `MethodArgumentNotValidException`

**ERROR Level** (server errors, unexpected scenarios):
- `SignatureException`
- `MalformedJwtException`
- `UnsupportedJwtException`
- `FileStorageException` (storage errors)
- `EmailException`
- `Exception` (catch-all)

### Logging Format

**Pattern**:
```
[EXCEPTION] <Description> - <contextual key-value pairs>
```

**Examples**:
```
[EXCEPTION] Resource not found - message=Project not found with id: 123
[EXCEPTION] Invalid credentials - message=Invalid username or password
[EXCEPTION] Validation failed - errorCount=3, fields=[title, description, email]
[EXCEPTION] File validation error - message=File type not allowed
[EXCEPTION] Email sending failed - message=SMTP server unavailable
[EXCEPTION] Unexpected error - type=NullPointerException, message=...
```

### Defensive Copy in ValidationErrorResponse

**Purpose**: Prevent internal representation exposure

**Implementation**:
```java
public Map<String, String> getErrors() {
    return new HashMap<>(errors);  // Return defensive copy
}
```

**Benefit**: External code cannot modify internal errors map

---

## Related Documentation

- [API Overview](../api/README.md) - API endpoints
- [Reference: Error Codes](../reference/error-codes.md) - Complete error code reference
- [Development: Logging Conventions](../development/logging-conventions.md) - Logging standards
