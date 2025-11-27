# Security Documentation

---

## Table of Contents
1. [Introduction](#1-introduction)
2. [Authentication](#2-authentication)
3. [Authorization](#3-authorization)
4. [Password Security](#4-password-security)
5. [Rate Limiting](#5-rate-limiting)
6. [Security Best Practices](#6-security-best-practices)
7. [Initial Setup](#7-initial-setup)

---

## 1. Introduction

This section provides comprehensive documentation of the Portfolio application security architecture. The application implements multiple layers of security including JWT authentication, role-based access control, password hashing, and rate limiting.

**Security Stack**:
- **Authentication**: JWT (JSON Web Tokens) with access + refresh tokens
- **Authorization**: Spring Security with role-based access control (RBAC)
- **Password Hashing**: BCrypt with salt (10 rounds)
- **Rate Limiting**: Redis-backed IP-based rate limiting
- **HTTPS**: SSL/TLS encryption in production

**Key Security Principles**:
- Defense in depth (multiple security layers)
- Least privilege (users have minimum required permissions)
- Stateless authentication (no server-side sessions)
- Secure by default (restrictive default configuration)
- Token rotation (automatic refresh token rotation)

---

## 2. Authentication

### 2.1 Authentication Architecture

**File**: [authentication.md](./authentication.md)

**Description**: Complete authentication architecture with JWT dual-token system, login flow, and token management.

**Key Features**:
- JWT dual-token system (access + refresh)
- Stateless authentication (no server-side sessions)
- Automatic token refresh via HTTP interceptor
- Token revocation on logout (refresh tokens)
- BCrypt password hashing

**Token Lifetimes**:
- Access Token: 15 minutes (JWT, stateless)
- Refresh Token: 7 days (UUID, stored in database)

**Authentication Flow**:
```
User Login → Credentials Validation → Generate Tokens
→ Frontend stores in localStorage → Attach to requests
→ Token expires (401) → Automatic refresh → Retry request
```

---

### 2.2 JWT Implementation

**File**: [jwt-implementation.md](./jwt-implementation.md)

**Description**: Detailed JWT implementation with token generation, validation, and refresh flow.

**Key Components**:
- **JwtTokenProvider**: Token generation and validation service
- **JwtAuthenticationFilter**: Spring Security filter for token validation
- **Frontend JWT Interceptor**: Automatic token refresh on 401

**Token Structure**:
```json
{
  "type": "access",
  "sub": "username",
  "authorities": [{"authority": "ROLE_ADMIN"}],
  "iss": "portfolio-backend",
  "aud": "portfolio-frontend",
  "iat": 1700000000,
  "exp": 1700000900
}
```

**Configuration**:
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}      # HS256 secret (256+ bits)
    expiration: 900000         # 15 minutes
    refresh-expiration: 7      # 7 days
```

---

## 3. Authorization

### 3.1 Role-Based Access Control (RBAC)

**File**: [rbac.md](./rbac.md)

**Description**: Complete RBAC implementation with roles, permissions, and endpoint protection.

**Roles**:

| Role | Authority | Description |
|------|-----------|-------------|
| **USER** | `ROLE_USER` | Standard authenticated user |
| **ADMIN** | `ROLE_ADMIN` | Administrator with full access |

**Endpoint Protection**:

| Endpoint Pattern | Required Role | Access |
|------------------|---------------|--------|
| `/api/auth/**` | Public | Login, refresh, logout |
| `/api/projects` | Public | Read-only access |
| `/api/articles` | Public | Published articles only |
| `/api/admin/**` | ROLE_ADMIN | All admin operations |

**Implementation**:
- Backend: Spring Security `SecurityFilterChain` configuration
- Frontend: Route guards (`authGuard`, `adminGuard`)
- Method-level: `@PreAuthorize("hasRole('ADMIN')")`

---

## 4. Password Security

### 4.1 Password Management

**File**: [password-management.md](./password-management.md)

**Description**: Secure password management with BCrypt hashing and password change workflow.

**Hashing Configuration**:
- Algorithm: BCrypt with automatic salt generation
- Strength: 10 rounds (1,024 iterations, ~100ms)
- No plain-text storage (all passwords hashed)

**Password Requirements**:
- Minimum length: 8 characters (backend enforced)
- Optional frontend validation: uppercase, lowercase, numbers

**Password Change Flow**:
1. User submits current password + new password
2. Backend verifies current password (BCrypt comparison)
3. If valid: hash new password and update database
4. If invalid: return 400 Bad Request

**Security Features**:
- Current password verification required
- BCrypt slow hashing (resistant to brute force)
- Salted hashes (prevents rainbow table attacks)
- No password reset feature (admin-managed system)

---

## 5. Rate Limiting

### 5.1 Rate Limiting Implementation

**File**: [rate-limiting.md](./rate-limiting.md)

**Description**: IP-based rate limiting with Redis for spam prevention and abuse protection.

**Strategy**: Fixed window (1-hour window, resets after expiration)

**Storage**: Redis with atomic operations (thread-safe, distributed)

**Rate Limits by Environment**:

| Environment | Max Requests/Hour | Use Case |
|-------------|-------------------|----------|
| Development | 10 | Lenient for testing |
| Staging | 5 | Production-like testing |
| Production | 3 | Strict spam prevention |

**Protected Endpoints**:
- `POST /api/contact` (public contact form)

**Redis Key Format**: `rate_limit:contact:{ip_address}`

**Error Response (429)**:
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Maximum 3 messages per hour."
}
```

**Features**:
- IP extraction with X-Forwarded-For support (proxy-aware)
- Atomic Redis increment operations (no race conditions)
- Automatic cleanup via TTL (1 hour expiration)
- Distributed tracking (works across multiple servers)

---

## 6. Security Best Practices

### 6.1 Backend Security Checklist

**Authentication & Authorization**:
- Short-lived access tokens (15 minutes)
- Long-lived refresh tokens with database storage (revocable)
- Token signature verification (HMAC-SHA256)
- Role-based access control (RBAC)
- Backend enforces all permissions (never trust frontend)
- Method-level authorization with `@PreAuthorize`

**Password Security**:
- BCrypt hashing with salt (10 rounds)
- Current password verification for changes
- No plain-text password storage
- No password reset (admin-managed system)

**Input Validation**:
- Bean Validation (`@Valid`, `@NotBlank`, `@Size`)
- MIME type validation (magic bytes, not file extensions)
- File size limits (10 MB)
- Path traversal prevention (sanitized filenames)
- SVG sanitization (whitelist-based XSS/XXE prevention) - see [svg-sanitization.md](./svg-sanitization.md)

**Rate Limiting**:
- IP-based rate limiting (contact form)
- Redis-backed atomic operations
- Environment-specific limits

**Error Handling**:
- Generic error messages (no sensitive info leak)
- Consistent error response format
- Stack traces disabled in production

---

### 6.2 Frontend Security Checklist

**Token Management**:
- localStorage for token storage (XSS mitigation via CSP)
- Automatic token refresh via HTTP interceptor
- Logout clears all stored tokens

**Route Protection**:
- authGuard for authenticated routes
- adminGuard for admin-only routes
- Redirect to login on unauthorized access

**Input Sanitization**:
- Angular built-in XSS protection (template binding)
- Automatic escaping for user-generated content
- Sanitization for innerHTML (when necessary)

---

### 6.3 Production Deployment Security Checklist

**Before Production Deployment**:

- [ ] Change JWT secret to strong, random value (256+ bits)
- [ ] Enable HTTPS with valid SSL/TLS certificates
- [ ] Configure CORS (restrict allowed origins)
- [ ] Disable stack traces in error responses
- [ ] Enable production rate limits (strict values)
- [ ] Disable Swagger UI in production
- [ ] Configure firewall rules (database, Redis ports closed)
- [ ] Enable database backups (automated, encrypted)
- [ ] Set up monitoring and alerting (failed logins, rate limit violations)
- [ ] Review log levels (WARN or ERROR only in production)
- [ ] Validate all environment variables (no defaults in production)
- [ ] Test authentication and authorization flows
- [ ] Review file upload permissions and storage paths
- [ ] Enable Redis password authentication
- [ ] Configure session timeout (frontend activity monitor)

---

### 6.4 Security Configuration Reference

**JWT Security**:
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}              # Strong random secret (256+ bits)
    expiration: 900000                 # 15 minutes
    refresh-expiration: 7              # 7 days
```

**Rate Limiting**:
```yaml
app:
  rate-limit:
    contact:
      max-requests-per-hour: 3         # Production: strict
```

**File Upload Security**:
```yaml
file:
  upload:
    max-size: 10MB                     # Prevent large uploads
    allowed-extensions:                # Whitelist only
      - jpg
      - jpeg
      - png
      - webp
      - pdf
```

**CORS Configuration**:
```yaml
cors:
  allowed-origins: https://yoursite.com  # Production domain only
  allowed-methods: GET,POST,PUT,DELETE
  allowed-headers: Authorization,Content-Type
```

---

## 7. Initial Setup

**File**: [initial-setup.md](./initial-setup.md)

**Description**: Security-critical configuration required before production deployment.

**Required Environment Variables**:

| Variable | Description | Required In |
|----------|-------------|-------------|
| `ADMIN_PASSWORD_HASH` | BCrypt hash for admin user | All environments |
| `JWT_SECRET` | JWT signing secret (256+ bits) | Prod, Staging |

**Configuration Steps**:
1. Generate BCrypt hash for admin password
2. Generate secure JWT secret (openssl rand -base64 64)
3. Set environment variables in `.env` or CI/CD secrets
4. Configure Docker Compose to inject variables

**Validation**:
- `JwtSecurityConfig`: Validates JWT secret at startup (crashes if invalid in prod/staging)
- `AdminSeeder`: Creates admin user from environment variable at startup

---

## Related Documentation

**Security Documentation**:
- [Initial Setup](./initial-setup.md) - Admin user and JWT secret configuration
- [SVG Sanitization](./svg-sanitization.md) - SVG upload security (XSS/XXE prevention)
- [JWT Implementation](./jwt-implementation.md) - JWT token generation and validation
- [RBAC](./rbac.md) - Role-based access control
- [Password Management](./password-management.md) - BCrypt hashing and password security
- [Rate Limiting](./rate-limiting.md) - IP-based rate limiting

**Related Modules**:
- [API: Authentication](../api/authentication.md) - Authentication API endpoints
- [Architecture: Frontend](../architecture/frontend-architecture.md) - Frontend security patterns
- [Architecture: Error Handling](../architecture/error-handling.md) - Error handling strategy
- [Features: Contact Form](../features/contact-form.md) - Contact form with rate limiting
- [Features: File Storage](../features/file-storage.md) - General file upload security
- [Reference: Configuration Properties](../reference/configuration-properties.md) - Security configuration
- [Reference: Error Codes](../reference/error-codes.md) - Security error codes (401, 403, 429)
- [Deployment: CI/CD](../deployment/ci-cd.md) - Secure deployment pipeline
