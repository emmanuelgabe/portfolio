# API Error Codes Reference

---

## Table of Contents
1. [Error Response Format](#1-error-response-format)
2. [HTTP Status Codes](#2-http-status-codes)
3. [Common Error Messages](#3-common-error-messages)

---

## 1. Error Response Format

### Standard Error Response

```json
{
  "status": 404,
  "message": "Resource not found with id: 123",
  "timestamp": "2025-11-19T14:23:45.123Z"
}
```

### Validation Error Response

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2025-11-19T14:23:45.123Z",
  "errors": {
    "field1": "Error message for field1",
    "field2": "Error message for field2"
  }
}
```

---

## 2. HTTP Status Codes

| Code | Status | Description | Common Causes |
|------|--------|-------------|---------------|
| 200 | OK | Success | Request completed successfully |
| 201 | Created | Resource created | POST request succeeded |
| 204 | No Content | Success, no response body | DELETE request succeeded |
| 400 | Bad Request | Invalid request | Validation errors, invalid arguments |
| 401 | Unauthorized | Authentication required | Invalid/expired token, invalid credentials |
| 403 | Forbidden | Insufficient permissions | Missing ROLE_ADMIN |
| 404 | Not Found | Resource doesn't exist | Invalid ID, resource deleted |
| 409 | Conflict | Duplicate resource | Unique constraint violation |
| 429 | Too Many Requests | Rate limit exceeded | Contact form spam prevention |
| 500 | Internal Server Error | Server error | Unexpected exceptions, storage failures |

---

## 3. Common Error Messages

### Resource Not Found (404)

| Resource | Error Message |
|----------|---------------|
| Project | `Project not found with id: {id}` |
| Skill | `Skill not found with id: {id}` |
| Tag | `Tag not found with id: {id}` |
| Experience | `Experience not found with id: {id}` |
| Article | `Article not found with slug: {slug}` |
| Article | `Article not found with id: {id}` |
| User | `User not found with id: {id}` |
| User | `User not found with username: {username}` |

### Authentication Errors (401)

| Scenario | Error Message |
|----------|---------------|
| Invalid credentials | `Invalid username or password` |
| Invalid token | `Invalid JWT token` |
| Expired token | `JWT token has expired` |
| Invalid signature | `Invalid JWT signature` |
| Malformed token | `Invalid JWT token` |
| Unsupported token | `Unsupported JWT token` |
| Invalid refresh token | `Invalid refresh token` |
| Expired refresh token | `Refresh token has expired` |

### Authorization Errors (403)

| Scenario | Error Message |
|----------|---------------|
| Insufficient permissions | `Access denied: insufficient permissions` |
| Not admin | Redirect to home (no error message) |

### Validation Errors (400)

| Field Type | Validation | Error Message |
|------------|------------|---------------|
| Title | Blank | `Title is required` |
| Title | Too short/long | `Title must be between {min} and {max} characters` |
| Email | Blank | `Email is required` |
| Email | Invalid format | `Email must be valid` |
| Description | Blank | `Description is required` |
| Description | Too short/long | `Description must be between {min} and {max} characters` |
| URL | Invalid format | `must be a valid URL` |
| Color | Invalid hex | `Color must be a valid hex color code (e.g., #FF5733)` |
| Date | Future date | `{field} cannot be in the future` |

### File Upload Errors (400/500)

| Scenario | Status | Error Message |
|----------|--------|---------------|
| Invalid file type | 400 | `Invalid file type. Supported types: JPEG, PNG, WebP` |
| File too large | 400 | `File size exceeds maximum allowed size (10 MB)` |
| Empty file | 400 | `File cannot be empty` |
| Storage failure | 500 | `Failed to store file` |

### Rate Limiting (429)

| Endpoint | Error Message |
|----------|---------------|
| Contact form | `Rate limit exceeded. You can send maximum {N} messages per hour. Please try again later.` |

### Duplicate Resource (409)

| Resource | Error Message |
|----------|---------------|
| Tag | `Tag with name '{name}' already exists` |
| User | `Username already exists` |
| User | `Email already exists` |

### Email Errors (500)

| Scenario | Error Message |
|----------|---------------|
| Send failure | `Failed to send email. Please try again later.` |

---

## Related Documentation

- [Architecture: Error Handling](../architecture/error-handling.md) - Error handling strategy
- [API Overview](../api/README.md) - API endpoints
