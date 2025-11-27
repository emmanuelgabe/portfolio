# CV Management System

---

## Table of Contents
1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Versioning System](#3-versioning-system)
4. [Database Schema](#4-database-schema)
5. [Configuration](#5-configuration)

---

## 1. Overview

The CV Management System provides a complete solution for uploading, versioning, and managing CV/resume files with a single "current" CV designation.

**Key Features**:
- Multiple CV versions support
- Single "current" CV at any time
- User tracking (who uploaded each CV)
- Public access to current CV
- Admin-only upload and management
- PDF validation and security

**Use Case**: Portfolio website owner can upload multiple CV versions, set one as current for public download, and maintain historical versions.

---

## 2. Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend (Angular)                    │
│                                                               │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│  │  Public Pages  │  │  Admin Pages   │  │  CV Service    │ │
│  │                │  │                │  │                │ │
│  │ - Download CV  │  │ - Upload CV    │  │ - HTTP Client  │ │
│  │ - View current │  │ - List all CVs │  │ - File upload  │ │
│  └────────┬───────┘  │ - Set current  │  └────────┬───────┘ │
│           │          │ - Delete CV    │           │         │
│           │          └────────┬───────┘           │         │
│           └─────────────────┬─┴───────────────────┘         │
└───────────────────────────┬─┴─────────────────────────────┘
                            │ HTTP/REST
┌───────────────────────────┴─┬─────────────────────────────┐
│                        Backend (Spring Boot)                │
│                                                               │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│  │  CvController  │  │   CvService    │  │ FileStorage    │ │
│  │                │  │                │  │    Service     │ │
│  │ - Public API   │─>│ - Business     │─>│                │ │
│  │ - Admin API    │  │   logic        │  │ - Upload       │ │
│  └────────────────┘  │ - Validation   │  │ - Delete       │ │
│                      │ - Versioning   │  │ - Validation   │ │
│                      └────────┬───────┘  └────────────────┘ │
│                               │                              │
│                      ┌────────┴───────┐                      │
│                      │ CvRepository   │                      │
│                      │                │                      │
│                      │ - JPA CRUD     │                      │
│                      │ - Custom query │                      │
│                      └────────┬───────┘                      │
└─────────────────────────────┬─┴──────────────────────────────┘
                              │ JDBC
┌─────────────────────────────┴─┬──────────────────────────────┐
│                        PostgreSQL Database                    │
│                                                               │
│  ┌────────────────┐  ┌────────────────┐                      │
│  │   cvs table    │  │  users table   │                      │
│  │                │  │                │                      │
│  │ - id           │  │ - id           │                      │
│  │ - filename     │  │ - username     │                      │
│  │ - file_path    │  │ - password     │                      │
│  │ - is_current   │  │ - roles        │                      │
│  │ - user_id (FK) │<─│                │                      │
│  └────────────────┘  └────────────────┘                      │
└───────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────┐
│                        File System                            │
│                                                               │
│  uploads/cvs/                                                 │
│  ├── cv_1_1700000000.pdf                                      │
│  ├── cv_2_1700100000.pdf                                      │
│  └── cv_3_1700200000.pdf (current)                            │
└───────────────────────────────────────────────────────────────┘
```

### Request Flow

**Upload CV Flow**:
```
User → AdminCvComponent → CvService (Angular)
→ POST /api/admin/cv/upload
→ CvController → CvService (Spring)
→ FileStorageService → File System
→ CvRepository → Database
→ Response with CV metadata
```

**Get Current CV Flow**:
```
User → HomePage → CvService (Angular)
→ GET /api/cv/current
→ CvController → CvService (Spring)
→ CvRepository → Database (WHERE is_current = true)
→ Response with CV metadata
```

**Download CV Flow**:
```
User → Click download → CvService (Angular)
→ GET /api/cv/download/{id}
→ CvController → CvService (Spring)
→ File System → Read PDF
→ Stream PDF to client
```

---

## 3. Versioning System

### How Versioning Works

The system supports multiple CV versions with a "current" flag to designate which CV is publicly visible.

**Key Rules**:
1. **Multiple Versions**: Unlimited CV uploads allowed
2. **Single Current**: Only ONE CV can be marked as `is_current = true` at any time
3. **Manual Activation**: New uploads are NOT automatically current
4. **Database Constraint**: Unique index enforces single current CV

### Version States

| State | Description | Public Access | Admin Actions |
|-------|-------------|---------------|---------------|
| **Current** | Active CV shown on public pages | Yes | Download, Delete |
| **Historical** | Previous CV versions | No | Set as current, Download, Delete |

### Setting Current CV

**Process**:
```sql
-- Step 1: Unset all current CVs
UPDATE cvs SET is_current = false WHERE is_current = true;

-- Step 2: Set new current CV
UPDATE cvs SET is_current = true WHERE id = ?;
```

**Backend Implementation**:
```java
@Transactional
public CvResponse setCurrentCv(Long cvId) {
    // Unset all current CVs
    cvRepository.unsetAllCurrent();

    // Set new current
    Cv cv = cvRepository.findById(cvId)
        .orElseThrow(() -> new ResourceNotFoundException("CV not found"));

    cv.setIsCurrent(true);
    Cv updated = cvRepository.save(cv);

    return cvMapper.toResponse(updated);
}
```

### Database Constraint

**Unique Index** ensures only one current CV:
```sql
CREATE UNIQUE INDEX unique_current_cv ON cvs (is_current) WHERE is_current = true;
```

**Why This Works**:
- Partial unique index (only rows where `is_current = true`)
- Multiple `false` values allowed (no constraint)
- Exactly one `true` value allowed (enforced by database)
- Prevents race conditions

---

## 4. Database Schema

### CVs Table

**Migration**: `V5__create_cvs_table.sql`

```sql
CREATE TABLE cvs (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_current BOOLEAN NOT NULL DEFAULT false,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_cvs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Ensure only one CV can be current at a time
CREATE UNIQUE INDEX unique_current_cv ON cvs (is_current) WHERE is_current = true;

-- Index for fast lookup of current CV
CREATE INDEX idx_cvs_is_current ON cvs (is_current);
```

**Columns**:
| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `id` | BIGSERIAL | No | Primary key |
| `filename` | VARCHAR(255) | No | Original filename |
| `file_path` | VARCHAR(500) | No | Server file path |
| `uploaded_at` | TIMESTAMP | No | Upload timestamp |
| `is_current` | BOOLEAN | No | Current CV flag (default: false) |
| `user_id` | BIGINT | No | Foreign key to users table |

**Indexes**:
- Primary key on `id`
- Unique partial index on `is_current` (WHERE `is_current = true`)
- Regular index on `is_current` for fast lookups

**Constraints**:
- Foreign key to `users(id)` with CASCADE delete
- Unique current CV (enforced by partial index)

---

## 5. Configuration

### Application Properties

**File**: `application.yml`

```yaml
file:
  upload:
    base-path: uploads
    cv-path: cvs
    max-size: 10MB

spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

### Environment Variables

```bash
# Optional: Override upload paths
FILE_UPLOAD_BASE_PATH=/var/www/uploads
FILE_UPLOAD_CV_PATH=cvs

# Optional: Override max file size
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=20MB
```

### Docker Volume

**docker-compose.yml**:
```yaml
services:
  portfolio-backend:
    volumes:
      - cv-uploads:/app/uploads/cvs
    environment:
      - FILE_UPLOAD_BASE_PATH=/app/uploads

volumes:
  cv-uploads:
```

**Benefits**:
- Persistent storage across container restarts
- Shared storage in multi-container setups
- Easy backup and restore

---

## Related Documentation

- [API: CV Management](../api/cv.md) - CV API endpoints
- [Features: File Storage](./file-storage.md) - File storage architecture
- [Security: RBAC](../security/rbac.md) - Role-based access control
- [Development: Testing](../development/testing-guide.md) - Testing guidelines
