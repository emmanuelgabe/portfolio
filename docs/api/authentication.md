# Authentication API

---

## Table of Contents
1. [Overview](#1-overview)
2. [Authentication Flow](#2-authentication-flow)
3. [Endpoints](#3-endpoints)
   - 3.1 [Login](#31-login)
   - 3.2 [Refresh Token](#32-refresh-token)
   - 3.3 [Logout](#33-logout)
   - 3.4 [Change Password](#34-change-password)
4. [Token Management](#4-token-management)
5. [Error Handling](#5-error-handling)

---

## 1. Overview

The Portfolio API uses **JWT (JSON Web Tokens)** for stateless authentication. The system provides two types of tokens:

- **Access Token**: Short-lived (15 minutes), used for API requests
- **Refresh Token**: Long-lived (7 days), used to obtain new access tokens

**Base Path**: `/api/auth`

**Security Features**:
- BCrypt password hashing
- Token expiration management
- Refresh token rotation
- Role-based access control (RBAC)

---

## 2. Authentication Flow

```
┌─────────┐                ┌─────────┐                ┌──────────┐
│ Client  │                │   API   │                │ Database │
└────┬────┘                └────┬────┘                └────┬─────┘
     │                          │                          │
     │  POST /auth/login        │                          │
     │  (username, password)    │                          │
     ├─────────────────────────>│                          │
     │                          │  Verify credentials      │
     │                          ├─────────────────────────>│
     │                          │<─────────────────────────┤
     │                          │  Store refresh token     │
     │                          ├─────────────────────────>│
     │  {accessToken,           │                          │
     │   refreshToken}          │                          │
     │<─────────────────────────┤                          │
     │                          │                          │
     │  API Request             │                          │
     │  Authorization: Bearer   │                          │
     │<─────────────────────────>│                          │
     │                          │                          │
     │  POST /auth/refresh      │                          │
     │  (refreshToken)          │                          │
     ├─────────────────────────>│                          │
     │                          │  Validate & rotate token │
     │                          ├─────────────────────────>│
     │  {new accessToken,       │                          │
     │   new refreshToken}      │                          │
     │<─────────────────────────┤                          │
     │                          │                          │
     │  POST /auth/logout       │                          │
     ├─────────────────────────>│                          │
     │                          │  Invalidate token        │
     │                          ├─────────────────────────>│
     │  200 OK                  │                          │
     │<─────────────────────────┤                          │
```

---

## 3. Endpoints

### 3.1 Login

Authenticate a user and receive JWT tokens.

**Endpoint**: `POST /api/auth/login`

**Authentication**: None (public endpoint)

**Request Body**:
```json
{
  "username": "admin",
  "password": "your-password"
}
```

**Request Schema**:
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `username` | string | Yes | Not blank | User's username |
| `password` | string | Yes | Not blank | User's password |

**Success Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "username": "admin",
  "roles": ["ROLE_ADMIN"]
}
```

**Response Schema**:
| Field | Type | Description |
|-------|------|-------------|
| `accessToken` | string | JWT access token (15 min validity) |
| `refreshToken` | string | JWT refresh token (7 days validity) |
| `tokenType` | string | Always "Bearer" |
| `expiresIn` | number | Access token expiration in seconds (900) |
| `username` | string | Authenticated user's username |
| `roles` | array | User's roles (e.g., ["ROLE_ADMIN", "ROLE_USER"]) |

**Error Responses**:

**Invalid Credentials (401 Unauthorized)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password",
  "path": "/api/auth/login"
}
```

**Validation Error (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "username",
      "rejectedValue": "",
      "message": "must not be blank"
    }
  ],
  "path": "/api/auth/login"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "your-password"
  }'
```

**Example Request (JavaScript)**:
```javascript
const response = await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: 'admin',
    password: 'your-password'
  })
});

const data = await response.json();
// Store tokens
localStorage.setItem('accessToken', data.accessToken);
localStorage.setItem('refreshToken', data.refreshToken);
```

---

### 3.2 Refresh Token

Obtain a new access token using a valid refresh token.

**Endpoint**: `POST /api/auth/refresh`

**Authentication**: None (public endpoint, requires valid refresh token)

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Request Schema**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `refreshToken` | string | Yes | Valid refresh token from login |

**Success Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Response Schema**:
| Field | Type | Description |
|-------|------|-------------|
| `accessToken` | string | New JWT access token (15 min validity) |
| `refreshToken` | string | New JWT refresh token (7 days validity) |
| `tokenType` | string | Always "Bearer" |
| `expiresIn` | number | Access token expiration in seconds (900) |

**Error Responses**:

**Invalid Token (401 Unauthorized)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid refresh token",
  "path": "/api/auth/refresh"
}
```

**Expired Token (401 Unauthorized)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Refresh token expired",
  "path": "/api/auth/refresh"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }'
```

**Example Request (JavaScript)**:
```javascript
const refreshToken = localStorage.getItem('refreshToken');

const response = await fetch('http://localhost:8080/api/auth/refresh', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ refreshToken })
});

const data = await response.json();
// Update stored tokens
localStorage.setItem('accessToken', data.accessToken);
localStorage.setItem('refreshToken', data.refreshToken);
```

---

### 3.3 Logout

Invalidate a refresh token.

**Endpoint**: `POST /api/auth/logout`

**Authentication**: Required (Bearer token)

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Request Schema**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `refreshToken` | string | Yes | Refresh token to invalidate |

**Success Response** (200 OK):
```json
{
  "message": "Logout successful"
}
```

**Error Responses**:

**Unauthorized (401)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/auth/logout"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }'
```

**Example Request (JavaScript)**:
```javascript
const accessToken = localStorage.getItem('accessToken');
const refreshToken = localStorage.getItem('refreshToken');

await fetch('http://localhost:8080/api/auth/logout', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({ refreshToken })
});

// Clear local storage
localStorage.removeItem('accessToken');
localStorage.removeItem('refreshToken');
```

---

### 3.4 Change Password

Change the authenticated user's password.

**Endpoint**: `POST /api/auth/change-password`

**Authentication**: Required (Bearer token)

**Request Body**:
```json
{
  "currentPassword": "old-password",
  "newPassword": "new-password"
}
```

**Request Schema**:
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `currentPassword` | string | Yes | Not blank | Current password |
| `newPassword` | string | Yes | Min 8 chars | New password |

**Success Response** (200 OK):
```json
{
  "message": "Password changed successfully"
}
```

**Error Responses**:

**Invalid Current Password (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Current password is incorrect",
  "path": "/api/auth/change-password"
}
```

**Validation Error (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "newPassword",
      "rejectedValue": "short",
      "message": "must be at least 8 characters"
    }
  ],
  "path": "/api/auth/change-password"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/auth/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "currentPassword": "old-password",
    "newPassword": "new-secure-password"
  }'
```

---

## 4. Token Management

### Access Token

**Validity**: 15 minutes
**Usage**: Include in `Authorization` header for all protected endpoints
**Format**: `Authorization: Bearer <access_token>`

**Token Structure**:
```json
{
  "type": "access",
  "sub": "admin",
  "authorities": [
    {"authority": "ROLE_ADMIN"}
  ],
  "iss": "portfolio-backend",
  "aud": "portfolio-frontend",
  "iat": 1700000000,
  "exp": 1700000900
}
```

### Refresh Token

**Validity**: 7 days
**Usage**: Obtain new access tokens via `/api/auth/refresh`
**Storage**: Database (`refresh_tokens` table) with user association

**Token Structure**:
```json
{
  "type": "refresh",
  "sub": "admin",
  "iss": "portfolio-backend",
  "aud": "portfolio-frontend",
  "iat": 1700000000,
  "exp": 1700604800
}
```

### Token Rotation

When refreshing tokens:
1. Old refresh token is invalidated in the database
2. New refresh token is generated and stored
3. New access token is generated
4. Both tokens are returned to the client

**Best Practice**: Always update both tokens in client storage after refresh.

---

## 5. Error Handling

### Common Error Scenarios

| Scenario | HTTP Status | Error Message |
|----------|-------------|---------------|
| Invalid credentials | 401 | "Invalid username or password" |
| Expired access token | 401 | "Token has expired" |
| Invalid token format | 401 | "Invalid token format" |
| Expired refresh token | 401 | "Refresh token expired" |
| Token not found in DB | 401 | "Invalid refresh token" |
| Missing Authorization header | 401 | "Missing or invalid Authorization header" |
| Wrong password (change-password) | 400 | "Current password is incorrect" |
| Validation errors | 400 | "Validation failed" with field details |

### Handling Token Expiration (Frontend)

**Recommended Approach**:

```javascript
// HTTP Interceptor for automatic token refresh
async function fetchWithTokenRefresh(url, options = {}) {
  const accessToken = localStorage.getItem('accessToken');

  // Add access token to request
  options.headers = {
    ...options.headers,
    'Authorization': `Bearer ${accessToken}`
  };

  let response = await fetch(url, options);

  // If 401, try to refresh token
  if (response.status === 401) {
    const refreshToken = localStorage.getItem('refreshToken');

    const refreshResponse = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });

    if (refreshResponse.ok) {
      const { accessToken, refreshToken: newRefreshToken } = await refreshResponse.json();
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', newRefreshToken);

      // Retry original request
      options.headers['Authorization'] = `Bearer ${accessToken}`;
      response = await fetch(url, options);
    } else {
      // Refresh failed, redirect to login
      window.location.href = '/login';
    }
  }

  return response;
}
```

---

## Related Documentation

- [API Overview](./README.md) - General API documentation
- [Security: Authentication Architecture](../security/authentication.md) - JWT implementation details and security considerations
- [Security: RBAC](../security/rbac.md) - Role-based access control
- [Security: Password Management](../security/password-management.md) - Password policies
