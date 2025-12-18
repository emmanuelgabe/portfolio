# GraphQL API

GraphQL API alternative to REST with Relay-style pagination.

---

## Table of Contents

1. [Overview](#overview)
2. [Endpoint](#endpoint)
3. [Schema Types](#schema-types)
4. [Queries](#queries)
5. [Mutations](#mutations)
6. [Configuration](#configuration)
7. [Related Documentation](#related-documentation)

---

## Overview

The GraphQL API provides an alternative to REST:

- Schema-first approach
- Relay Connection pagination
- Filtering and sorting
- Full CRUD operations for admin

Key characteristics:
- Single endpoint `/graphql`
- GraphiQL UI available at `/graphiql`
- Authentication via JWT Bearer token
- Admin mutations require ADMIN role

---

## Endpoint

```
POST /graphql
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>  (for admin mutations)
```

**Request body:**
```json
{
  "query": "query { ... }",
  "variables": { }
}
```

---

## Schema Types

### Core Types

| Type | Description |
|------|-------------|
| `Project` | Portfolio project with images and tags |
| `Article` | Blog article with content and tags |
| `Skill` | Technical skill with category |
| `Experience` | Work/education experience |
| `Tag` | Categorization tag |
| `SiteConfiguration` | Site-wide settings |
| `Cv` | CV document |

### Pagination (Relay)

| Type | Description |
|------|-------------|
| `PageInfo` | hasNextPage, hasPreviousPage, cursors, totals |
| `ProjectConnection` | Paginated projects with edges |
| `ArticleConnection` | Paginated articles with edges |
| `ProjectEdge` | node + cursor |
| `ArticleEdge` | node + cursor |

### Enums

| Enum | Values |
|------|--------|
| `SkillCategory` | FRONTEND, BACKEND, DATABASE, DEVOPS, TOOLS |
| `ExperienceType` | WORK, EDUCATION, CERTIFICATION, VOLUNTEERING |
| `IconType` | FONT_AWESOME, CUSTOM_SVG |
| `ImageStatus` | PROCESSING, READY, FAILED |
| `SortDirection` | ASC, DESC |

---

## Queries

### Projects

```graphql
# Paginated projects with filtering
query {
  projects(
    filter: { featured: true, tagIds: [1, 2] }
    sort: { field: CREATED_AT, direction: DESC }
    page: { page: 0, size: 10 }
  ) {
    edges {
      node {
        id
        title
        description
        tags { name }
      }
      cursor
    }
    pageInfo {
      hasNextPage
      totalElements
    }
  }
}

# Single project
query {
  project(id: 1) {
    id
    title
    description
    images { imageUrl }
  }
}

# Featured projects
query {
  featuredProjects {
    id
    title
  }
}
```

### Articles

```graphql
# Paginated articles
query {
  articles(
    filter: { tagIds: [1] }
    sort: { field: PUBLISHED_AT, direction: DESC }
    page: { page: 0, size: 10 }
  ) {
    edges {
      node {
        id
        title
        slug
        excerpt
        publishedAt
      }
    }
    pageInfo {
      totalElements
    }
  }
}

# Article by slug
query {
  articleBySlug(slug: "my-article") {
    id
    title
    contentHtml
    tags { name }
  }
}
```

### Skills

```graphql
# All skills
query {
  skills {
    id
    name
    category
  }
}

# Skills grouped by category
query {
  skillsByCategory {
    category
    categoryDisplayName
    skills {
      id
      name
      icon
    }
  }
}
```

### Experiences

```graphql
# Filtered experiences
query {
  experiences(filter: { type: WORK }) {
    id
    company
    role
    startDate
    endDate
  }
}

# Experiences grouped by type
query {
  experiencesByType {
    type
    experiences {
      id
      company
      role
    }
  }
}

# Recent experiences
query {
  recentExperiences(limit: 3) {
    id
    company
    role
  }
}
```

### Other Queries

```graphql
# Tags
query { tags { id name color } }

# Site configuration
query { siteConfiguration { fullName email heroTitle } }

# Current CV
query { currentCv { id fileName fileUrl } }

# Search
query { search(query: "spring") { projects { title } articles { title } totalCount } }
```

---

## Mutations

### Projects (Admin)

```graphql
mutation {
  createProject(input: {
    title: "New Project"
    description: "Description"
    techStack: "Angular, Spring"
    tagIds: [1, 2]
  }) {
    id
    title
  }
}

mutation {
  updateProject(id: 1, input: {
    title: "Updated Title"
    featured: true
  }) {
    id
    title
    featured
  }
}

mutation {
  deleteProject(id: 1)
}
```

### Articles (Admin)

```graphql
mutation {
  createArticle(input: {
    title: "New Article"
    content: "# Markdown content..."
    excerpt: "Summary"
    draft: true
    tagIds: [1]
  }) {
    id
    title
    slug
  }
}

mutation { publishArticle(id: 1) { id draft publishedAt } }
mutation { unpublishArticle(id: 1) { id draft } }
```

### Contact (Public)

```graphql
mutation {
  sendContactMessage(input: {
    name: "John Doe"
    email: "john@example.com"
    subject: "Inquiry"
    message: "Hello..."
  }) {
    success
    message
  }
}
```

---

## Configuration

### Application Properties

```yaml
spring:
  graphql:
    graphiql:
      enabled: ${GRAPHQL_GRAPHIQL_ENABLED:true}
      path: /graphiql
    schema:
      locations: classpath:graphql/
      printer:
        enabled: ${GRAPHQL_SCHEMA_PRINTER_ENABLED:true}
      introspection:
        enabled: ${GRAPHQL_INTROSPECTION_ENABLED:true}
```

### Environment Configuration

| Setting | Dev | Staging | Prod |
|---------|-----|---------|------|
| `graphiql.enabled` | `true` | `false` | `false` |
| `schema.printer.enabled` | `true` | `false` | `false` |
| `schema.introspection.enabled` | `true` | `false` | `false` |

### Environment Variables

| Variable | Description | Default (Dev) | Default (Prod/Staging) |
|----------|-------------|---------------|------------------------|
| `GRAPHQL_GRAPHIQL_ENABLED` | Enable GraphiQL interactive UI | `true` | `false` |
| `GRAPHQL_SCHEMA_PRINTER_ENABLED` | Enable schema printer endpoint | `true` | `false` |
| `GRAPHQL_INTROSPECTION_ENABLED` | Enable schema introspection queries | `true` | `false` |

### Security Notes

- **Introspection** exposes the full API schema - disabled in production by default
- **GraphiQL** provides an interactive query interface - disabled in production
- **Schema Printer** outputs the SDL schema - disabled in production
- To temporarily enable in production, set the environment variable to `true`

### GraphiQL UI

Available at `/graphiql` when enabled (development only). Provides:
- Interactive query builder
- Schema documentation
- Query history
- Variable editor

---

## Related Documentation

- [Search API](./search.md) - REST search endpoints
- [Authentication](../security/authentication.md) - JWT authentication
