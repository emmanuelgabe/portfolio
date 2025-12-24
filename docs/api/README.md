# API Reference Overview

---

## Table of Contents
1. [Introduction](#1-introduction)
2. [Base URLs](#2-base-urls)
3. [Authentication](#3-authentication)
4. [Common Headers](#4-common-headers)
5. [Response Format](#5-response-format)
6. [HTTP Status Codes](#6-http-status-codes)
7. [API Endpoints Overview](#7-api-endpoints-overview)
8. [Interactive Documentation](#8-interactive-documentation)

---

## 1. Introduction

This document provides an overview of the Portfolio REST API. The API follows RESTful principles and uses JSON for request and response payloads.

**API Version**: 1.0
**Base Technology**: Spring Boot 3.x REST API
**Authentication**: JWT (JSON Web Tokens)

---

## 2. Base URLs

Base URLs vary depending on the environment:

| Environment | Base URL                   | Port | Notes |
|-------------|----------------------------|------|-------|
| **Local Development** | `http://localhost:8080`    | 8080 | Docker or direct execution |
| **Staging** | `http://localhost:3001`    | 3001 | Docker Compose staging |
| **Production** | `https://emmanuelgabe.com` | 443 | HTTPS only |

**API Path Prefix**: All endpoints are prefixed with `/api`

**Example**:
```
Local:      http://localhost:8080/api/projects
Staging:    http://localhost:3001/api/projects
Production: https://api.yoursite.com/api/projects
```

---

## 3. Authentication

The API uses **JWT (JSON Web Tokens)** for authentication.

**Authentication Flow**:
1. Obtain tokens via `POST /api/auth/login`
2. Include access token in `Authorization` header for protected endpoints
3. Refresh expired access tokens via `POST /api/auth/refresh`

**Token Types**:
- **Access Token**: Short-lived (15 minutes), used for API requests
- **Refresh Token**: Long-lived (7 days), used to obtain new access tokens

**Protected Routes**:
- Public routes: `/api/projects`, `/api/skills`, `/api/cv/current`
- Admin routes: `/api/admin/**` (requires `ROLE_ADMIN`)

See [authentication.md](./authentication.md) for detailed authentication documentation.

---

## 4. Common Headers

### Request Headers

**For Authentication Endpoints**:
```http
Content-Type: application/json
```

**For Protected Endpoints**:
```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

**For File Upload Endpoints**:
```http
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

### Response Headers

```http
Content-Type: application/json; charset=UTF-8
```

---

## 5. Response Format

### Success Response

**Standard Success (2xx)**:
```json
{
  "id": 1,
  "title": "Project Name",
  "description": "Project description",
  "createdAt": "2025-11-15T10:30:00"
}
```

**List Response**:
```json
[
  {
    "id": 1,
    "name": "Item 1"
  },
  {
    "id": 2,
    "name": "Item 2"
  }
]
```

### Error Response

**Standard Error Format**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for field 'title': must not be blank",
  "path": "/api/admin/projects"
}
```

**Validation Error (400)**:
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

**Authentication Error (401)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/admin/projects"
}
```

**Authorization Error (403)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied: insufficient permissions",
  "path": "/api/admin/projects"
}
```

**Not Found (404)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Project not found with id: 123",
  "path": "/api/admin/projects/123"
}
```

---

## 6. HTTP Status Codes

The API uses standard HTTP status codes:

### Success Codes (2xx)

| Code | Meaning | Usage |
|------|---------|-------|
| `200 OK` | Success | GET, PUT, DELETE successful |
| `201 Created` | Resource created | POST successful |
| `204 No Content` | Success without body | DELETE successful (alternative) |

### Client Error Codes (4xx)

| Code | Meaning | Usage |
|------|---------|-------|
| `400 Bad Request` | Invalid request | Validation errors, malformed JSON |
| `401 Unauthorized` | Authentication required | Missing or invalid token |
| `403 Forbidden` | Insufficient permissions | Valid token but wrong role |
| `404 Not Found` | Resource not found | Entity doesn't exist |
| `409 Conflict` | Conflict with current state | Duplicate entry, constraint violation |

### Server Error Codes (5xx)

| Code | Meaning | Usage |
|------|---------|-------|
| `500 Internal Server Error` | Server error | Unexpected server-side error |

---

## 7. API Endpoints Overview

### 7.1 Authentication

**Base Path**: `/api/auth`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/login` | Public | Authenticate user |
| POST | `/refresh` | Public | Refresh access token |
| POST | `/logout` | Required | Invalidate refresh token |
| POST | `/change-password` | Required | Change user password |

See [authentication.md](./authentication.md) for details.

### 7.2 Projects

**Base Path**: `/api/projects` (public), `/api/admin/projects` (admin)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/projects` | Public | List all projects |
| GET | `/api/projects/{id}` | Public | Get project details |
| POST | `/api/admin/projects` | Admin | Create project |
| PUT | `/api/admin/projects/{id}` | Admin | Update project |
| DELETE | `/api/admin/projects/{id}` | Admin | Delete project |
| POST | `/api/admin/projects/{id}/upload-image` | Admin | Upload project image |
| DELETE | `/api/admin/projects/{id}/delete-image` | Admin | Delete project image |

See [projects.md](./projects.md) for details.

### 7.3 Skills

**Base Path**: `/api/skills` (public), `/api/admin/skills` (admin)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/skills` | Public | List all skills |
| GET | `/api/skills/{id}` | Public | Get skill details |
| POST | `/api/admin/skills` | Admin | Create skill |
| PUT | `/api/admin/skills/{id}` | Admin | Update skill |
| DELETE | `/api/admin/skills/{id}` | Admin | Delete skill |

See [skills.md](./skills.md) for details.

### 7.4 CV Management

**Base Path**: `/api/cv` (public), `/api/admin/cv` (admin)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/cv/current` | Public | Get current CV |
| GET | `/api/cv/download/{id}` | Public | Download specific CV |
| POST | `/api/admin/cv/upload` | Admin | Upload new CV |
| GET | `/api/admin/cv/all` | Admin | List all CVs |
| PUT | `/api/admin/cv/{id}/set-current` | Admin | Set CV as current |
| DELETE | `/api/admin/cv/{id}` | Admin | Delete CV |

See [cv.md](./cv.md) for details.

### 7.5 Experiences

**Base Path**: `/api/experiences`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/experiences` | Public | List all experiences |
| GET | `/api/experiences/{id}` | Public | Get experience details |
| GET | `/api/experiences/type/{type}` | Public | Get experiences by type |
| GET | `/api/experiences/ongoing` | Public | Get ongoing experiences |
| GET | `/api/experiences/recent` | Public | Get recent experiences |
| POST | `/api/experiences/admin` | Admin | Create experience |
| PUT | `/api/experiences/admin/{id}` | Admin | Update experience |
| DELETE | `/api/experiences/admin/{id}` | Admin | Delete experience |

See [experiences.md](./experiences.md) for details.

### 7.6 Tags

**Base Path**: `/api/tags`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/tags` | Public | List all tags |
| GET | `/api/tags/{id}` | Public | Get tag details |
| POST | `/api/tags/admin` | Admin | Create tag |
| PUT | `/api/tags/admin/{id}` | Admin | Update tag |
| DELETE | `/api/tags/admin/{id}` | Admin | Delete tag |

See [tags.md](./tags.md) for details.

### 7.7 Contact

**Base Path**: `/api/contact`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/contact` | Public | Send contact message (rate limited) |

See [contact.md](./contact.md) for details.

### 7.8 Articles

**Base Path**: `/api/articles`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/articles` | Public | List published articles |
| GET | `/api/articles/paginated` | Public | List articles with pagination |
| GET | `/api/articles/{slug}` | Public | Get article by slug |
| GET | `/api/articles/admin` | Admin | List all articles (including drafts) |
| GET | `/api/articles/admin/{id}` | Admin | Get article by ID |
| POST | `/api/articles/admin` | Admin | Create article |
| PUT | `/api/articles/admin/{id}` | Admin | Update article |
| DELETE | `/api/articles/admin/{id}` | Admin | Delete article |
| PUT | `/api/articles/admin/{id}/publish` | Admin | Publish article |
| PUT | `/api/articles/admin/{id}/unpublish` | Admin | Unpublish article |

See [articles-api.md](./articles-api.md) for details.

### 7.9 File Upload

**Base Path**: `/api/admin/upload`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/admin/upload` | Admin | Generic file upload |

See [files.md](./files.md) for details.

### 7.10 Site Configuration

**Base Path**: `/api/configuration` (public), `/api/admin/configuration` (admin)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/configuration` | Public | Get site configuration |
| GET | `/api/admin/configuration` | Admin | Get site configuration (admin) |
| PUT | `/api/admin/configuration` | Admin | Update site configuration |
| POST | `/api/admin/configuration/profile-image` | Admin | Upload profile image |
| DELETE | `/api/admin/configuration/profile-image` | Admin | Delete profile image |

See [site-configuration.md](./site-configuration.md) for details.

### 7.11 GraphQL API

**Base Path**: `/graphql`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/graphql` | Public/Admin | GraphQL endpoint |
| GET | `/graphiql` | Public | GraphiQL interactive UI |

See [graphql.md](./graphql.md) for schema and queries.

### 7.12 Search API

**Base Path**: `/api/admin/search`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/admin/search/articles` | Admin | Search articles |
| GET | `/api/admin/search/projects` | Admin | Search projects |
| GET | `/api/admin/search/experiences` | Admin | Search experiences |
| POST | `/api/admin/search/reindex` | Admin | Rebuild indices |

See [search.md](./search.md) for details.

### 7.13 Audit API

**Base Path**: `/api/admin/audit`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/admin/audit` | Admin | List audit logs |
| GET | `/api/admin/audit/entity/{type}/{id}` | Admin | Entity history |
| GET | `/api/admin/audit/stats` | Admin | Audit statistics |
| GET | `/api/admin/audit/export/csv` | Admin | Export to CSV |
| GET | `/api/admin/audit/export/json` | Admin | Export to JSON |

See [audit.md](./audit.md) for details.

### 7.14 Visitors API

**Base Path**: `/api/visitors` (public), `/api/admin/visitors` (admin)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/visitors/heartbeat` | Public | Register heartbeat |
| GET | `/api/admin/visitors/stream` | Admin | SSE stream |
| GET | `/api/admin/visitors/count` | Admin | Active count |
| GET | `/api/admin/visitors/stats` | Admin | Visitor stats |
| GET | `/api/admin/visitors/daily` | Admin | Daily data |

See [visitors.md](./visitors.md) for details.

### 7.15 Batch API

**Base Path**: `/api/admin/batch`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/admin/batch/image-reprocessing/stats` | Admin | Reprocessing stats |
| GET | `/api/admin/batch/image-reprocessing/last` | Admin | Last job info |
| POST | `/api/admin/batch/image-reprocessing/run` | Admin | Run job |

### 7.16 Circuit Breakers API

**Base Path**: `/api/admin/circuit-breakers`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/admin/circuit-breakers` | Admin | All circuit breakers |
| GET | `/api/admin/circuit-breakers/{name}` | Admin | Specific breaker |

### 7.17 Health Check

**Base Path**: `/actuator`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/actuator/health` | Public | Application health status |

Returns `{"status": "UP"}` when application is healthy.

---

## 8. Interactive Documentation

### Swagger UI

The API provides interactive documentation via Swagger UI:

**Local Development**:
```
http://localhost:8080/swagger-ui.html
```

**Staging**:
```
http://localhost:3001/swagger-ui.html
```

**Features**:
- Interactive endpoint testing
- Request/response schema documentation
- Authentication token management
- Example values for all models

### OpenAPI JSON Specification

Download the OpenAPI 3.0 specification:

```
http://localhost:8080/api-docs
```

This JSON file can be imported into tools like Postman, Insomnia, or used to generate client SDKs.

---

## Related Documentation

- [Authentication Guide](./authentication.md) - JWT authentication details
- [Projects API](./projects.md) - Project management endpoints
- [Skills API](./skills.md) - Skill management endpoints
- [Experiences API](./experiences.md) - Experience management endpoints
- [Tags API](./tags.md) - Tag management endpoints
- [Contact API](./contact.md) - Contact form endpoint
- [Articles API](./articles-api.md) - Blog articles endpoints
- [CV API](./cv.md) - CV management endpoints
- [Site Configuration API](./site-configuration.md) - Site configuration endpoints
- [Files API](./files.md) - File upload endpoints
- [Security: RBAC](../security/rbac.md) - Role-based access control
- [CI/CD Documentation](../deployment/ci-cd.md) - Deployment pipeline
