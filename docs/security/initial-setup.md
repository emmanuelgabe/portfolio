# Initial Security Setup

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
| `ADMIN_PASSWORD_HASH` | Optional (admin not created) | Required | Required |

### 1.2 Quick Start by Environment

**Development (local):**
```bash
# Minimum required in .env:
DB_USER_PASSWORD=your_local_db_password
MAIL_USERNAME=your_email@gmail.com
MAIL_APP_PASSWORD=your_gmail_app_password

# Optional - add only if you need admin access:
ADMIN_PASSWORD_HASH=$2a$10$your_bcrypt_hash_here
```

**Staging / Production:**
```bash
# ALL variables required in .env:
DB_USER_PASSWORD=secure_db_password
MAIL_USERNAME=your_email@gmail.com
MAIL_APP_PASSWORD=your_gmail_app_password
JWT_SECRET=your_64_char_random_secret
ADMIN_PASSWORD_HASH=$2a$10$your_bcrypt_hash_here
```

### 1.3 What Happens If Missing?

| Variable | Dev | Staging/Prod |
|----------|-----|--------------|
| `JWT_SECRET` | Uses default (warning logged) | App crashes at startup |
| `ADMIN_PASSWORD_HASH` | Admin not created (warning logged) | Admin not created (error logged) |
| `DB_USER_PASSWORD` | Database connection fails | Database connection fails |
| `MAIL_USERNAME/PASSWORD` | Contact form emails fail | Contact form emails fail |

### 1.4 Security Principles

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
# Unix/Linux/macOS
./scripts/security/generate-bcrypt-hash.sh

# Enter your password when prompted
```

**Using Python** (if bcrypt is installed):

```bash
pip install bcrypt
python -c "import bcrypt; print(bcrypt.hashpw(b'YourSecurePassword123!', bcrypt.gensalt(10)).decode())"
```

**Using htpasswd** (Apache utils):

```bash
htpasswd -nbBC 10 "" "YourSecurePassword123!" | tr -d ':\n' | sed 's/$2y/$2a/'
```

**Using online tools** (for testing only):
- https://bcrypt-generator.com/
- Use 10 rounds

**Hash Format**: The hash must start with `$2a$`, `$2b$`, or `$2y$` (BCrypt variants).

Example: `$2a$10$K7xKQYzP1q2wR3sT4uV5xO6yZ7aB8cD9eF0gH1iJ2kL3mN4oP5qR6s`

### 2.3 Set Environment Variable

**Local development** (`.env` file):

```bash
ADMIN_PASSWORD_HASH=$2a$10$K7xKQYzP1q2wR3sT4uV5xO6yZ7aB8cD9eF0gH1iJ2kL3mN4oP5qR6s
```

**CI/CD** (GitHub Actions):
- Add `ADMIN_PASSWORD_HASH` as a repository secret
- Reference in workflow: `${{ secrets.ADMIN_PASSWORD_HASH }}`

**Server** (production/staging):

```bash
export ADMIN_PASSWORD_HASH='$2a$10$K7xKQYzP1q2wR3sT4uV5xO6yZ7aB8cD9eF0gH1iJ2kL3mN4oP5qR6s'
```

**Important**: Use single quotes to prevent shell expansion of `$` characters.

---

## 3. JWT Secret Setup

### 3.1 JwtSecurityConfig Overview

JWT secret validation is handled by `JwtSecurityConfig` at application startup.

**Validation Rules** (prod/staging only):
- Secret must not contain "dev" or "default" (case-insensitive)
- Secret must be at least 43 characters (256 bits base64)
- Validation failure causes application to crash with clear error message

**Behavior by Environment**:
| Environment | Default Secret Used | Result |
|-------------|---------------------|--------|
| `dev` | Yes | Warning logged, app starts |
| `dev` | No | Info logged, app starts |
| `staging` | Yes | Error, app crashes |
| `staging` | No | Info logged, app starts |
| `prod` | Yes | Error, app crashes |
| `prod` | No | Info logged, app starts |

### 3.2 Generate JWT Secret

**Using openssl** (recommended):

```bash
openssl rand -base64 64
```

This generates a 512-bit random string encoded in base64 (88 characters).

**Using /dev/urandom**:

```bash
head -c 64 /dev/urandom | base64
```

**Example output**:
```
K7xKQYzP1q2wR3sT4uV5xO6yZ7aB8cD9eF0gH1iJ2kL3mN4oP5qR6sT7uV8wX9yZ0aB1cD2eF3gH4iJ5kL6mN7oP8q==
```

### 3.3 Set Environment Variable

**Local development** (`.env` file):

```bash
JWT_SECRET=K7xKQYzP1q2wR3sT4uV5xO6yZ7aB8cD9eF0gH1iJ2kL3mN4oP5qR6sT7uV8wX9yZ0aB1cD2eF3gH4iJ5kL6mN7oP8q==
```

**CI/CD** (GitHub Actions):
- Add `JWT_SECRET` as a repository secret
- Reference in workflow: `${{ secrets.JWT_SECRET }}`

**Server** (production/staging):

```bash
export JWT_SECRET='K7xKQYzP1q2wR3sT4uV5xO6yZ7aB8cD9eF0gH1iJ2kL3mN4oP5qR6sT7uV8wX9yZ0aB1cD2eF3gH4iJ5kL6mN7oP8q=='
```

### 3.4 Configuration Files

**application.yml** (global with fallback for dev):
```yaml
app:
  jwt:
    secret: ${JWT_SECRET:defaultSecretKeyForDevOnlyChangeInProductionMustBeAtLeast256BitsLongForHS256Algorithm}
```

**application-prod.yml** (no fallback):
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
```

**application-staging.yml** (no fallback):
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
```

---

## 4. Database Password Setup

### 4.1 Overview

PostgreSQL database password is required for both the Spring Boot application and the PostgreSQL container.

**Usage**:
- Spring Boot connects to PostgreSQL using `SPRING_DATASOURCE_PASSWORD`
- PostgreSQL container uses `POSTGRES_PASSWORD` to set the database user password
- Both are configured via `DB_USER_PASSWORD` environment variable

### 4.2 Generate Database Password

**Using openssl**:

```bash
openssl rand -base64 32
```

**Using /dev/urandom**:

```bash
head -c 24 /dev/urandom | base64
```

**Requirements**:
- Minimum 16 characters recommended
- Avoid special characters that may cause shell escaping issues (`$`, `!`, `` ` ``)
- Use alphanumeric characters and `-`, `_`, `.`

### 4.3 Set Environment Variable

**Local development** (`.env` file):

```bash
DB_USER_PASSWORD=your_secure_database_password_here
```

**CI/CD** (GitHub Actions):
- Add `DB_USER_PASSWORD` as a repository secret
- Reference in workflow: `${{ secrets.DB_USER_PASSWORD }}`

**Server** (production/staging):

```bash
export DB_USER_PASSWORD='your_secure_database_password_here'
```

### 4.4 Docker Compose Configuration

The password is injected into both the backend and database containers:

```yaml
services:
  spring-backend:
    environment:
      - SPRING_DATASOURCE_PASSWORD=${DB_USER_PASSWORD}

  portfolio-db:
    environment:
      POSTGRES_PASSWORD: ${DB_USER_PASSWORD}
```

**Important**: Both services must use the same password value.

---

## 5. Mail Configuration

### 5.1 Overview

Mail configuration is required for the contact form feature. When visitors submit the contact form, the application sends an email notification via Gmail SMTP.

**Usage**:
- `MAIL_USERNAME`: Gmail address that sends notification emails
- `MAIL_APP_PASSWORD`: Gmail App Password (not your regular Gmail password)

### 5.2 Generate Gmail App Password

Gmail requires an "App Password" for SMTP authentication (regular passwords are blocked).

**Steps**:
1. Go to Google Account settings: https://myaccount.google.com/
2. Navigate to Security > 2-Step Verification (must be enabled)
3. At the bottom, click "App passwords"
4. Select app: "Mail", Select device: "Other (Custom name)"
5. Enter name: "Portfolio Contact Form"
6. Click "Generate"
7. Copy the 16-character password (format: `xxxx xxxx xxxx xxxx`)

**Important**:
- Remove spaces when setting the environment variable
- App passwords can only be viewed once - save it securely
- If compromised, revoke it in Google Account settings

### 5.3 Set Environment Variables

**Local development** (`.env` file):

```bash
MAIL_USERNAME=your_email@gmail.com
MAIL_APP_PASSWORD=yourapppassword
```

**CI/CD** (GitHub Actions):
- Add `MAIL_USERNAME` and `MAIL_APP_PASSWORD` as repository secrets
- Reference in workflow: `${{ secrets.MAIL_APP_PASSWORD }}`

**Server** (production/staging):

```bash
export MAIL_USERNAME='your_email@gmail.com'
export MAIL_APP_PASSWORD='yourapppassword'
```

### 5.4 Configuration in application.yml

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_APP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

---

## 6. Environment Variables Summary

### 6.1 Required Variables

| Variable | Description | Dev | Staging/Prod | Default |
|----------|-------------|-----|--------------|---------|
| `DB_USER_PASSWORD` | PostgreSQL database password | Required | Required | None |
| `MAIL_USERNAME` | Gmail address for notifications | Required | Required | None |
| `MAIL_APP_PASSWORD` | Gmail App Password | Required | Required | None |
| `JWT_SECRET` | JWT signing secret (256+ bits) | Optional | Required | Dev default |
| `ADMIN_PASSWORD_HASH` | BCrypt hash for admin password | Optional | Required | None |

### 6.2 All Security Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `ADMIN_PASSWORD_HASH` | Admin BCrypt hash | `$2a$10$...` |
| `JWT_SECRET` | JWT secret (base64) | `K7xKQYzP1q2w...` |
| `DB_USER_PASSWORD` | PostgreSQL password | `SecureDbPass123` |
| `MAIL_USERNAME` | Gmail address | `contact@gmail.com` |
| `MAIL_APP_PASSWORD` | Gmail App Password | `abcdabcdabcdabcd` |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token TTL (ms) | `900000` (15 min) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token TTL (ms) | `604800000` (7 days) |
| `JWT_ISSUER` | Token issuer identifier | `portfolio-backend` |
| `JWT_AUDIENCE` | Token audience identifier | `portfolio-frontend` |

### 6.3 Validation Checklist

**For Development (local):**
- [ ] `DB_USER_PASSWORD` is set
- [ ] `MAIL_USERNAME` and `MAIL_APP_PASSWORD` are set (for contact form testing)
- [ ] (Optional) `ADMIN_PASSWORD_HASH` is set if you need admin access

**For Staging/Production:**
- [ ] `DB_USER_PASSWORD` is set with secure password
- [ ] `MAIL_USERNAME` is set with valid Gmail address
- [ ] `MAIL_APP_PASSWORD` is set with Gmail App Password
- [ ] `JWT_SECRET` is set with secure random value (43+ chars)
- [ ] `JWT_SECRET` does not contain "dev" or "default"
- [ ] `ADMIN_PASSWORD_HASH` is set with valid BCrypt hash
- [ ] Variables are stored securely (not in version control)
- [ ] CI/CD secrets are configured

---

## 7. Docker Compose Configuration

### 7.1 Environment Variable Injection

Docker Compose files reference variables from `.env` file or host environment.

**docker-compose.yml** (base):
```yaml
services:
  spring-backend:
    environment:
      - SPRING_DATASOURCE_PASSWORD=${DB_USER_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - ADMIN_PASSWORD_HASH=${ADMIN_PASSWORD_HASH}
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_APP_PASSWORD=${MAIL_APP_PASSWORD}

  portfolio-db:
    environment:
      POSTGRES_PASSWORD: ${DB_USER_PASSWORD}
```

**docker-compose.prod.yml**:
```yaml
services:
  spring-backend:
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_PASSWORD=${DB_USER_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - ADMIN_PASSWORD_HASH=${ADMIN_PASSWORD_HASH}
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_APP_PASSWORD=${MAIL_APP_PASSWORD}

  portfolio-db:
    environment:
      POSTGRES_PASSWORD: ${DB_USER_PASSWORD}
```

**docker-compose.staging.yml**:
```yaml
services:
  spring-backend:
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - SPRING_DATASOURCE_PASSWORD=${DB_USER_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - ADMIN_PASSWORD_HASH=${ADMIN_PASSWORD_HASH}
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_APP_PASSWORD=${MAIL_APP_PASSWORD}

  portfolio-db:
    environment:
      POSTGRES_PASSWORD: ${DB_USER_PASSWORD}
```

### 7.2 .env File Template

Create `.env` file in project root (never commit to git).

**For Development (local):**
```bash
# Database (required)
DB_USER_PASSWORD=local_dev_password

# Mail (required for contact form)
MAIL_USERNAME=your_email@gmail.com
MAIL_APP_PASSWORD=your_gmail_app_password

# Optional: Add only if you need admin access in dev
# Generated with: ./scripts/security/generate-bcrypt-hash.sh
# ADMIN_PASSWORD_HASH=$2a$10$your_hash_here
```

**For Staging/Production:**
```bash
# Database (required)
DB_USER_PASSWORD=secure_production_password

# Mail (required)
MAIL_USERNAME=your_email@gmail.com
MAIL_APP_PASSWORD=your_gmail_app_password

# JWT Secret (required) - Generated with: openssl rand -base64 64
JWT_SECRET=K7xKQYzP1q2wR3sT4uV5xO6yZ7aB8cD9eF0gH1iJ2kL3mN4oP5qR6s...

# Admin password hash (required) - Generated with: ./scripts/security/generate-bcrypt-hash.sh
ADMIN_PASSWORD_HASH=$2a$10$your_bcrypt_hash_here
```

### 7.3 Startup Verification

After starting the application, verify in logs:

**Success** (staging/prod):
```
[SECURITY] JWT secret validation passed for prod environment
[ADMIN_SEEDER] Admin user created - username=admin
```

**Failure** (missing JWT_SECRET in prod):
```
[SECURITY] JWT_SECRET contains default/dev value in prod environment
SECURITY ERROR: JWT_SECRET contains default value...
```

**Failure** (missing ADMIN_PASSWORD_HASH):
```
[ADMIN_SEEDER] ADMIN_PASSWORD_HASH not configured in prod environment. Admin user will NOT be created.
```

---

## Related Documentation

- [JWT Implementation](./jwt-implementation.md) - JWT token generation and validation
- [Password Management](./password-management.md) - BCrypt hashing and password security
- [Configuration Properties](../reference/configuration-properties.md) - All configuration options
- [Environments](../reference/environments.md) - Environment-specific configuration
