# GraphQL API

GraphQL endpoint as an alternative to REST API for flexible data querying.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Endpoint](#2-endpoint)
3. [Schema](#3-schema)
4. [Queries](#4-queries)
5. [Mutations](#5-mutations)
6. [Configuration](#6-configuration)

---

## 1. Overview

The Portfolio application provides a GraphQL API alongside the REST API for flexible data querying.

**Key Capabilities**:
- Single endpoint for all operations
- Client-specified response fields
- Reduced over-fetching
- Type-safe schema
- Introspection support
- GraphiQL playground (development)

**Endpoint**: `/graphql`

---

## 2. Endpoint

### 2.1 URL

| Environment | URL |
|-------------|-----|
| Local | http://localhost:8080/graphql |
| Staging | https://staging-api.example.com/graphql |
| Production | https://api.example.com/graphql |

### 2.2 GraphiQL Playground

**URL**: `/graphiql` (development only)

Provides:
- Interactive query editor
- Schema documentation
- Query history
- Variables panel

### 2.3 Request Format

```http
POST /graphql
Content-Type: application/json

{
  "query": "query { projects { id title } }",
  "variables": {},
  "operationName": "GetProjects"
}
```

---

## 3. Schema

### 3.1 Types

**Project**:
```graphql
type Project {
  id: ID!
  title: String!
  slug: String!
  description: String
  shortDescription: String
  technologies: [String!]!
  githubUrl: String
  demoUrl: String
  imageUrl: String
  thumbnailUrl: String
  featured: Boolean!
  displayOrder: Int
  startDate: String
  endDate: String
  tags: [Tag!]!
  createdAt: String!
  updatedAt: String!
}
```

**Article**:
```graphql
type Article {
  id: ID!
  title: String!
  slug: String!
  content: String!
  htmlContent: String!
  published: Boolean!
  publishedAt: String
  readingTime: Int
  tags: [Tag!]!
  createdAt: String!
  updatedAt: String!
}
```

**Experience**:
```graphql
type Experience {
  id: ID!
  title: String!
  organization: String!
  location: String
  description: String
  type: ExperienceType!
  startDate: String!
  endDate: String
  ongoing: Boolean!
  createdAt: String!
}

enum ExperienceType {
  WORK
  EDUCATION
  CERTIFICATION
  VOLUNTEERING
}
```

**Tag**:
```graphql
type Tag {
  id: ID!
  name: String!
  slug: String!
}
```

**Skill**:
```graphql
type Skill {
  id: ID!
  name: String!
  category: String!
  proficiency: Int!
  displayOrder: Int
}
```

---

## 4. Queries

### 4.1 Projects

```graphql
# Get all projects
query {
  projects {
    id
    title
    description
    tags { name }
  }
}

# Get featured projects
query {
  featuredProjects {
    id
    title
    imageUrl
  }
}

# Get project by ID
query {
  project(id: 1) {
    id
    title
    description
    technologies
    githubUrl
    demoUrl
  }
}

# Get project by slug
query {
  projectBySlug(slug: "my-project") {
    id
    title
    description
  }
}
```

### 4.2 Articles

```graphql
# Get published articles
query {
  articles {
    id
    title
    slug
    readingTime
    publishedAt
  }
}

# Get article by slug
query {
  articleBySlug(slug: "my-article") {
    id
    title
    htmlContent
    tags { name }
  }
}
```

### 4.3 Experiences

```graphql
# Get all experiences
query {
  experiences {
    id
    title
    organization
    type
    startDate
    endDate
    ongoing
  }
}

# Get experiences by type
query {
  experiencesByType(type: WORK) {
    id
    title
    organization
    description
  }
}
```

### 4.4 Skills

```graphql
# Get all skills
query {
  skills {
    id
    name
    category
    proficiency
  }
}

# Get skills by category
query {
  skillsByCategory(category: "Backend") {
    id
    name
    proficiency
  }
}
```

### 4.5 Tags

```graphql
# Get all tags
query {
  tags {
    id
    name
    slug
  }
}
```

---

## 5. Mutations

### 5.1 Admin Mutations (Requires Authentication)

**Projects**:
```graphql
mutation {
  createProject(input: {
    title: "New Project"
    description: "Description"
    technologies: ["Java", "Spring"]
  }) {
    id
    title
  }
}

mutation {
  updateProject(id: 1, input: {
    title: "Updated Title"
  }) {
    id
    title
  }
}

mutation {
  deleteProject(id: 1)
}
```

**Articles**:
```graphql
mutation {
  createArticle(input: {
    title: "New Article"
    content: "# Markdown content"
  }) {
    id
    title
    slug
  }
}

mutation {
  publishArticle(id: 1) {
    id
    published
    publishedAt
  }
}
```

### 5.2 Authentication

Admin mutations require JWT token:

```http
POST /graphql
Authorization: Bearer <access_token>
Content-Type: application/json
```

---

## 6. Configuration

### 6.1 Dependencies

```gradle
implementation 'org.springframework.boot:spring-boot-starter-graphql'
```

### 6.2 Application Properties

```yaml
spring:
  graphql:
    graphiql:
      enabled: true  # Disable in production
    schema:
      locations: classpath:graphql/
      printer:
        enabled: true
    cors:
      allowed-origins: "*"
```

### 6.3 Schema Location

Schema files in `src/main/resources/graphql/`:
- `schema.graphqls` - Main schema
- `types/project.graphqls` - Project types
- `types/article.graphqls` - Article types

---

## 7. REST vs GraphQL

### 7.1 Comparison

| Aspect | REST | GraphQL |
|--------|------|---------|
| Endpoints | Multiple | Single |
| Response size | Fixed | Client-defined |
| Over-fetching | Common | Avoided |
| Under-fetching | Requires multiple calls | Single query |
| Caching | HTTP caching | Client-side |
| File uploads | Native | Requires multipart |

### 7.2 When to Use GraphQL

**Use GraphQL for**:
- Complex nested queries
- Client-specified fields
- Aggregating multiple resources
- Mobile apps (bandwidth optimization)

**Use REST for**:
- Simple CRUD operations
- File uploads
- Webhooks
- Server-side caching

---

## Related Documentation

- [API: REST Overview](../api/README.md) - REST API documentation
- [Architecture: Backend](../architecture/README.md) - Backend architecture
- [Security: Authentication](../security/authentication.md) - JWT authentication
