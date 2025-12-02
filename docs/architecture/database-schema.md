# Database Schema

---

## Table of Contents
1. [Overview](#1-overview)
2. [Schema Diagram](#2-schema-diagram)
3. [Core Entities](#3-core-entities)
4. [Authentication and Security](#4-authentication-and-security)
5. [Content Management](#5-content-management)
6. [Many-to-Many Relationships](#6-many-to-many-relationships)
7. [Indexes and Constraints](#7-indexes-and-constraints)
8. [Migration History](#8-migration-history)

---

## 1. Overview

The portfolio database uses **PostgreSQL** with **Flyway** for version-controlled schema migrations. Designed for scalability, data integrity, and query performance.

**Database Features**:
- Automated timestamps with `CURRENT_TIMESTAMP`
- Cascading deletes for referential integrity
- Check constraints for data validation
- Comprehensive indexing strategy
- UUID-based refresh tokens
- Support for draft/publish workflows

**Migration Tool**: Flyway (V1-V19 migrations applied)

---

## 2. Schema Diagram

### Entity Relationships

```
users ──┬── 1:N ── refresh_tokens
        ├── 1:N ── cvs
        └── 1:N ── articles

projects ─── M:N ── tags (via project_tags)

articles ──┬── M:N ── tags (via article_tags)
           └── 1:N ── article_images

experiences (independent entity)

skills (independent entity)

site_configuration (singleton entity)
```

---

## 3. Core Entities

### Projects Table

**Purpose**: Portfolio projects with images and metadata

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-incrementing primary key |
| title | VARCHAR(100) | NOT NULL | Project title |
| description | TEXT | NOT NULL | Full project description |
| tech_stack | VARCHAR(500) | NOT NULL | Technologies used |
| github_url | VARCHAR(255) | Nullable | GitHub repository URL |
| image_url | VARCHAR(255) | Nullable | Project image (WebP) |
| thumbnail_url | VARCHAR(255) | Nullable | Thumbnail (400x300px) |
| demo_url | VARCHAR(255) | Nullable | Live demo URL |
| featured | BOOLEAN | NOT NULL, DEFAULT false | Homepage featured flag |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Last update timestamp |

**Indexes**:
- `idx_projects_featured` - Filter featured projects
- `idx_projects_title` - Search by title
- `idx_projects_created_at DESC` - Chronological sorting

---

### Tags Table

**Purpose**: Technology tags for projects and articles

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-incrementing primary key |
| name | VARCHAR(50) | NOT NULL, UNIQUE | Tag name |
| color | VARCHAR(7) | NOT NULL, CHECK hex format | Hex color code (#RGB or #RRGGBB) |

**Constraints**:
```sql
CHECK (color ~ '^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$')
```

**Indexes**:
- `idx_tags_name` - Name lookups

---

### Skills Table

**Purpose**: Technical skills with categories and proficiency

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-incrementing primary key |
| name | VARCHAR(50) | NOT NULL | Skill name |
| icon | VARCHAR(50) | NOT NULL | Bootstrap Icons class name |
| color | VARCHAR(7) | NOT NULL, CHECK hex | Hex color code |
| category | VARCHAR(20) | NOT NULL, CHECK enum | FRONTEND, BACKEND, DATABASE, DEVOPS, TOOLS |
| display_order | INTEGER | NOT NULL, DEFAULT 0, CHECK >= 0 | Display order |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Last update timestamp |

**Constraints**:
```sql
CHECK (category IN ('FRONTEND', 'BACKEND', 'DATABASE', 'DEVOPS', 'TOOLS'))
CHECK (display_order >= 0)
```

**Indexes**:
- `idx_skills_display_order` - Ordered display
- `idx_skills_category` - Filter by category
- `idx_skills_name` - Name lookups

---

### Experiences Table

**Purpose**: Professional, educational, certification, and volunteering experiences

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-incrementing primary key |
| company | VARCHAR(200) | NOT NULL | Company or institution name |
| role | VARCHAR(200) | NOT NULL | Job title, degree, or certification |
| start_date | DATE | NOT NULL | Experience start date |
| end_date | DATE | Nullable | End date (NULL = ongoing) |
| description | TEXT | NOT NULL | Detailed description |
| type | VARCHAR(50) | NOT NULL, CHECK enum | WORK, EDUCATION, CERTIFICATION, VOLUNTEERING |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Last update timestamp |

**Constraints**:
```sql
CHECK (type IN ('WORK', 'EDUCATION', 'CERTIFICATION', 'VOLUNTEERING'))
CHECK (end_date IS NULL OR end_date >= start_date)
```

**Indexes**:
- `idx_experiences_start_date DESC` - Chronological sorting
- `idx_experiences_type` - Filter by type
- `idx_experiences_ongoing WHERE end_date IS NULL` - Ongoing experiences (partial index)

---

## 4. Authentication and Security

### Users Table

**Purpose**: Authenticated users with role-based access control

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-incrementing primary key |
| username | VARCHAR(50) | NOT NULL, UNIQUE, CHECK >= 3 chars | Username |
| email | VARCHAR(100) | NOT NULL, UNIQUE, CHECK email format | Email address |
| password | VARCHAR(255) | NOT NULL | BCrypt hashed password |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'ROLE_GUEST' | ROLE_ADMIN or ROLE_GUEST |
| enabled | BOOLEAN | NOT NULL, DEFAULT true | Account status |
| account_non_expired | BOOLEAN | NOT NULL, DEFAULT true | Account expiration |
| account_non_locked | BOOLEAN | NOT NULL, DEFAULT true | Account lock status |
| credentials_non_expired | BOOLEAN | NOT NULL, DEFAULT true | Password expiration |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Last update timestamp |

**Constraints**:
```sql
CHECK (role IN ('ROLE_ADMIN', 'ROLE_GUEST'))
CHECK (char_length(username) >= 3)
CHECK (email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
```

**Indexes**:
- `idx_users_username` - Login lookups
- `idx_users_email` - Email lookups
- `idx_users_role` - Role filtering
- `idx_users_enabled` - Active users

**Seed Data**: Default admin user (username: `admin`, password: `Admin123!`)

---

### Refresh Tokens Table

**Purpose**: JWT refresh tokens for secure token rotation

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-incrementing primary key |
| token | VARCHAR(255) | NOT NULL, UNIQUE | UUID refresh token |
| user_id | BIGINT | NOT NULL, FK → users(id) CASCADE | Token owner |
| expiry_date | TIMESTAMP | NOT NULL | Token expiration |
| revoked | BOOLEAN | NOT NULL, DEFAULT false | Revocation flag |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Creation timestamp |

**Indexes**:
- `idx_refresh_tokens_token` - Token lookups
- `idx_refresh_tokens_user_id` - User's tokens
- `idx_refresh_tokens_expiry_date` - Expiration checks
- `idx_refresh_tokens_revoked` - Active tokens

---

### CVs Table

**Purpose**: Version-controlled CV uploads

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-incrementing primary key |
| user_id | BIGINT | NOT NULL, FK → users(id) CASCADE | CV owner |
| file_name | VARCHAR(255) | NOT NULL | Stored filename |
| original_file_name | VARCHAR(255) | NOT NULL | Original upload name |
| file_url | VARCHAR(255) | NOT NULL | CV file URL |
| file_size | BIGINT | NOT NULL | File size in bytes |
| uploaded_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Upload timestamp |
| current | BOOLEAN | NOT NULL, DEFAULT false | Current CV flag |

**Constraints**:
```sql
UNIQUE INDEX idx_cvs_user_current ON cvs(user_id) WHERE current = true
-- Ensures only ONE current CV per user
```

**Indexes**:
- `idx_cvs_user_id` - User's CVs
- `idx_cvs_current` - Current CVs
- `idx_cvs_user_current` - Unique current CV per user (partial)

---

## 5. Content Management

### Articles Table

**Purpose**: Blog articles with Markdown content

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-incrementing primary key |
| title | VARCHAR(200) | NOT NULL | Article title |
| slug | VARCHAR(200) | NOT NULL, UNIQUE | URL-friendly identifier |
| content | TEXT | NOT NULL | Markdown source |
| excerpt | VARCHAR(500) | Nullable | Short summary |
| author_id | BIGINT | NOT NULL, FK → users(id) RESTRICT | Article author |
| draft | BOOLEAN | NOT NULL, DEFAULT true | Draft status |
| published_at | TIMESTAMP | Nullable | Publication timestamp |
| reading_time_minutes | INTEGER | Nullable | Estimated reading time |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Last update timestamp |

**Indexes**:
- `idx_articles_slug` - Slug lookups
- `idx_articles_published DESC WHERE draft = false` - Published articles (partial)
- `idx_articles_draft` - Draft filtering
- `idx_articles_author` - Author's articles

---

### Article Images Table

**Purpose**: Images embedded in articles

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK | Auto-incrementing primary key |
| article_id | BIGINT | NOT NULL, FK → articles(id) CASCADE | Parent article |
| image_url | VARCHAR(255) | NOT NULL | Image URL |
| thumbnail_url | VARCHAR(255) | Nullable | Thumbnail URL |
| uploaded_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Upload timestamp |

**Indexes**:
- `idx_article_images_article` - Article's images

---

### Site Configuration Table

**Purpose**: Centralized site-wide configuration (singleton pattern)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PK, CHECK (id = 1) | Always 1 (singleton) |
| full_name | VARCHAR(100) | NOT NULL | Site owner's name |
| email | VARCHAR(255) | NOT NULL | Contact email |
| hero_title | VARCHAR(200) | NOT NULL | Hero section title |
| hero_description | TEXT | NOT NULL | Hero section description |
| site_title | VARCHAR(100) | NOT NULL | Browser tab title |
| seo_description | VARCHAR(300) | NOT NULL | Meta description |
| profile_image_url | VARCHAR(500) | Nullable | Profile image URL |
| github_url | VARCHAR(500) | NOT NULL | GitHub profile URL |
| linkedin_url | VARCHAR(500) | NOT NULL | LinkedIn profile URL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Last update timestamp |

**Constraints**:
```sql
CHECK (id = 1)  -- Ensures singleton pattern
```

---

## 6. Many-to-Many Relationships

### Project Tags Junction Table

**Table**: `project_tags`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| project_id | BIGINT | PK, FK → projects(id) CASCADE | Project reference |
| tag_id | BIGINT | PK, FK → tags(id) CASCADE | Tag reference |

**Composite Primary Key**: `(project_id, tag_id)`

**Indexes**:
- `idx_project_tags_tag_id` - Projects by tag
- `idx_project_tags_project_id` - Tags by project

**Cascade Behavior**: Deleting project or tag removes relationship

---

### Article Tags Junction Table

**Table**: `article_tags`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| article_id | BIGINT | PK, FK → articles(id) CASCADE | Article reference |
| tag_id | BIGINT | PK, FK → tags(id) CASCADE | Tag reference |

**Composite Primary Key**: `(article_id, tag_id)`

**Indexes**:
- `idx_article_tags_article` - Tags by article
- `idx_article_tags_tag` - Articles by tag

**Cascade Behavior**: Deleting article or tag removes relationship

---

## 7. Indexes and Constraints

### Performance Indexes

**Chronological Sorting**:
```sql
CREATE INDEX idx_projects_created_at ON projects(created_at DESC);
CREATE INDEX idx_experiences_start_date ON experiences(start_date DESC);
CREATE INDEX idx_articles_published ON articles(published_at DESC) WHERE draft = false;
```

**Filtering and Search**:
```sql
CREATE INDEX idx_skills_category ON skills(category);
CREATE INDEX idx_experiences_type ON experiences(type);
CREATE INDEX idx_projects_featured ON projects(featured);
```

**Unique Constraints**:
```sql
ALTER TABLE tags ADD CONSTRAINT uk_tags_name UNIQUE (name);
ALTER TABLE articles ADD CONSTRAINT uk_articles_slug UNIQUE (slug);
ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
```

**Partial Indexes** (PostgreSQL-specific):
```sql
-- Only index ongoing experiences
CREATE INDEX idx_experiences_ongoing ON experiences(end_date) WHERE end_date IS NULL;

-- Only index published articles
CREATE INDEX idx_articles_published ON articles(published_at DESC) WHERE draft = false;

-- Ensure only one current CV per user
CREATE UNIQUE INDEX idx_cvs_user_current ON cvs(user_id) WHERE current = true;
```

---

## 8. Migration History

| Version | Description | Tables Created/Modified |
|---------|-------------|-------------------------|
| V1 | Initial schema | projects, tags, project_tags |
| V2 | Skills management | skills |
| V3 | Authentication | users, refresh_tokens |
| V4 | Skills refactoring | skills (removed level column) |
| V5 | CV management | cvs |
| V6 | Project thumbnails | projects (added thumbnail_url) |
| V7 | Experience timeline | experiences |
| V8 | Blog articles | articles, article_tags, article_images |
| V9 | Article metrics | articles (removed views_count) |
| V19 | Site configuration | site_configuration (replaces hero_section) |

---

## Related Documentation

- [API Overview](../api/README.md) - API documentation
- [Features: Image Processing](../features/image-processing.md) - Image storage
- [Security: Authentication](../security/authentication.md) - Auth system
- [Development: Setup](../development/setup.md) - Database setup instructions
