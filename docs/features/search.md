# Full-Text Search

Elasticsearch-based full-text search with JPA fallback for articles, projects, and experiences.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Indexed Entities](#indexed-entities)
4. [Configuration](#configuration)
5. [Related Documentation](#related-documentation)

---

## Overview

The search feature provides full-text search capabilities:

- **Article search** - Search by title, content, excerpt, and tags
- **Project search** - Search by title, description, tech stack, and tags
- **Experience search** - Search by company, role, and description

Key capabilities:
- Elasticsearch primary implementation
- JPA-based fallback when Elasticsearch unavailable
- Automatic index updates on entity changes
- Manual reindex endpoint for admin
- Weekly scheduled reindex job

---

## Architecture

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `SearchService` | `service/` | Search interface |
| `ElasticsearchSearchServiceImpl` | `service/impl/` | Elasticsearch implementation |
| `JpaFallbackSearchServiceImpl` | `service/impl/` | JPA fallback implementation |
| `ArticleSearchRepository` | `search/` | Elasticsearch article repository |
| `ProjectSearchRepository` | `search/` | Elasticsearch project repository |
| `ExperienceSearchRepository` | `search/` | Elasticsearch experience repository |
| `SearchDocumentMapper` | `search/` | Entity to document mapper |
| `SearchIndexEventListener` | `search/` | Event listener for index updates |
| `AdminSearchController` | `controller/` | REST API endpoints |

### Document Classes

| Document | Fields Indexed |
|----------|---------------|
| `ArticleDocument` | title, content, excerpt, tags |
| `ProjectDocument` | title, description, techStack, tags |
| `ExperienceDocument` | company, role, description |

### Frontend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `SearchService` | `services/` | HTTP service for search API |
| `SearchInputComponent` | `components/shared/` | Reusable search input |

---

## Indexed Entities

### Articles

Searchable fields:
- `title` - Article title
- `content` - Full markdown content
- `excerpt` - Article summary
- `tags` - Associated tag names

### Projects

Searchable fields:
- `title` - Project title
- `description` - Project description
- `techStack` - Technologies used
- `tags` - Associated tag names

### Experiences

Searchable fields:
- `company` - Company name
- `role` - Job title
- `description` - Experience description

---

## Configuration

### Application Properties

```yaml
# Elasticsearch connection
elasticsearch:
  enabled: ${ELASTICSEARCH_ENABLED:false}
  host: ${ELASTICSEARCH_HOST:localhost}
  port: ${ELASTICSEARCH_PORT:9200}
  username: ${ELASTICSEARCH_USERNAME:elastic}
  password: ${ELASTICSEARCH_PASSWORD:}

# Batch reindex job
batch:
  search-reindex:
    cron: "0 0 3 * * SUN"  # Every Sunday at 3:00 AM
```

### Docker Compose

```yaml
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.12.0
    container_name: portfolio-elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD-SHELL", "curl -s http://localhost:9200/_cluster/health | grep -q 'green\\|yellow'"]
      interval: 30s
      timeout: 10s
      retries: 5
```

---

## Fallback Strategy

When Elasticsearch is unavailable or disabled:

1. `JpaFallbackSearchServiceImpl` is activated
2. Searches use LIKE queries on database
3. Less efficient but functional
4. Logs warning when falling back

---

## Index Management

### Automatic Updates

Entity changes trigger automatic index updates:
- CREATE: Document added to index
- UPDATE: Document updated in index
- DELETE: Document removed from index

### Manual Reindex

Admin can trigger full reindex via API:

```
POST /api/admin/search/reindex
```

### Scheduled Reindex

Weekly job rebuilds all indices every Sunday at 3:00 AM.

---

## Related Documentation

- [Search API](../api/search.md) - REST API endpoints
- [GraphQL API](../api/graphql.md) - GraphQL search queries
- [Batch Processing](./batch-processing.md) - Scheduled reindex job
