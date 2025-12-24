# Search API

REST API for full-text search across articles, projects, and experiences.

---

## Table of Contents

1. [Overview](#overview)
2. [Admin Endpoints](#admin-endpoints)
3. [Data Models](#data-models)
4. [Error Codes](#error-codes)
5. [Related Documentation](#related-documentation)

---

## Overview

The Search API provides full-text search capabilities:

- Search articles by title, content, excerpt, tags
- Search projects by title, description, tech stack, tags
- Search experiences by company, role, description
- Manual reindex trigger

**Base URL:** `/api/admin/search`

**Authentication:** Required (ADMIN role)

---

## Admin Endpoints

### Search Articles

```
GET /api/admin/search/articles?q={query}
```

Full-text search across article title, content, excerpt, and tags.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `q` | string | Yes | Search query (2-100 characters) |

**Response:**
```json
[
  {
    "id": 1,
    "title": "Getting Started with Spring Boot",
    "slug": "getting-started-with-spring-boot",
    "excerpt": "A comprehensive guide...",
    "draft": false,
    "publishedAt": "2024-01-15T10:00:00Z",
    "tags": ["java", "spring"]
  }
]
```

---

### Search Projects

```
GET /api/admin/search/projects?q={query}
```

Full-text search across project title, description, tech stack, and tags.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `q` | string | Yes | Search query (2-100 characters) |

**Response:**
```json
[
  {
    "id": 1,
    "title": "Portfolio Application",
    "description": "Full-stack web application...",
    "techStack": "Angular, Spring Boot, PostgreSQL",
    "featured": true,
    "tags": ["angular", "spring-boot"]
  }
]
```

---

### Search Experiences

```
GET /api/admin/search/experiences?q={query}
```

Full-text search across experience company, role, and description.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `q` | string | Yes | Search query (2-100 characters) |

**Response:**
```json
[
  {
    "id": 1,
    "company": "Tech Company",
    "role": "Senior Developer",
    "type": "WORK",
    "startDate": "2022-01-01",
    "endDate": null
  }
]
```

---

### Rebuild Indices

```
POST /api/admin/search/reindex
```

Rebuilds all search indices from database. Use when indices are out of sync.

**Response:**
```json
{
  "totalIndexed": 150,
  "message": "Reindex completed successfully"
}
```

---

## Data Models

### ArticleSearchResult

```json
{
  "id": "long",
  "title": "string",
  "slug": "string",
  "excerpt": "string (nullable)",
  "draft": "boolean",
  "publishedAt": "datetime (nullable)",
  "tags": "string[]"
}
```

### ProjectSearchResult

```json
{
  "id": "long",
  "title": "string",
  "description": "string",
  "techStack": "string (nullable)",
  "featured": "boolean",
  "tags": "string[]"
}
```

### ExperienceSearchResult

```json
{
  "id": "long",
  "company": "string",
  "role": "string",
  "type": "string (WORK|EDUCATION|CERTIFICATION|VOLUNTEERING)",
  "startDate": "date",
  "endDate": "date (nullable)"
}
```

---

## Error Codes

| Status | Code | Description |
|--------|------|-------------|
| 400 | BAD_REQUEST | Query too short or too long |
| 401 | UNAUTHORIZED | Not authenticated |
| 403 | FORBIDDEN | Not authorized (requires ADMIN role) |

---

## Related Documentation

- [Full-Text Search](../features/search.md) - Feature documentation
- [GraphQL API](./graphql.md) - GraphQL search query
