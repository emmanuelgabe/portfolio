# Authentication Architecture

---

## Table of Contents
1. [Overview](#1-overview)
2. [JWT Architecture](#2-jwt-architecture)
3. [Authentication Flow](#3-authentication-flow)
4. [Token Management](#4-token-management)
5. [Security Features](#5-security-features)
6. [Configuration](#6-configuration)

---

## 1. Overview

The Portfolio application uses **stateless JWT (JSON Web Tokens)** authentication with a dual-token system.

**Technology Stack**:
- **Backend**: Spring Security 6.x + JJWT library
- **Frontend**: Angular 18 with HTTP interceptors
- **Storage**: Database for refresh tokens, localStorage for client tokens

**Key Features**:
- Stateless authentication (no server-side sessions)
- Dual-token system (access + refresh)
- Automatic token refresh
- Role-based access control (RBAC)
- BCrypt password hashing

---

## 2. JWT Architecture

### Token Types

The system uses two types of JWT tokens:

#### Access Token

**Purpose**: Short-lived token for API requests

**Characteristics**:
- **Validity**: 15 minutes
- **Storage**: Client-side only (localStorage)
- **Usage**: Included in `Authorization` header for all protected requests
- **Cannot be revoked**: Stateless design, valid until expiration

**Token Structure**:
```json
{
  "type": "access",
  "sub": "admin",
  "authorities": [
    {
      "authority": "ROLE_ADMIN"
    }
  ],
  "iss": "portfolio-backend",
  "aud": "portfolio-frontend",
  "iat": 1700000000,
  "exp": 1700000900
}
```

**Payload Fields**:
| Field | Description |
|-------|-------------|
| `type` | Token type ("access") |
| `sub` | Subject (username) |
| `authorities` | User roles (for authorization) |
| `iss` | Issuer (backend identifier) |
| `aud` | Audience (frontend identifier) |
| `iat` | Issued at (timestamp) |
| `exp` | Expiration (timestamp) |

#### Refresh Token

**Purpose**: Long-lived token for obtaining new access tokens

**Characteristics**:
- **Validity**: 7 days
- **Storage**: Client-side (localStorage) + Database
- **Usage**: Used only for `/api/auth/refresh` endpoint
- **Can be revoked**: Stored in database, deleted on logout

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

**Database Storage**:
```sql
CREATE TABLE refresh_tokens (
  id BIGSERIAL PRIMARY KEY,
  token VARCHAR(500) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL REFERENCES users(id),
  expiry_date TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 3. Authentication Flow

### 3.1 Login Flow

```
┌─────────┐                ┌─────────────┐               ┌──────────┐
│ Angular │                │ Spring Boot │               │ Database │
│ Client  │                │   Backend   │               │          │
└────┬────┘                └──────┬──────┘               └────┬─────┘
     │                            │                           │
     │ 1. POST /auth/login        │                           │
     │    {username, password}    │                           │
     ├───────────────────────────>│                           │
     │                            │                           │
     │                            │ 2. Load user by username  │
     │                            ├──────────────────────────>│
     │                            │<──────────────────────────┤
     │                            │   User entity             │
     │                            │                           │
     │                            │ 3. Verify password        │
     │                            │    (BCrypt comparison)    │
     │                            │                           │
     │                            │ 4. Generate access token  │
     │                            │    (JWT, 15 min)          │
     │                            │                           │
     │                            │ 5. Generate refresh token │
     │                            │    (JWT, 7 days)          │
     │                            │                           │
     │                            │ 6. Store refresh token    │
     │                            ├──────────────────────────>│
     │                            │                           │
     │ 7. Return tokens           │                           │
     │    {accessToken, ...}      │                           │
     │<───────────────────────────┤                           │
     │                            │                           │
     │ 8. Store tokens            │                           │
     │    localStorage.setItem()  │                           │
     │                            │                           │
```

**Steps**:
1. User submits username and password
2. Backend loads user from database
3. Password verified with BCrypt
4. Access token generated (15 min expiry)
5. Refresh token generated (7 day expiry)
6. Refresh token stored in database with user association
7. Both tokens returned to client
8. Client stores tokens in localStorage

### 3.2 Token Refresh Flow

```
┌─────────┐                ┌─────────────┐               ┌──────────┐
│ Angular │                │ Spring Boot │               │ Database │
│ Client  │                │   Backend   │               │          │
└────┬────┘                └──────┬──────┘               └────┬─────┘
     │                            │                           │
     │ 1. Access token expired    │                           │
     │    (detected via 401)      │                           │
     │                            │                           │
     │ 2. POST /auth/refresh      │                           │
     │    {refreshToken}          │                           │
     ├───────────────────────────>│                           │
     │                            │                           │
     │                            │ 3. Validate refresh token │
     │                            ├──────────────────────────>│
     │                            │<──────────────────────────┤
     │                            │   Token exists & valid    │
     │                            │                           │
     │                            │ 4. Delete old token       │
     │                            ├──────────────────────────>│
     │                            │                           │
     │                            │ 5. Generate new tokens    │
     │                            │    (access + refresh)     │
     │                            │                           │
     │                            │ 6. Store new refresh token│
     │                            ├──────────────────────────>│
     │                            │                           │
     │ 7. Return new tokens       │                           │
     │<───────────────────────────┤                           │
     │                            │                           │
     │ 8. Update localStorage     │                           │
     │                            │                           │
     │ 9. Retry original request  │                           │
     │    with new access token   │                           │
     ├───────────────────────────>│                           │
     │<───────────────────────────┤                           │
     │    Success                 │                           │
```

**Key Points**:
- Old refresh token is deleted (single-use)
- New refresh token is generated and stored
- Access token is also regenerated
- Client automatically retries failed request

### 3.3 Logout Flow

```
┌─────────┐                ┌─────────────┐               ┌──────────┐
│ Angular │                │ Spring Boot │               │ Database │
│ Client  │                │   Backend   │               │          │
└────┬────┘                └──────┬──────┘               └────┬─────┘
     │                            │                           │
     │ 1. POST /auth/logout       │                           │
     │    {refreshToken}          │                           │
     │    Authorization: Bearer   │                           │
     ├───────────────────────────>│                           │
     │                            │                           │
     │                            │ 2. Delete refresh token   │
     │                            ├──────────────────────────>│
     │                            │                           │
     │ 3. Success response        │                           │
     │<───────────────────────────┤                           │
     │                            │                           │
     │ 4. Clear localStorage      │                           │
     │    removeItem(tokens)      │                           │
     │                            │                           │
     │ 5. Redirect to login       │                           │
     │                            │                           │
```

**Note**: Access token cannot be revoked (stateless), but will expire within 15 minutes.

---

## 4. Token Management

### Token Generation

**Backend Code** (JwtTokenProvider.java):
```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration; // 900000 ms (15 min)

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration; // 604800000 ms (7 days)

    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .claim("type", "access")
                .claim("authorities", userDetails.getAuthorities())
                .setSubject(userDetails.getUsername())
                .setIssuer("portfolio-backend")
                .setAudience("portfolio-frontend")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .claim("type", "refresh")
                .setSubject(username)
                .setIssuer("portfolio-backend")
                .setAudience("portfolio-frontend")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }
}
```

### Token Validation

**Backend Code**:
```java
public boolean validateToken(String token) {
    try {
        Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token);
        return true;
    } catch (SignatureException ex) {
        log.error("[JWT_VALIDATION] Invalid JWT signature");
    } catch (MalformedJwtException ex) {
        log.error("[JWT_VALIDATION] Invalid JWT token");
    } catch (ExpiredJwtException ex) {
        log.error("[JWT_VALIDATION] Expired JWT token");
    } catch (UnsupportedJwtException ex) {
        log.error("[JWT_VALIDATION] Unsupported JWT token");
    } catch (IllegalArgumentException ex) {
        log.error("[JWT_VALIDATION] JWT claims string is empty");
    }
    return false;
}
```

### Token Rotation

**Refresh Token Rotation** (security best practice):

1. When `/auth/refresh` is called:
   - Old refresh token is validated
   - Old refresh token is deleted from database
   - New refresh token is generated and stored
   - New access token is also generated

2. This prevents:
   - Refresh token reuse attacks
   - Stolen token exploitation
   - Long-term token persistence

**Implementation**:
```java
public AuthResponse refreshTokens(RefreshTokenRequest request) {
    String refreshToken = request.getRefreshToken();

    // Validate token
    if (!jwtTokenProvider.validateToken(refreshToken)) {
        throw new InvalidTokenException("Invalid refresh token");
    }

    // Get username from token
    String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

    // Delete old refresh token (single-use)
    refreshTokenRepository.deleteByToken(refreshToken);

    // Generate new tokens
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UserNotFoundException("User not found"));

    String newAccessToken = jwtTokenProvider.generateAccessToken(user);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

    // Store new refresh token
    RefreshToken newToken = new RefreshToken(newRefreshToken, user);
    refreshTokenRepository.save(newToken);

    return new AuthResponse(newAccessToken, newRefreshToken);
}
```

### User Details Loading

**CustomUserDetailsService** handles user loading for Spring Security authentication.

**Location**: `security/CustomUserDetailsService.java`

**Implementation**:
```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList()))
            .build();
    }
}
```

**Purpose**:
- Loads user from database by username
- Converts application `User` entity to Spring Security `UserDetails`
- Maps user roles to Spring Security authorities
- Used by `DaoAuthenticationProvider` during authentication

---

## 5. Security Features

### Password Security

**Hashing Algorithm**: BCrypt with strength 10

**Example**:
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
}

// Usage
String hashedPassword = passwordEncoder.encode(rawPassword);
boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);
```

**Benefits**:
- Salt automatically generated
- Computationally expensive (prevents brute-force)
- Industry-standard algorithm

### Token Security

**JWT Secret**:
- Minimum 256 bits (32 characters)
- Stored in environment variables (not in code)
- Different secrets for dev/staging/prod

**Token Expiration**:
- Access: 15 minutes (limits exposure)
- Refresh: 7 days (balance between security and UX)

**Token Validation**:
- Signature verification
- Expiration check
- Issuer/audience validation
- Type claim verification

### CORS Configuration

**Allowed Origins**:
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:4200",  // Angular dev
        "http://localhost:3000",  // Staging
        "https://yoursite.com"    // Production
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
}
```

---

## 6. Configuration

### Application Properties

**File**: `application.yml`

```yaml
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-change-in-production}
  access-token-expiration: 900000      # 15 minutes in milliseconds
  refresh-token-expiration: 604800000  # 7 days in milliseconds
  issuer: portfolio-backend
  audience: portfolio-frontend
```

### Environment Variables

**Required**:
```bash
# Production
JWT_SECRET=your-strong-secret-key-min-256-bits

# Development (optional, uses default)
# JWT_SECRET not set, uses application.yml default
```

---

## Related Documentation

- [API: Authentication](../api/authentication.md) - Authentication endpoints
- [Security: RBAC](./rbac.md) - Role-based access control
- [Security: Password Management](./password-management.md) - Password policies
- [Development: Testing](../development/testing-guide.md) - Testing authentication
