# Role-Based Access Control (RBAC)

---

## Table of Contents
1. [Overview](#1-overview)
2. [Roles and Permissions](#2-roles-and-permissions)
3. [Backend Authorization](#3-backend-authorization)
4. [Frontend Route Guards](#4-frontend-route-guards)
5. [Permission Matrix](#5-permission-matrix)
6. [Implementation Details](#6-implementation-details)

---

## 1. Overview

The Portfolio application implements **Role-Based Access Control (RBAC)** to manage user permissions.

**Authorization Model**:
- **Backend**: Spring Security method-level authorization
- **Frontend**: Angular route guards
- **Token-Based**: User roles embedded in JWT access token

**Key Principles**:
- Public endpoints require no authentication
- Admin endpoints require ROLE_ADMIN
- Authorization checked on both frontend and backend
- Backend is the source of truth (frontend guards are UX only)

---

## 2. Roles and Permissions

### Available Roles

| Role | Authority | Description |
|------|-----------|-------------|
| **USER** | `ROLE_USER` | Standard authenticated user |
| **ADMIN** | `ROLE_ADMIN` | Administrator with full access |

**Note**: In Spring Security, role names are prefixed with `ROLE_` in authorities.

### Role Hierarchy

```
ADMIN (has explicit admin permissions)
  └── USER (basic authenticated access)
```

---

## 3. Backend Authorization

### Endpoint Protection

**SecurityFilterChain Configuration**:

```yaml
# Public endpoints (no authentication)
- /health, /api/version, /actuator/health
- /api/auth/**
- POST /api/contact
- /swagger-ui/**, /v3/api-docs/**
- /uploads/**
- /api/cv/current, /api/cv/download
- GET /api/projects, /api/skills, /api/tags, /api/experiences, /api/articles

# Admin endpoints (ROLE_ADMIN required)
- /api/admin/**

# All other endpoints
- Require authentication
```

### Method-Level Authorization

**Using @PreAuthorize**:

```java
@PreAuthorize("hasRole('ADMIN')")              // Single role
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")   // Multiple roles
@PreAuthorize("hasAuthority('ROLE_ADMIN')")    // By authority
@PreAuthorize("isAuthenticated()")             // Any authenticated user
```

---

## 4. Frontend Route Guards

### Auth Guard

**Purpose**: Protect routes requiring authentication

**Behavior**:
- Allows access if authenticated
- Redirects to `/login` with `returnUrl` parameter if not

### Admin Guard

**Purpose**: Protect admin-only routes

**Behavior**:
- Allows access if authenticated AND has `ROLE_ADMIN`
- Redirects to `/login` if not authenticated
- Redirects to `/` (home) if authenticated but not admin

---

## 5. Permission Matrix

### API Endpoints Summary

| Category | Public | USER | ADMIN |
|----------|--------|------|-------|
| `GET /api/projects` | Yes | Yes | Yes |
| `POST /api/admin/projects` | No | No | Yes |
| `PUT /api/admin/projects/{id}` | No | No | Yes |
| `DELETE /api/admin/projects/{id}` | No | No | Yes |
| `GET /api/skills` | Yes | Yes | Yes |
| `POST /api/admin/skills` | No | No | Yes |
| `GET /api/articles` | Yes | Yes | Yes |
| `POST /api/admin/articles` | No | No | Yes |
| `GET /api/experiences` | Yes | Yes | Yes |
| `POST /api/admin/experiences` | No | No | Yes |
| `GET /api/cv/current` | Yes | Yes | Yes |
| `POST /api/admin/cv/upload` | No | No | Yes |
| `POST /api/contact` | Yes | Yes | Yes |
| `POST /api/auth/login` | Yes | Yes | Yes |
| `POST /api/auth/logout` | No | Yes | Yes |

**Legend**: Yes = Allowed, No = Forbidden (401 or 403)

### Frontend Routes

| Route | Public | USER | ADMIN |
|-------|--------|------|-------|
| `/` | Yes | Yes | Yes |
| `/login` | Yes | Yes | Yes |
| `/projects` | Yes | Yes | Yes |
| `/blog` | Yes | Yes | Yes |
| `/admin/**` | No | No | Yes |

---

## 6. Implementation Details

### User Entity with Roles

**Database Schema**:
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);
```

**Role Enum**:
```java
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}
```

### JWT Token with Roles

**Token Payload**:
```json
{
  "type": "access",
  "sub": "admin",
  "authorities": [
    { "authority": "ROLE_ADMIN" }
  ],
  "iss": "portfolio-backend",
  "aud": "portfolio-frontend",
  "iat": 1700000000,
  "exp": 1700000900
}
```

---

## Related Documentation

- [Security: Authentication](./authentication.md) - JWT authentication architecture
- [Security: Password Management](./password-management.md) - Password policies
- [API: Authentication](../api/authentication.md) - Authentication endpoints
