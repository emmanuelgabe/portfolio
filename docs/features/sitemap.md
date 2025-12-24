# Sitemap Generation

Dynamic sitemap.xml generation with scheduled updates.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [URL Structure](#url-structure)
4. [Configuration](#configuration)
5. [Related Documentation](#related-documentation)

---

## Overview

The sitemap system provides:

- **Dynamic generation** - Includes all projects and articles
- **Scheduled updates** - Daily batch job regeneration
- **Priority weighting** - Higher priority for important pages
- **Last modification dates** - Accurate lastmod for content

Key characteristics:
- XML Sitemap Protocol 0.9
- Batch job scheduled daily at 4:00 AM
- Includes static pages, projects, and published articles
- Excludes admin and draft content

---

## Architecture

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `SitemapService` | `service/` | Sitemap interface |
| `SitemapServiceImpl` | `service/impl/` | Generation implementation |
| `SitemapGenerationJobConfig` | `batch/` | Scheduled batch job |

### Generation Flow

```
┌─────────────────────────────────────────────────────────────┐
│                  SitemapService.generateSitemap()            │
│  1. Add static pages (home, blog, contact)                  │
│  2. Fetch all projects from database                        │
│  3. Fetch published articles (non-draft)                    │
│  4. Generate XML with lastmod, priority, changefreq         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  SitemapService.writeSitemapToFile()         │
│  Write generated XML to configured output path              │
└─────────────────────────────────────────────────────────────┘
```

---

## URL Structure

### Static Pages

| URL | Priority | Change Frequency |
|-----|----------|------------------|
| `/` | 1.0 | weekly |
| `/blog` | 0.8 | weekly |
| `/contact` | 0.5 | monthly |

### Dynamic Pages

| URL Pattern | Priority | Change Frequency | lastmod |
|-------------|----------|------------------|---------|
| `/projects/{id}` | 0.6 | monthly | updatedAt |
| `/blog/{slug}` | 0.7 | monthly | updatedAt or publishedAt |

### XML Output Format

```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://emmanuelgabe.com</loc>
    <changefreq>weekly</changefreq>
    <priority>1.0</priority>
  </url>
  <url>
    <loc>https://emmanuelgabe.com/blog</loc>
    <changefreq>weekly</changefreq>
    <priority>0.8</priority>
  </url>
  <url>
    <loc>https://emmanuelgabe.com/projects/1</loc>
    <lastmod>2024-01-15</lastmod>
    <changefreq>monthly</changefreq>
    <priority>0.6</priority>
  </url>
  <url>
    <loc>https://emmanuelgabe.com/blog/my-article</loc>
    <lastmod>2024-01-20</lastmod>
    <changefreq>monthly</changefreq>
    <priority>0.7</priority>
  </url>
</urlset>
```

---

## Configuration

### Application Properties

```yaml
sitemap:
  base-url: ${SITEMAP_BASE_URL:https://emmanuelgabe.com}
  output-path: ${SITEMAP_OUTPUT_PATH:sitemap.xml}

batch:
  sitemap:
    cron: "0 0 4 * * ?"  # Daily at 4:00 AM
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SITEMAP_BASE_URL` | `https://emmanuelgabe.com` | Base URL for all sitemap entries |
| `SITEMAP_OUTPUT_PATH` | `sitemap.xml` | Output file path |

### Nginx/Reverse Proxy

Serve sitemap from generated location:

```nginx
location = /sitemap.xml {
    alias /app/sitemap.xml;
    add_header Content-Type application/xml;
}
```

---

## Exclusions

The following are NOT included in sitemap:

- Admin pages (`/admin/*`)
- Draft articles (`draft = true`)
- Login page
- Error pages
- API endpoints

---

## Related Documentation

- [Batch Processing](./batch-processing.md) - Scheduled generation job
- [SEO](./seo.md) - Meta tags and structured data
- [Blog Articles](./blog-articles.md) - Article publishing workflow

