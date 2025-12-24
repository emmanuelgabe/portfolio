# Initial Security Setup

Core security configuration for the Portfolio application.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Admin User Setup](#2-admin-user-setup)
3. [JWT Secret Setup](#3-jwt-secret-setup)
4. [Database Password Setup](#4-database-password-setup)
5. [Mail Configuration](#5-mail-configuration)
6. [Environment Variables Summary](#6-environment-variables-summary)
7. [Docker Compose Configuration](#7-docker-compose-configuration)

---

## 1. Overview

Security-critical configuration for the Portfolio application. Requirements vary by environment.

### 1.1 Requirements by Environment

| Variable | Dev (local) | Staging | Production |
|----------|-------------|---------|------------|
| `DB_USER_PASSWORD` | Required | Required | Required |
| `MAIL_USERNAME` | Required | Required | Required |
| `MAIL_APP_PASSWORD` | Required | Required | Required |
| `JWT_SECRET` | Optional (uses default) | Required | Required |
| `ADMIN_PASSWORD_HASH` | Optional | Required | Required |

For infrastructure credentials (Redis, RabbitMQ, Elasticsearch, Sentry, Grafana), see [Infrastructure Credentials](./infrastructure-credentials.md).

### 1.2 Security Principles

- No hardcoded credentials in codebase
- Environment-based configuration (different values per environment)
- Application startup validation (fails fast if misconfigured in prod)
- BCrypt for password hashing (one-way, salted)

---

## 2. Admin User Setup

### 2.1 AdminSeeder Overview

Admin user creation is handled by `AdminSeeder` at application startup.

**Behavior**:
- Checks if admin user exists (by username)
- If not exists and `ADMIN_PASSWORD_HASH` is set: creates admin user
- If not exists and hash is missing: logs warning (dev) or error (prod/staging)
- Never overwrites existing admin user

**Default Admin Credentials**:
- Username: `admin` (fixed)
- Email: `admin@portfolio.local` (fixed, not used for login)
- Role: `ROLE_ADMIN`

### 2.2 Generate BCrypt Hash

**Using the provided script** (recommended):

```bash
./scripts/security/generate-bcrypt-hash.sh
```

**Using Python**:

```bash
pip install bcrypt
python -c "import bcrypt; print(bcrypt.hashpw(b'YourPassword', bcrypt.gensalt(10)).decode())"
```

**Using htpasswd**:

```bash
htpasswd -nbBC 10 "" "YourPassword" | tr -d ':\n' | sed 's/$2y/$2a/'
```

**Hash Format**: Must start with `$2a$`, `$2b$`, or `$2y$`.

### 2.3 Set Environment Variable

**CI/CD** (GitHub Actions):
- Add `ADMIN_PASSWORD_HASH` as a repository secret

**Important**: Use single quotes to prevent shell expansion of `$` characters.

---

## 3. JWT Secret Setup

### 3.1 JwtSecurityConfig Overview

JWT secret validation is handled by `JwtSecurityConfig` at application startup.

**Validation Rules** (prod/staging only):
- Secret must not contain "dev" or "default" (case-insensitive)
- Secret must be at least 43 characters (256 bits base64)
- Validation failure causes application to crash

### 3.2 Generate JWT Secret

```bash
openssl rand -base64 64
```

Generates a 512-bit random string (88 characters).

### 3.3 Set Environment Variable

**CI/CD** (GitHub Actions):
- Add `JWT_SECRET` as a repository secret

---

## 4. Database Password Setup

### 4.1 Overview

PostgreSQL database password is required for both Spring Boot and PostgreSQL container.

**Usage**:
- Spring Boot: `SPRING_DATASOURCE_PASSWORD`
- PostgreSQL: `POSTGRES_PASSWORD`
- Both configured via `DB_USER_PASSWORD`

### 4.2 Generate Database Password

```bash
openssl rand -base64 32
```

**Requirements**:
- Minimum 16 characters
- Avoid shell-escaping characters (`$`, `!`, `` ` ``)

### 4.3 Set Environment Variable

**CI/CD** (GitHub Actions):
- Add `DB_USER_PASSWORD` as a repository secret

---

## 5. Mail Configuration

### 5.1 Overview

Mail configuration is required for the contact form feature.

**Required Variables**:
- `MAIL_USERNAME`: Gmail address
- `MAIL_APP_PASSWORD`: Gmail App Password

### 5.2 Generate Gmail App Password

1. Go to https://myaccount.google.com/
2. Navigate to Security > 2-Step Verification
3. Click "App passwords"
4. Select "Mail" and "Other (Custom name)"
5. Copy the 16-character password

**Important**: Remove spaces when setting the environment variable.

### 5.3 Set Environment Variables

**CI/CD** (GitHub Actions):
- Add `MAIL_USERNAME` as a repository secret
- Add `MAIL_APP_PASSWORD` as a repository secret

---

## 6. Environment Variables Summary

### 6.1 Core Security Variables

| Variable | Description | Required In |
|----------|-------------|-------------|
| `DB_USER_PASSWORD` | PostgreSQL password | All |
| `MAIL_USERNAME` | Gmail address | All |
| `MAIL_APP_PASSWORD` | Gmail App Password | All |
| `JWT_SECRET` | JWT signing secret (256+ bits) | Staging/Prod |
| `ADMIN_PASSWORD_HASH` | BCrypt hash for admin | Staging/Prod |

For infrastructure variables (Redis, RabbitMQ, etc.), see [Infrastructure Credentials](./infrastructure-credentials.md).

### 6.2 Validation Checklist

**For Development (local):**
- [ ] `DB_USER_PASSWORD` is set
- [ ] `MAIL_USERNAME` and `MAIL_APP_PASSWORD` are set
- [ ] (Optional) `ADMIN_PASSWORD_HASH` for admin access

**For Staging/Production:**
- [ ] `DB_USER_PASSWORD` is set with secure password
- [ ] `MAIL_USERNAME` and `MAIL_APP_PASSWORD` are set
- [ ] `JWT_SECRET` is set (43+ chars, no "dev"/"default")
- [ ] `ADMIN_PASSWORD_HASH` is set with valid BCrypt hash
- [ ] CI/CD secrets are configured in GitHub Actions

---

## 7. Docker Compose Configuration

### 7.1 Environment Variable Injection

**docker-compose.yml** (base):
```yaml
services:
  spring-backend:
    environment:
      - SPRING_DATASOURCE_PASSWORD=${DB_USER_PASSWORD}
      - JWT_SECRET=${JWT_SECRET:-}
      - ADMIN_PASSWORD_HASH=${ADMIN_PASSWORD_HASH:-}
      - MAIL_USERNAME=${MAIL_USERNAME:-}
      - MAIL_APP_PASSWORD=${MAIL_APP_PASSWORD:-}

  portfolio-db:
    environment:
      POSTGRES_PASSWORD: ${DB_USER_PASSWORD}
```

### 7.2 .env File Template

**For Development (local):**
```bash
DB_USER_PASSWORD=local_dev_password
MAIL_USERNAME=your_email@gmail.com
MAIL_APP_PASSWORD=your_gmail_app_password
# ADMIN_PASSWORD_HASH=$2a$10$your_hash_here
```

**For Staging/Production:**
```bash
DB_USER_PASSWORD=secure_production_password
MAIL_USERNAME=your_email@gmail.com
MAIL_APP_PASSWORD=your_gmail_app_password
JWT_SECRET=your_64_char_random_secret
ADMIN_PASSWORD_HASH=$2a$10$your_bcrypt_hash_here
```

### 7.3 Startup Verification

**Success**:
```
[SECURITY] JWT secret validation passed for prod environment
[ADMIN_SEEDER] Admin user created - username=admin
```

**Failure**:
```
SECURITY ERROR: JWT_SECRET contains default value...
```

---

## Related Documentation

- [Infrastructure Credentials](./infrastructure-credentials.md) - Redis, RabbitMQ, Elasticsearch, Sentry, Grafana
- [JWT Implementation](./jwt-implementation.md) - Token generation and validation
- [Password Management](./password-management.md) - BCrypt hashing
- [Configuration Properties](../reference/configuration-properties.md) - All configuration options
