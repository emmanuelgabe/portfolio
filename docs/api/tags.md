# Tags API

---

## Table of Contents
1. [Overview](#1-overview)
2. [Endpoints](#2-endpoints)
   - 2.1 [List All Tags](#21-list-all-tags)
   - 2.2 [Get Tag by ID](#22-get-tag-by-id)
   - 2.3 [Create Tag](#23-create-tag)
   - 2.4 [Update Tag](#24-update-tag)
   - 2.5 [Delete Tag](#25-delete-tag)
3. [Data Models](#3-data-models)
4. [Business Rules](#4-business-rules)
5. [Error Handling](#5-error-handling)

---

## 1. Overview

The Tags API provides endpoints for managing tags used to categorize articles and projects. Tags enable content organization and filtering across the portfolio.

**Base Paths**:
- Public: `/api/tags`
- Admin: `/api/tags/admin`

**Features**:
- Full CRUD operations for tags
- Unique tag names (case-sensitive)
- Hex color code validation
- Many-to-many relationships with articles and projects
- Public read access, admin-only write access

---

## 2. Endpoints

### 2.1 List All Tags

Retrieve all tags (public endpoint).

**Endpoint**: `GET /api/tags`

**Authentication**: None (public)

**Query Parameters**: None

**Success Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Angular",
    "color": "#DD0031"
  },
  {
    "id": 2,
    "name": "Spring Boot",
    "color": "#6DB33F"
  },
  {
    "id": 3,
    "name": "Java",
    "color": "#007396"
  },
  {
    "id": 4,
    "name": "TypeScript",
    "color": "#3178C6"
  }
]
```

**Example Request (curl)**:
```bash
curl -X GET http://localhost:8080/api/tags
```

**Example Request (JavaScript)**:
```javascript
const response = await fetch('http://localhost:8080/api/tags');
const tags = await response.json();
```

---

### 2.2 Get Tag by ID

Retrieve a single tag by its ID (public endpoint).

**Endpoint**: `GET /api/tags/{id}`

**Authentication**: None (public)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Tag ID |

**Success Response** (200 OK):
```json
{
  "id": 1,
  "name": "Angular",
  "color": "#DD0031"
}
```

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Tag not found with id: 999",
  "path": "/api/tags/999"
}
```

**Example Request (curl)**:
```bash
curl -X GET http://localhost:8080/api/tags/1
```

---

### 2.3 Create Tag

Create a new tag (admin only).

**Endpoint**: `POST /api/tags/admin`

**Authentication**: Required (ROLE_ADMIN)

**Request Body**:
```json
{
  "name": "React",
  "color": "#61DAFB"
}
```

**Request Schema**:
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | Yes | 2-50 chars, unique | Tag name (case-sensitive) |
| `color` | string | Yes | Hex color code | Color in format #RGB or #RRGGBB |

**Color Format**:
- Must start with `#`
- Supports 3-digit shorthand: `#RGB` (e.g., `#F00`)
- Supports 6-digit full format: `#RRGGBB` (e.g., `#FF0000`)
- Accepts uppercase and lowercase hex digits (A-F, a-f)

**Success Response** (201 Created):
```json
{
  "id": 5,
  "name": "React",
  "color": "#61DAFB"
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
      "message": "Tag name is required"
    },
    {
      "field": "color",
      "rejectedValue": "red",
      "message": "Color must be a valid hex color code (e.g., #FF5733)"
    }
  ],
  "path": "/api/tags/admin"
}
```

**Duplicate Tag Name (409 Conflict)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 409,
  "error": "Conflict",
  "message": "Tag with name 'Angular' already exists",
  "path": "/api/tags/admin"
}
```

**Unauthorized (401)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/tags/admin"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/tags/admin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "name": "React",
    "color": "#61DAFB"
  }'
```

---

### 2.4 Update Tag

Update an existing tag (admin only).

**Endpoint**: `PUT /api/tags/admin/{id}`

**Authentication**: Required (ROLE_ADMIN)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Tag ID to update |

**Request Body**:
```json
{
  "name": "React.js",
  "color": "#61DAFB"
}
```

**Request Schema**: Same as [Create Tag](#23-create-tag), but all fields are optional (partial updates supported).

**Success Response** (200 OK):
```json
{
  "id": 5,
  "name": "React.js",
  "color": "#61DAFB"
}
```

**Error Responses**:

**Tag Not Found (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Tag not found with id: 999",
  "path": "/api/tags/admin/999"
}
```

**Duplicate Tag Name (409 Conflict)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 409,
  "error": "Conflict",
  "message": "Tag with name 'Angular' already exists",
  "path": "/api/tags/admin/5"
}
```

**Example Request (curl)**:
```bash
curl -X PUT http://localhost:8080/api/tags/admin/5 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "name": "React.js"
  }'
```

---

### 2.5 Delete Tag

Delete a tag (admin only).

**Endpoint**: `DELETE /api/tags/admin/{id}`

**Authentication**: Required (ROLE_ADMIN)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Tag ID to delete |

**Success Response** (204 No Content):
No response body.

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Tag not found with id: 999",
  "path": "/api/tags/admin/999"
}
```

**Note**: Deleting a tag:
- Removes the tag from all associated articles and projects
- Does NOT delete the articles or projects themselves
- Updates the many-to-many relationship tables automatically

**Example Request (curl)**:
```bash
curl -X DELETE http://localhost:8080/api/tags/admin/5 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## 3. Data Models

### TagResponse

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | integer | No | Tag ID |
| `name` | string | No | Tag name (2-50 chars, unique) |
| `color` | string | No | Hex color code (#RGB or #RRGGBB) |

**Example TagResponse**:
```json
{
  "id": 1,
  "name": "Angular",
  "color": "#DD0031"
}
```

---

## 4. Business Rules

### Tag Creation

1. **Unique Names**: Tag names must be unique (case-sensitive)
   - "Angular" and "angular" are considered different tags
   - Duplicate check enforced at database level (unique constraint)

2. **Color Validation**: Colors must be valid hex codes
   - Valid: `#FF0000`, `#F00`, `#61DAFB`, `#6DB33F`
   - Invalid: `red`, `#GGGGGG`, `FF0000` (missing #), `#12345` (wrong length)

3. **Name Length**: Tag names must be meaningful (2-50 characters)
   - Minimum 2 characters prevents single-letter tags
   - Maximum 50 characters ensures UI compatibility

### Tag Relationships

1. **Many-to-Many with Projects**: A tag can be associated with multiple projects, and a project can have multiple tags
2. **Many-to-Many with Articles**: A tag can be associated with multiple articles, and an article can have multiple tags
3. **Cascade Delete**: Deleting a tag removes the associations but not the related entities

### Partial Updates

1. **UpdateTagRequest**: All fields are optional
2. **Null Handling**: Only provided fields are updated (null values ignored using MapStruct's NullValuePropertyMappingStrategy.IGNORE)
3. **Uniqueness Check**: When updating name, must still be unique (excluding the current tag)

---

## 5. Error Handling

### Common Error Scenarios

| Scenario | HTTP Status | Error Message |
|----------|-------------|---------------|
| Tag not found | 404 | "Tag not found with id: {id}" |
| Missing required field | 400 | Field-specific validation message |
| Duplicate tag name | 409 | "Tag with name '{name}' already exists" |
| Invalid hex color | 400 | "Color must be a valid hex color code (e.g., #FF5733)" |
| Name too short | 400 | "Tag name must be between 2 and 50 characters" |
| Name too long | 400 | "Tag name must be between 2 and 50 characters" |
| Missing Authorization | 401 | "Missing or invalid Authorization header" |
| Insufficient permissions | 403 | "Access denied: insufficient permissions" |

### Hex Color Validation

**Valid hex color formats**:
```
#RGB     - 3-digit shorthand (e.g., #F00 for red)
#RRGGBB  - 6-digit full format (e.g., #FF0000 for red)
```

**Examples of valid colors**:
- `#F00` → Red (shorthand)
- `#FF0000` → Red (full)
- `#61DAFB` → Light blue
- `#6DB33F` → Green
- `#DD0031` → Red

**Examples of invalid colors**:
- `red` → Not hex format
- `#GGGGGG` → Invalid hex digits
- `FF0000` → Missing # prefix
- `#12345` → Wrong length (5 digits)
- `#1234567` → Wrong length (7 digits)

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
      "rejectedValue": "A",
      "message": "Tag name must be between 2 and 50 characters"
    },
    {
      "field": "color",
      "rejectedValue": "blue",
      "message": "Color must be a valid hex color code (e.g., #FF5733)"
    }
  ],
  "path": "/api/tags/admin"
}
```

### Handling Duplicate Tag Names

When attempting to create or update a tag with a name that already exists:

**Error Response (409 Conflict)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 409,
  "error": "Conflict",
  "message": "Tag with name 'Angular' already exists",
  "path": "/api/tags/admin"
}
```

**Important Notes**:
- Tag names are case-sensitive ("Angular" != "angular")
- Unique constraint enforced at database level
- Check for existing tags before creation to provide better UX

---

## Related Documentation

- [API Overview](./README.md) - General API documentation
- [Authentication API](./authentication.md) - JWT authentication
- [Projects API](./projects.md) - Projects using tags
- [Articles API](./articles-api.md) - Articles using tags
- [Security: RBAC](../security/rbac.md) - Role-based access control
