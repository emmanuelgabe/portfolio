# Site Configuration Feature

Centralized configuration management for all site-wide settings including personal identity, hero section content, SEO metadata, and social links.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Database Schema](#database-schema)
- [Configuration Fields](#configuration-fields)
- [Profile Image Management](#profile-image-management)
- [Related Documentation](#related-documentation)

---

## Overview

The Site Configuration feature provides a single point of management for all configurable site content:

- **Personal Identity**: Full name and contact email
- **Hero Section**: Landing page title and description
- **SEO Metadata**: Browser title and meta description for search engines
- **Social Links**: GitHub and LinkedIn profile URLs
- **Profile Image**: Photo displayed in hero section with WebP optimization

The configuration follows a singleton pattern with a single row (id=1) in the database.

---

## Architecture

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `SiteConfiguration` | `entity/` | JPA entity with validation |
| `SiteConfigurationRepository` | `repository/` | Data access layer |
| `SiteConfigurationService` | `service/` | Business logic interface |
| `SiteConfigurationServiceImpl` | `service/impl/` | Service implementation |
| `SiteConfigurationController` | `controller/` | Public REST endpoint |
| `AdminSiteConfigurationController` | `controller/` | Admin REST endpoints |
| `SiteConfigurationMapper` | `mapper/` | MapStruct entity-DTO mapping |
| `SiteConfigurationResponse` | `dto/` | Response DTO |
| `UpdateSiteConfigurationRequest` | `dto/` | Update request DTO |

### Frontend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `SiteConfigurationService` | `services/` | HTTP client for API calls |
| `SiteConfigurationResponse` | `models/` | TypeScript interface |
| `UpdateSiteConfigurationRequest` | `models/` | TypeScript interface |
| `HomeComponent` | `components/home/` | Displays hero section |
| Admin form component | `pages/admin/` | Configuration edit form |

### Data Flow

```
Frontend                    Backend
   |                           |
   |-- GET /api/configuration -|-> SiteConfigurationController
   |                           |        |
   |<-- SiteConfigurationResponse       v
   |                           |   SiteConfigurationService
   |                           |        |
   |                           |        v
   |                           |   SiteConfigurationRepository
   |                           |        |
   |                           |        v
   |                           |   PostgreSQL (site_configuration)
```

---

## Database Schema

### site_configuration Table

```sql
CREATE TABLE site_configuration (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    hero_title VARCHAR(200) NOT NULL,
    hero_description TEXT NOT NULL,
    site_title VARCHAR(100) NOT NULL,
    seo_description VARCHAR(300) NOT NULL,
    profile_image_url VARCHAR(500),
    github_url VARCHAR(500) NOT NULL,
    linkedin_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_site_configuration_single_row CHECK (id = 1)
);
```

### Singleton Constraint

The `CHECK (id = 1)` constraint ensures only one configuration row exists. This guarantees:
- Consistent site-wide settings
- No orphan configurations
- Simple retrieval without ID parameter

### Migration

The schema is created by Flyway migration `V19__transform_hero_to_site_configuration.sql` which:
1. Creates the `site_configuration` table
2. Migrates data from the legacy `hero_section` table
3. Drops the old `hero_section` table

---

## Configuration Fields

### Personal Identity

| Field | Column | Constraints | Usage |
|-------|--------|-------------|-------|
| Full Name | `full_name` | 2-100 chars, required | Displayed in hero and contact |
| Email | `email` | Valid email, max 255 chars | Contact information |

### Hero Section

| Field | Column | Constraints | Usage |
|-------|--------|-------------|-------|
| Hero Title | `hero_title` | 3-200 chars, required | Main heading on landing page |
| Hero Description | `hero_description` | 10-1000 chars, required | Subheading text |

### SEO Metadata

| Field | Column | Constraints | Usage |
|-------|--------|-------------|-------|
| Site Title | `site_title` | 3-100 chars, required | Browser tab title |
| SEO Description | `seo_description` | 10-300 chars, required | Meta description tag |

### Social Links

| Field | Column | Constraints | Usage |
|-------|--------|-------------|-------|
| GitHub URL | `github_url` | Valid URL, max 500 chars | GitHub profile link |
| LinkedIn URL | `linkedin_url` | Valid URL, max 500 chars | LinkedIn profile link |

---

## Profile Image Management

### Upload Process

1. Admin uploads image via multipart form
2. ImageService validates file type (JPEG, PNG, GIF, WebP)
3. Image is converted to WebP format for optimization
4. File is saved to `/uploads/profile/profile.webp`
5. Previous image is automatically replaced
6. `profile_image_url` field is updated

### Storage Location

```
uploads/
  profile/
    profile.webp    # Current profile image
```

### Image Constraints

| Constraint | Value |
|------------|-------|
| Max file size | 5 MB |
| Allowed formats | JPEG, PNG, GIF, WebP |
| Output format | WebP |
| Fixed filename | `profile.webp` |

### Deletion

When the profile image is deleted:
1. File is removed from disk
2. `profile_image_url` is set to `null`
3. Frontend displays default placeholder

---

## Related Documentation

- [Site Configuration API](../api/site-configuration.md) - REST endpoints reference
- [Image Processing](./image-processing.md) - WebP conversion details
- [File Storage](./file-storage.md) - Upload directory structure
