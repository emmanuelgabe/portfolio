# Password Management

---

## Table of Contents
1. [Overview](#1-overview)
2. [Password Requirements](#2-password-requirements)
3. [Password Hashing](#3-password-hashing)
4. [Password Change Flow](#4-password-change-flow)
5. [Implementation Details](#5-implementation-details)

---

## 1. Overview

The Portfolio application implements secure password management using industry-standard practices.

**Key Features**:
- BCrypt hashing with configurable strength
- Password validation requirements
- Secure password change workflow
- No password recovery (admin-managed system)

**Security Stack**:
- **Algorithm**: BCrypt with salt
- **Strength**: 10 rounds (configurable)
- **Backend**: Spring Security PasswordEncoder
- **Frontend**: Validation before submission

---

## 2. Password Requirements

### Backend Validation

**Minimum Requirements**:
| Requirement | Value | Enforced On |
|-------------|-------|-------------|
| Minimum Length | 8 characters | Change password only |
| Maximum Length | No limit | Not enforced |
| Complexity | None | Not enforced |
| Special Characters | Not required | Not enforced |

**Validation Annotation**:
```java
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}
```

### Frontend Validation (Recommended)

**Enhanced Requirements** (optional, frontend only):
```typescript
export interface PasswordRequirements {
  minLength: number;          // 8
  requireUppercase: boolean;  // true
  requireLowercase: boolean;  // true
  requireNumber: boolean;     // true
  requireSpecial: boolean;    // false
}

export function validatePassword(password: string, requirements: PasswordRequirements): ValidationResult {
  const errors: string[] = [];

  if (password.length < requirements.minLength) {
    errors.push(`Password must be at least ${requirements.minLength} characters`);
  }

  if (requirements.requireUppercase && !/[A-Z]/.test(password)) {
    errors.push('Password must contain at least one uppercase letter');
  }

  if (requirements.requireLowercase && !/[a-z]/.test(password)) {
    errors.push('Password must contain at least one lowercase letter');
  }

  if (requirements.requireNumber && !/[0-9]/.test(password)) {
    errors.push('Password must contain at least one number');
  }

  if (requirements.requireSpecial && !/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
    errors.push('Password must contain at least one special character');
  }

  return {
    valid: errors.length === 0,
    errors
  };
}
```

---

## 3. Password Hashing

### BCrypt Algorithm

**Why BCrypt?**
- Adaptive function (computationally expensive)
- Automatic salt generation
- Configurable work factor (strength)
- Industry-standard and proven secure
- Resistant to rainbow table attacks

### Configuration

**Backend Configuration**:
```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
```

**Strength Parameter**:
| Strength | Iterations | Hash Time (approx) | Security Level |
|----------|------------|---------------------|----------------|
| 4 | 16 | ~5 ms | Low (testing only) |
| 10 | 1024 | ~100 ms | Standard (recommended) |
| 12 | 4096 | ~400 ms | High security |
| 15 | 32768 | ~3 seconds | Very high (not practical) |

**Current Setting**: Strength 10 (good balance between security and performance)

### Hash Format

**BCrypt Hash Structure**:
```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
│  │  │ └────────────────────────────────────────────────────┘
│  │  │                       Hash (31 chars)
│  │  └─ Salt (22 chars)
│  └──── Cost factor (10 = 2^10 iterations)
└───────── Algorithm version (2a = BCrypt)
```

**Example**:
```
Algorithm: $2a
Cost:      $10
Salt:      $N9qo8uLOickgx2ZMRZoMye
Hash:      IjZAgcfl7p92ldGxad68LJZdL17lhWy
```

### Usage

**Encoding**:
```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public void createUser(String username, String rawPassword) {
        String hashedPassword = passwordEncoder.encode(rawPassword);

        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);

        userRepository.save(user);

        log.info("[CREATE_USER] User created - username={}", username);
        // Note: Never log the password (raw or hashed)
    }
}
```

**Verification**:
```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public boolean verifyPassword(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
}
```

---

## 4. Password Change Flow

### Flow Diagram

```
┌─────────┐              ┌─────────────┐              ┌──────────┐
│ Angular │              │ Spring Boot │              │ Database │
│ Client  │              │   Backend   │              │          │
└────┬────┘              └──────┬──────┘              └────┬─────┘
     │                          │                          │
     │ 1. Enter current         │                          │
     │    and new password      │                          │
     │                          │                          │
     │ 2. POST /auth/           │                          │
     │    change-password       │                          │
     │    Authorization: Bearer │                          │
     ├─────────────────────────>│                          │
     │                          │                          │
     │                          │ 3. Extract username      │
     │                          │    from JWT token        │
     │                          │                          │
     │                          │ 4. Load user             │
     │                          ├─────────────────────────>│
     │                          │<─────────────────────────┤
     │                          │   User entity            │
     │                          │                          │
     │                          │ 5. Verify current        │
     │                          │    password (BCrypt)     │
     │                          │                          │
     │                          │ 6. Hash new password     │
     │                          │    (BCrypt)              │
     │                          │                          │
     │                          │ 7. Update user password  │
     │                          ├─────────────────────────>│
     │                          │                          │
     │                          │ 8. Invalidate refresh    │
     │                          │    tokens (security)     │
     │                          ├─────────────────────────>│
     │                          │                          │
     │ 9. Success response      │                          │
     │<─────────────────────────┤                          │
     │                          │                          │
     │ 10. Show success message │                          │
     │     Redirect to login    │                          │
     │                          │                          │
```

### Steps Explained

1. **User Input**: User enters current password and new password
2. **Request**: Frontend sends request with passwords and auth token
3. **Extract User**: Backend extracts username from JWT token
4. **Load User**: User entity loaded from database
5. **Verify Current**: Current password verified with BCrypt
6. **Hash New**: New password hashed with BCrypt
7. **Update**: Password updated in database
8. **Invalidate Tokens**: All refresh tokens invalidated (force re-login)
9. **Response**: Success message returned
10. **Redirect**: User redirected to login page to re-authenticate

---

## 5. Implementation Details

**API Endpoint**: `POST /api/auth/change-password`

**Required Headers**:
```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "currentPassword": "OldPassword123",
  "newPassword": "NewSecurePassword456"
}
```

**Backend Configuration**:
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
}
```

**Key Implementation Points**:
- Current password verified with BCrypt matches
- New password hashed with BCrypt before storage
- All refresh tokens invalidated on password change
- User forced to re-login after successful change

---

## Related Documentation

- [Security: Authentication](./authentication.md) - JWT authentication architecture
- [Security: RBAC](./rbac.md) - Role-based access control
- [API: Authentication](../api/authentication.md) - Authentication endpoints
- [Development: Testing](../development/testing-guide.md) - Testing guidelines
