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

Overview of the Portfolio application features. Each feature is documented in detail in its respective file.

**Feature Categories**:
- Content Management: Blog articles, experiences, CV management, site configuration
- User Interaction: Contact form with rate limiting
- Media Processing: Image optimization, file storage
- Infrastructure: Messaging, event streaming, caching, resilience

---

## 2. Content Management Features

### 2.1 Blog Articles

**File**: [blog-articles.md](./blog-articles.md)

Markdown blog system with GitHub Flavored Markdown support.

**Capabilities**: Flexmark rendering, slug generation, reading time calculation, draft/publish workflow, tag categorization

---

### 2.2 Experience Management

**File**: [experience-management.md](./experience-management.md)

Timeline-based experience management (work, education, certifications, volunteering).

**Capabilities**: Four experience types, date validation, ongoing experiences, type-based filtering

---

### 2.3 CV Management

**File**: [cv-management.md](./cv-management.md)

CV/resume management system with versioning.

**Capabilities**: Multiple versions, single "current" designation, PDF validation, public download

---

### 2.4 Site Configuration

**File**: [site-configuration.md](./site-configuration.md)

Centralized site-wide settings (identity, hero, SEO, social links).

**Capabilities**: Profile image upload, WebP optimization, singleton pattern

---

## 3. User Interaction Features

### 3.1 Contact Form

**File**: [contact-form.md](./contact-form.md)

Rate-limited contact form with email notification.

**Capabilities**: IP-based rate limiting (Redis), email notification, validation, proxy support

---

## 4. Media Processing Features

### 4.1 Image Processing

**File**: [image-processing.md](./image-processing.md)

Automatic image optimization with WebP conversion.

**Capabilities**: WebP conversion, thumbnail generation (400x300px), MIME validation, 25-35% size reduction

---

### 4.2 File Storage

**File**: [file-storage.md](./file-storage.md)

Secure file storage with validation and cleanup.

**Capabilities**: Organized directory structure, timestamped filenames, MIME validation, path traversal prevention

---

## 5. Infrastructure Features

### 5.1 Messaging (RabbitMQ)

**File**: [messaging.md](./messaging.md)

Asynchronous message processing for email, image processing, audit logging.

**Capabilities**: Non-blocking operations, dead letter queue, automatic retries

---

### 5.2 Event Streaming (Kafka)

**File**: [event-streaming.md](./event-streaming.md)

Event-driven architecture for admin actions, analytics, activity tracking.

**Capabilities**: Admin events, analytics events, activity events, 3 partitions per topic

---

### 5.3 Audit System

**File**: [audit-system.md](./audit-system.md)

AOP-based audit logging for administrative actions.

**Capabilities**: @Auditable annotation, old/new value capture, CSV/JSON/PDF export, 90-day retention

---

### 5.4 Full-text Search

**File**: [search.md](./search.md)

Elasticsearch-based full-text search.

**Capabilities**: Articles/projects/experiences search, JPA fallback, weekly reindex

---

### 5.5 Batch Processing

**File**: [batch-processing.md](./batch-processing.md)

Spring Batch jobs for scheduled maintenance.

**Capabilities**: Audit cleanup, report generation, stats aggregation, sitemap regeneration

---

### 5.6 Visitor Tracking

**File**: [visitor-tracking.md](./visitor-tracking.md)

Real-time visitor tracking using Redis and SSE.

**Capabilities**: Active visitors count, daily unique visitors, SSE stream, 7-day chart

---

### 5.7 Circuit Breaker

**File**: [circuit-breaker.md](./circuit-breaker.md)

Resilience patterns for external service calls.

**Capabilities**: Circuit breaker (email), retry with backoff, fallback handling

---

### 5.8 PWA (Offline Support)

**File**: [pwa.md](./pwa.md)

Progressive Web App with Service Worker.

**Capabilities**: Offline access, asset caching, installable as native app

---

### 5.9 Internationalization (i18n)

**File**: [i18n.md](./i18n.md)

Multi-language support (10 languages including RTL).

**Capabilities**: Browser detection, localStorage persistence, Arabic RTL support

---

### 5.10 SEO

**File**: [seo.md](./seo.md)

Search engine optimization.

**Capabilities**: Dynamic meta tags, OpenGraph/Twitter Cards, JSON-LD structured data

---

### 5.11 Sitemap

**File**: [sitemap.md](./sitemap.md)

Dynamic sitemap.xml generation.

**Capabilities**: All projects/articles, daily updates, priority attributes

---

### 5.12 Application Caching

**File**: [caching.md](./caching.md)

Redis-based application caching.

**Capabilities**: 5 caches (siteConfig, skills, projects, experiences, tags), TTL expiration, auto-eviction

---

## Related Documentation

- [API Documentation](../api/README.md) - API endpoints
- [Architecture: Database Schema](../architecture/database-schema.md) - Database design
- [Architecture: Frontend](../architecture/frontend-architecture.md) - Angular architecture
- [Security: JWT Implementation](../security/jwt-implementation.md) - Authentication
- [Reference: Configuration Properties](../reference/configuration-properties.md) - Configuration
