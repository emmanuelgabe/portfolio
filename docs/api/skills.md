# Skills API

---

## Table of Contents
1. [Overview](#1-overview)
2. [Endpoints](#2-endpoints)
   - 2.1 [List Skills](#21-list-skills)
   - 2.2 [Get Skill by ID](#22-get-skill-by-id)
   - 2.3 [Create Skill](#23-create-skill)
   - 2.4 [Update Skill](#24-update-skill)
   - 2.5 [Delete Skill](#25-delete-skill)
3. [Data Models](#3-data-models)
4. [Skill Categories](#4-skill-categories)
5. [Error Handling](#5-error-handling)

---

## 1. Overview

The Skills API provides endpoints for managing technical skills with categorization.

**Base Paths**:
- Public: `/api/skills`
- Admin: `/api/admin/skills`

**Features**:
- Full CRUD operations for skills
- Categorization system (PROGRAMMING, FRAMEWORK, DATABASE, etc.)
- Public and admin access levels
- Predefined skill categories

---

## 2. Endpoints

### 2.1 List Skills

Retrieve all skills (public endpoint).

**Endpoint**: `GET /api/skills`

**Authentication**: None (public)

**Query Parameters**: None

**Success Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Java",
    "category": "PROGRAMMING",
    "createdAt": "2025-11-10T10:00:00",
    "updatedAt": "2025-11-10T10:00:00"
  },
  {
    "id": 2,
    "name": "Spring Boot",
    "category": "FRAMEWORK",
    "createdAt": "2025-11-10T10:05:00",
    "updatedAt": "2025-11-10T10:05:00"
  },
  {
    "id": 3,
    "name": "PostgreSQL",
    "category": "DATABASE",
    "createdAt": "2025-11-10T10:10:00",
    "updatedAt": "2025-11-10T10:10:00"
  },
  {
    "id": 4,
    "name": "Docker",
    "category": "TOOL",
    "createdAt": "2025-11-10T10:15:00",
    "updatedAt": "2025-11-10T10:15:00"
  }
]
```

**Note**: Skills are typically grouped by category in the frontend.

**Example Request (curl)**:
```bash
curl -X GET http://localhost:8080/api/skills
```

**Example Request (JavaScript)**:
```javascript
const response = await fetch('http://localhost:8080/api/skills');
const skills = await response.json();

// Group by category
const groupedSkills = skills.reduce((acc, skill) => {
  if (!acc[skill.category]) acc[skill.category] = [];
  acc[skill.category].push(skill);
  return acc;
}, {});
```

---

### 2.2 Get Skill by ID

Retrieve a single skill by its ID (public endpoint).

**Endpoint**: `GET /api/skills/{id}`

**Authentication**: None (public)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Skill ID |

**Success Response** (200 OK):
```json
{
  "id": 1,
  "name": "Java",
  "category": "PROGRAMMING",
  "createdAt": "2025-11-10T10:00:00",
  "updatedAt": "2025-11-10T10:00:00"
}
```

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Skill not found with id: 999",
  "path": "/api/skills/999"
}
```

**Example Request (curl)**:
```bash
curl -X GET http://localhost:8080/api/skills/1
```

---

### 2.3 Create Skill

Create a new skill (admin only).

**Endpoint**: `POST /api/admin/skills`

**Authentication**: Required (ROLE_ADMIN)

**Request Body**:
```json
{
  "name": "TypeScript",
  "category": "PROGRAMMING"
}
```

**Request Schema**:
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | Yes | 1-100 chars, not blank | Skill name |
| `category` | string | Yes | Valid category enum | Skill category |

**Valid Categories**: See [Skill Categories](#4-skill-categories) section.

**Success Response** (201 Created):
```json
{
  "id": 15,
  "name": "TypeScript",
  "category": "PROGRAMMING",
  "createdAt": "2025-11-19T15:30:00",
  "updatedAt": "2025-11-19T15:30:00"
}
```

**Error Responses**:

**Validation Error (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "name",
      "rejectedValue": "",
      "message": "must not be blank"
    }
  ],
  "path": "/api/admin/skills"
}
```

**Invalid Category (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid category: INVALID_CATEGORY. Valid categories: PROGRAMMING, FRAMEWORK, DATABASE, TOOL, LANGUAGE, CLOUD, DEVOPS, METHODOLOGY, TESTING",
  "path": "/api/admin/skills"
}
```

**Unauthorized (401)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/admin/skills"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/admin/skills \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "name": "TypeScript",
    "category": "PROGRAMMING"
  }'
```

---

### 2.4 Update Skill

Update an existing skill (admin only).

**Endpoint**: `PUT /api/admin/skills/{id}`

**Authentication**: Required (ROLE_ADMIN)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Skill ID to update |

**Request Body**:
```json
{
  "name": "TypeScript 5",
  "category": "PROGRAMMING"
}
```

**Request Schema**: Same as [Create Skill](#23-create-skill)

**Success Response** (200 OK):
```json
{
  "id": 15,
  "name": "TypeScript 5",
  "category": "PROGRAMMING",
  "createdAt": "2025-11-19T15:30:00",
  "updatedAt": "2025-11-19T16:45:00"
}
```

**Error Responses**: Same as [Create Skill](#23-create-skill), plus:

**Skill Not Found (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Skill not found with id: 999",
  "path": "/api/admin/skills/999"
}
```

**Example Request (curl)**:
```bash
curl -X PUT http://localhost:8080/api/admin/skills/15 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "name": "TypeScript 5",
    "category": "PROGRAMMING"
  }'
```

---

### 2.5 Delete Skill

Delete a skill (admin only).

**Endpoint**: `DELETE /api/admin/skills/{id}`

**Authentication**: Required (ROLE_ADMIN)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Skill ID to delete |

**Success Response** (200 OK):
```json
{
  "message": "Skill deleted successfully"
}
```

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Skill not found with id: 999",
  "path": "/api/admin/skills/999"
}
```

**Note**: Deleting a skill does NOT affect projects or other entities. Skills are standalone entities.

**Example Request (curl)**:
```bash
curl -X DELETE http://localhost:8080/api/admin/skills/15 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## 3. Data Models

### SkillResponse

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | integer | No | Skill ID |
| `name` | string | No | Skill name (max 100 chars) |
| `category` | string | No | Skill category (enum) |
| `createdAt` | datetime | No | Creation timestamp |
| `updatedAt` | datetime | No | Last update timestamp |

### CreateSkillRequest / UpdateSkillRequest

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | Yes | 1-100 chars, not blank | Skill name |
| `category` | string | Yes | Valid category enum | Skill category |

---

## 4. Skill Categories

The system supports the following predefined categories:

| Category | Description | Examples |
|----------|-------------|----------|
| `PROGRAMMING` | Programming languages | Java, Python, TypeScript, JavaScript, C++, Go |
| `FRAMEWORK` | Frameworks and libraries | Spring Boot, Angular, React, Vue.js, Django |
| `DATABASE` | Database systems | PostgreSQL, MySQL, MongoDB, Redis, Oracle |
| `TOOL` | Development tools | Docker, Git, Maven, Gradle, npm, IntelliJ |
| `LANGUAGE` | Human languages | English, French, Spanish, German |
| `CLOUD` | Cloud platforms | AWS, Azure, Google Cloud, Heroku |
| `DEVOPS` | DevOps tools and practices | Kubernetes, Jenkins, GitLab CI, Ansible |
| `METHODOLOGY` | Development methodologies | Agile, Scrum, TDD, DDD, Clean Architecture |
| `TESTING` | Testing tools and frameworks | JUnit, Jasmine, Karma, Selenium, Postman |

**Example Category Usage**:
```json
{
  "name": "Spring Boot",
  "category": "FRAMEWORK"
}
```

**Invalid Category Example**:
```json
{
  "name": "Invalid Skill",
  "category": "INVALID_CATEGORY"
}
```
Response: `400 Bad Request` with error message listing valid categories.

---

## 5. Error Handling

### Common Error Scenarios

| Scenario | HTTP Status | Error Message |
|----------|-------------|---------------|
| Skill not found | 404 | "Skill not found with id: {id}" |
| Missing required field | 400 | Validation error with field details |
| Invalid category | 400 | "Invalid category: {category}. Valid categories: ..." |
| Name too long | 400 | "size must be between 0 and 100" |
| Missing Authorization | 401 | "Missing or invalid Authorization header" |
| Insufficient permissions | 403 | "Access denied: insufficient permissions" |

### Handling Validation Errors

**Example Validation Error Response**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "name",
      "rejectedValue": "",
      "message": "must not be blank"
    },
    {
      "field": "category",
      "rejectedValue": null,
      "message": "must not be null"
    }
  ],
  "path": "/api/admin/skills"
}
```

### Frontend Category Validation

**Recommended Approach**:
```javascript
const VALID_CATEGORIES = [
  'PROGRAMMING',
  'FRAMEWORK',
  'DATABASE',
  'TOOL',
  'LANGUAGE',
  'CLOUD',
  'DEVOPS',
  'METHODOLOGY',
  'TESTING'
];

function validateSkillCategory(category) {
  return VALID_CATEGORIES.includes(category);
}

// Usage
const skill = {
  name: 'Spring Boot',
  category: 'FRAMEWORK'
};

if (!validateSkillCategory(skill.category)) {
  throw new Error(`Invalid category: ${skill.category}`);
}
```

---

## Related Documentation

- [API Overview](./README.md) - General API documentation
- [Authentication API](./authentication.md) - JWT authentication
- [Projects API](./projects.md) - Projects management
- [Security: RBAC](../security/rbac.md) - Role-based access control
