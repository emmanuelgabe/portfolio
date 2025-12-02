# Features Overview

---

## Table of Contents
1. [Introduction](#1-introduction)
2. [Content Management Features](#2-content-management-features)
3. [User Interaction Features](#3-user-interaction-features)
4. [Media Processing Features](#4-media-processing-features)
5. [Infrastructure Features](#5-infrastructure-features)

---

## 1. Introduction

This document provides an overview of the Portfolio application features. Each feature is documented in detail in its respective documentation file.

**Feature Categories**:
- Content Management: Blog articles, experiences, CV management, site configuration
- User Interaction: Contact form with rate limiting
- Media Processing: Image optimization, file storage
- Infrastructure: Security, validation, error handling

---

## 2. Content Management Features

### 2.1 Blog Articles

**File**: [blog-articles.md](./blog-articles.md)

**Description**: Full-featured blog system with Markdown rendering, GitHub Flavored Markdown support, and draft/publish workflow.

**Key Capabilities**:
- Markdown content with Flexmark renderer
- GitHub Flavored Markdown extensions (tables, strikethrough, task lists)
- Automatic slug generation from title
- Reading time calculation (200 words/minute)
- Draft/publish status management
- Tag-based categorization
- Inline image support with Azure Blob Storage

**API Endpoints**:
- Public: `GET /api/articles`, `GET /api/articles/{slug}`
- Admin: `POST /api/articles/admin`, `PUT /api/articles/admin/{id}/publish`

**Technologies**: Flexmark (Markdown rendering), MapStruct (DTO mapping)

---

### 2.2 Experience Management

**File**: [experience-management.md](./experience-management.md)

**Description**: Timeline-based experience management for work history, education, certifications, and volunteering.

**Key Capabilities**:
- Four experience types: WORK, EDUCATION, CERTIFICATION, VOLUNTEERING
- Date range validation (end_date >= start_date)
- Ongoing experiences support (null endDate)
- Type-based filtering
- Recent experiences endpoint (configurable limit)
- Chronological ordering (most recent first)

**API Endpoints**:
- Public: `GET /api/experiences`, `GET /api/experiences/type/{type}`, `GET /api/experiences/ongoing`
- Admin: `POST /api/experiences/admin`, `PUT /api/experiences/admin/{id}`

**Use Cases**: Professional timeline, education history, certifications showcase

---

### 2.3 CV Management

**File**: [cv-management.md](./cv-management.md)

**Description**: Complete CV/resume management system with versioning and public download capability.

**Key Capabilities**:
- Multiple CV versions support
- Single "current" CV designation (enforced by database constraint)
- PDF validation (magic bytes)
- User tracking (who uploaded each CV)
- Public access to current CV
- Admin-only upload and management
- Historical version preservation

**API Endpoints**:
- Public: `GET /api/cv/current`, `GET /api/cv/download/{id}`
- Admin: `POST /api/admin/cv/upload`, `PUT /api/admin/cv/{id}/set-current`

**Database**: Unique partial index ensures only one `is_current = true` at a time

---

### 2.4 Site Configuration

**File**: [site-configuration.md](./site-configuration.md)

**Description**: Centralized configuration for all site-wide settings including personal identity, hero section content, SEO metadata, and social links.

**Key Capabilities**:
- Personal identity management (full name, email)
- Hero section content (title, description)
- SEO metadata (site title, meta description)
- Social links (GitHub, LinkedIn URLs)
- Profile image upload with WebP optimization
- Singleton pattern (single configuration row)

**API Endpoints**:
- Public: `GET /api/configuration`
- Admin: `PUT /api/admin/configuration`, `POST /api/admin/configuration/profile-image`

**Database**: Single row constraint ensures only one configuration exists

---

## 3. User Interaction Features

### 3.1 Contact Form

**File**: [contact-form.md](./contact-form.md)

**Description**: Rate-limited contact form with email notification and spam prevention.

**Key Capabilities**:
- IP-based rate limiting (Redis-backed)
- Configurable rate limits per environment
- Email notification to admin
- Request validation (name, email, message)
- X-Forwarded-For header support (proxy-aware)
- Atomic Redis operations for thread safety

**API Endpoint**: `POST /api/contact` (public, rate-limited)

**Rate Limits**:
- Development: 10 requests/hour per IP
- Staging: 5 requests/hour per IP
- Production: 3 requests/hour per IP

**Technologies**: Redis (rate limiting), Spring Mail (email sending)

---

## 4. Media Processing Features

### 4.1 Image Processing

**File**: [image-processing.md](./image-processing.md)

**Description**: Automatic image optimization with WebP conversion and thumbnail generation.

**Key Capabilities**:
- Automatic WebP conversion (from JPEG, PNG, WebP)
- Thumbnail generation (400×300px, quality 0.8)
- Full image optimization (quality 0.85)
- MIME type validation (magic bytes)
- Old image cleanup (prevents orphaned files)
- 25-35% file size reduction vs JPEG

**Technologies**: Thumbnailator (Java image processing library)

**Performance**:
- Processing time: ~350ms per image (full + thumbnail)
- File size savings: 72% vs PNG, 30% vs JPEG
- Bandwidth savings: 93% reduction with thumbnails

**Use Cases**: Project images, portfolio gallery, optimized page load times

---

### 4.2 File Storage

**File**: [file-storage.md](./file-storage.md)

**Description**: Secure, organized file storage architecture with validation and cleanup.

**Key Capabilities**:
- Organized directory structure (`uploads/projects/`, `uploads/cvs/`)
- Timestamped filenames (prevents collisions)
- MIME type validation (magic bytes, not extensions)
- File size limits (10 MB default)
- Path traversal prevention
- Old file cleanup on updates

**Storage Structure**:
```
uploads/
├── projects/           # Project images (WebP)
│   ├── project_1_1700000000.webp
│   └── project_1_1700000000_thumb.webp
├── cvs/                # CV documents (PDF)
│   └── cv_1_1700000000.pdf
└── articles/           # Article images (JPEG, PNG, WebP)
    └── article_1_1700000000.webp
```

**Security**:
- Magic bytes validation (content-based, not extension-based)
- Path sanitization (prevents directory traversal)
- File size limits (Spring Boot enforced)
- Content-Type validation

---

## 5. Infrastructure Features

### 5.1 File Validation

**Cross-Feature**: All file upload features (projects, CVs, articles)

**Validation Layers**:

1. **MIME Type Validation** (magic bytes):
   - JPEG: `FF D8 FF`
   - PNG: `89 50 4E 47 0D 0A 1A 0A`
   - WebP: `52 49 46 46 ... 57 45 42 50`
   - PDF: `25 50 44 46`

2. **File Size Validation**:
   - Max size: 10 MB (configurable)
   - Enforced by Spring Boot and service layer

3. **Path Validation**:
   - Filename sanitization
   - Directory traversal prevention
   - Absolute path verification

**Implementation**: `FileStorageService`, `ImageService`

---

### 5.2 Markdown Rendering

**Feature**: Blog Articles

**GitHub Flavored Markdown Extensions**:
- Tables
- Strikethrough (`~~text~~`)
- Task lists (`- [ ]`, `- [x]`)
- Autolinks (URLs automatically converted)
- Fenced code blocks with syntax highlighting support

**Implementation**:
```java
Parser parser = Parser.builder()
    .extensions(Arrays.asList(
        TablesExtension.create(),
        StrikethroughExtension.create(),
        TaskListExtension.create(),
        AutolinkExtension.create()
    ))
    .build();

Node document = parser.parse(markdownContent);
String html = renderer.render(document);
```

**Service**: `MarkdownService` (converts Markdown → HTML)

---

### 5.3 Slug Generation

**Feature**: Blog Articles

**Algorithm**:
1. Convert title to lowercase
2. Trim whitespace
3. Remove special characters (keep only a-z, 0-9, spaces, hyphens)
4. Replace spaces with hyphens
5. Collapse multiple hyphens
6. Remove leading/trailing hyphens

**Example**:
```
"My First Blog Post!" → "my-first-blog-post"
"Angular 18: What's New?" → "angular-18-whats-new"
"C++ Tutorial (Part 1)" → "c-tutorial-part-1"
```

**Implementation**: Entity lifecycle listener (`@PrePersist`, `@PreUpdate`)

---

### 5.4 Reading Time Calculation

**Feature**: Blog Articles

**Algorithm**: Words per minute (WPM) calculation

**Formula**: `readingTime = Math.ceil(wordCount / 200)`

**Examples**:
- 500 words → 3 minutes
- 1000 words → 5 minutes
- 1500 words → 8 minutes

**Implementation**: Automatic calculation in `Article` entity on save

---

## Related Documentation

- [API Documentation](../api/README.md) - API endpoints for all features
- [Architecture: Database Schema](../architecture/database-schema.md) - Database design
- [Architecture: Frontend](../architecture/frontend-architecture.md) - Angular architecture
- [Architecture: Error Handling](../architecture/error-handling.md) - Error handling strategy
- [Security: JWT Implementation](../security/jwt-implementation.md) - Authentication
- [Security: Rate Limiting](../security/rate-limiting.md) - Rate limiting details
- [Reference: Configuration Properties](../reference/configuration-properties.md) - Configuration
- [Reference: Error Codes](../reference/error-codes.md) - Error codes reference
