# Progressive Web App (PWA)

Service Worker implementation for offline support and optimized caching.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Caching Strategies](#caching-strategies)
4. [Configuration](#configuration)
5. [Related Documentation](#related-documentation)

---

## Overview

The PWA implementation provides:

- **Offline support** - Core app works without network
- **Smart caching** - API responses cached with strategies
- **Fast loading** - Prefetched assets for instant startup
- **Installable** - Add to home screen capability

Key characteristics:
- Angular Service Worker (@angular/service-worker)
- Freshness strategy for API data
- Performance strategy for images
- Automatic cache invalidation on deployment

---

## Architecture

### Configuration Files

| File | Purpose |
|------|---------|
| `ngsw-config.json` | Service Worker configuration |
| `site.webmanifest` | Web app manifest |
| `angular.json` | Build configuration with SW enabled |

### Asset Groups

| Group | Install Mode | Files |
|-------|--------------|-------|
| `app` | Prefetch | index.html, JS, CSS, favicons |
| `assets` | Lazy | Images, fonts, SVG files |

### Data Groups

| Group | Strategy | Max Age | Description |
|-------|----------|---------|-------------|
| `api-site-config` | Freshness | 1 day | Site configuration |
| `api-projects` | Freshness | 1 hour | Project data |
| `api-skills` | Freshness | 1 hour | Skills data |
| `api-articles` | Freshness | 30 min | Article listings |
| `api-experiences` | Freshness | 1 hour | Experience data |
| `api-tags` | Freshness | 1 hour | Tags data |
| `api-cv` | Freshness | 1 day | CV metadata |
| `api-images` | Performance | 7 days | Uploaded images |

---

## Caching Strategies

### Freshness Strategy

Network-first with cache fallback:

```json
{
  "name": "api-projects",
  "urls": ["**/api/projects", "**/api/projects/featured"],
  "cacheConfig": {
    "maxSize": 50,
    "maxAge": "1h",
    "timeout": "10s",
    "strategy": "freshness"
  }
}
```

Behavior:
1. Attempt network request with 10s timeout
2. If network fails, serve from cache
3. Cache updated on successful network response
4. Entries expire after configured maxAge

### Performance Strategy

Cache-first for static assets:

```json
{
  "name": "api-images",
  "urls": ["**/uploads/**"],
  "cacheConfig": {
    "maxSize": 100,
    "maxAge": "7d",
    "timeout": "5s",
    "strategy": "performance"
  }
}
```

Behavior:
1. Serve from cache if available
2. Background fetch to update cache
3. Network request only if not in cache
4. Optimal for rarely-changing content

---

## Configuration

### ngsw-config.json

```json
{
  "$schema": "./node_modules/@angular/service-worker/config/schema.json",
  "index": "/index.html",
  "assetGroups": [
    {
      "name": "app",
      "installMode": "prefetch",
      "resources": {
        "files": [
          "/favicon.ico",
          "/favicon.svg",
          "/index.html",
          "/site.webmanifest",
          "/*.css",
          "/*.js"
        ]
      }
    },
    {
      "name": "assets",
      "installMode": "lazy",
      "updateMode": "prefetch",
      "resources": {
        "files": [
          "/assets/**",
          "/*.(svg|jpg|jpeg|png|webp|gif|woff|woff2)"
        ]
      }
    }
  ],
  "navigationUrls": [
    "/**",
    "!/**/*.*",
    "!/api/**"
  ]
}
```

### Web App Manifest

```json
{
  "name": "Emmanuel Gabe - Portfolio",
  "short_name": "Portfolio",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#ffffff",
  "theme_color": "#1a1a2e",
  "icons": [
    {
      "src": "/web-app-manifest-192x192.png",
      "sizes": "192x192",
      "type": "image/png"
    },
    {
      "src": "/web-app-manifest-512x512.png",
      "sizes": "512x512",
      "type": "image/png"
    }
  ]
}
```

### Angular Build Configuration

```json
{
  "projects": {
    "portfolio-frontend": {
      "architect": {
        "build": {
          "configurations": {
            "production": {
              "serviceWorker": true,
              "ngswConfigPath": "ngsw-config.json"
            }
          }
        }
      }
    }
  }
}
```

---

## Navigation URLs

URLs handled by Service Worker for SPA routing:

```json
{
  "navigationUrls": [
    "/**",
    "!/**/*.*",
    "!/**/*__*",
    "!/api/**"
  ]
}
```

- `/**` - All paths handled
- `!/**/*.*` - Exclude files with extensions
- `!/api/**` - Exclude API calls (handled by data groups)

---

## Related Documentation

- [Frontend Architecture](../architecture/frontend-architecture.md) - Angular setup
- [SEO](./seo.md) - Meta tags and structured data

