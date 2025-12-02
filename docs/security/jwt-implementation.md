# JWT Implementation

---

## Table of Contents
1. [Overview](#1-overview)
2. [Token Types](#2-token-types)
3. [Token Generation](#3-token-generation)
4. [Token Validation](#4-token-validation)
5. [Token Refresh Flow](#5-token-refresh-flow)
6. [Configuration](#6-configuration)

---

## 1. Overview

JWT (JSON Web Tokens) authentication system with access/refresh token pair for secure API access.

**Key Features**:
- Dual token system (access + refresh)
- Automatic token refresh via HTTP interceptor
- Token rotation for enhanced security
- Refresh token revocation support
- Redis-backed token storage capability

**Library**: `io.jsonwebtoken:jjwt-api:0.12.5`

---

## 2. Token Types

### Access Token

**Purpose**: Short-lived token for API authentication

**Lifetime**: 15 minutes (configurable via `app.jwt.expiration`)

**Storage**: Frontend `localStorage`

**Usage**: Attached to every API request via `Authorization: Bearer {token}` header

**Claims**:
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

### Refresh Token

**Purpose**: Long-lived token for obtaining new access tokens

**Lifetime**: 7 days (configurable via `app.jwt.refresh-expiration`)

**Storage**:
- Frontend `localStorage`
- Backend database (`refresh_tokens` table)

**Usage**: Used only at `/api/auth/refresh` endpoint

**Format**: UUID (not JWT)

---

## 3. Token Generation

### JwtTokenProvider

**Service**: `com.emmanuelgabe.portfolio.security.JwtTokenProvider`

**Access Token Generation**:
```java
public String generateAccessToken(String username, Set<Role> roles) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpiration);

    return Jwts.builder()
        .claim("type", "access")
        .subject(username)
        .claim("authorities", roles.stream()
            .map(role -> Map.of("authority", role.getName()))
            .collect(Collectors.toList()))
        .issuer(jwtIssuer)
        .audience().add(jwtAudience).and()
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(key)
        .compact();
}
```

**Refresh Token Generation**:
```java
public RefreshToken createRefreshToken(Long userId) {
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUser(userRepository.findById(userId).orElseThrow());
    refreshToken.setToken(UUID.randomUUID().toString());
    refreshToken.setExpiryDate(
        LocalDateTime.now().plusDays(refreshTokenDurationDays)
    );
    return refreshTokenRepository.save(refreshToken);
}
```

---

## 4. Token Validation

### JwtAuthenticationFilter

**Purpose**: Extract and validate JWT from requests

**Filter Chain Position**: Before `UsernamePasswordAuthenticationFilter`

**Validation Steps**:
1. Extract token from `Authorization` header
2. Parse JWT and extract username
3. Validate signature
4. Check expiration
5. Load user details from database
6. Set authentication in `SecurityContext`

**Implementation**:
```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) {
    String token = extractToken(request);

    if (token != null && jwtTokenProvider.validateToken(token)) {
        String username = jwtTokenProvider.getUsernameFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
            );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
}
```

---

## 5. Token Refresh Flow

### Backend Flow

**Endpoint**: `POST /api/auth/refresh`

**Request**:
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Process**:
1. Validate refresh token exists in database
2. Check if token is revoked
3. Check if token is expired
4. Generate new access token
5. Return new access token

**Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

### Frontend Automatic Refresh

**JWT Interceptor** (`jwt.interceptor.ts`):

**Trigger**: 401 Unauthorized response

**Flow**:
```
1. Request fails with 401
2. Interceptor catches error
3. Call AuthService.refreshToken()
4. If refresh succeeds:
   - Get new access token
   - Retry original request with new token
5. If refresh fails:
   - Logout user
   - Redirect to login
```

**Implementation**:
```typescript
return next(authReq).pipe(
  catchError((error: HttpErrorResponse) => {
    if (error.status === 401 && !isAuthEndpoint) {
      return authService.refreshToken().pipe(
        switchMap(() => {
          const newToken = authService.getToken();
          const retryReq = req.clone({
            setHeaders: { Authorization: `Bearer ${newToken}` }
          });
          return next(retryReq);
        }),
        catchError((refreshError) => {
          authService.logout();
          return throwError(() => refreshError);
        })
      );
    }
    return throwError(() => error);
  })
);
```

---

## 6. Configuration

### Application Properties

**JWT Configuration**:
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}  # HS256 secret key (min 256 bits)
    expiration: 900000     # Access token: 15 minutes (in ms)
    refresh-expiration: 7  # Refresh token: 7 days
    issuer: portfolio-backend
    audience: portfolio-frontend
```

### Startup Validation (JwtSecurityConfig)

**Class**: `com.emmanuelgabe.portfolio.config.JwtSecurityConfig`

**Purpose**: Validates JWT secret at application startup to prevent insecure deployments.

**Validation Rules** (prod/staging environments):
- Secret must not contain "dev" or "default" (case-insensitive)
- Secret must be at least 43 characters (256 bits in base64)
- Validation failure causes application startup to fail with clear error message

**Behavior by Environment**:
| Environment | Invalid Secret | Result |
|-------------|----------------|--------|
| `dev` | Yes | Warning logged, app starts |
| `staging` | Yes | Error, app crashes |
| `prod` | Yes | Error, app crashes |

**Generate Secure Secret**:
```bash
openssl rand -base64 64
```

See [Initial Setup](./initial-setup.md) for complete configuration instructions.

### Security Configuration

**Public Endpoints** (no authentication):
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/projects`
- `GET /api/articles`
- `POST /api/contact`

**Protected Endpoints** (requires valid JWT):
- `POST /api/admin/**` (requires ROLE_ADMIN)

---

## Related Documentation

- [Initial Setup](./initial-setup.md) - JWT secret and admin user configuration
- [Authentication API](../api/authentication.md) - Auth endpoints
- [Security: RBAC](./rbac.md) - Role-based access control
- [Architecture: Frontend](../architecture/frontend-architecture.md) - Frontend auth flow
