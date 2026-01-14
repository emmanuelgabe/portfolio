# Projects API

---

## Table of Contents
1. [Overview](#1-overview)
2. [Endpoints](#2-endpoints)
   - 2.1 [List Projects](#21-list-projects)
   - 2.2 [Get Project by ID](#22-get-project-by-id)
   - 2.3 [Create Project](#23-create-project)
   - 2.4 [Update Project](#24-update-project)
   - 2.5 [Delete Project](#25-delete-project)
   - 2.6 [Upload Project Image](#26-upload-project-image)
   - 2.7 [Delete Project Image](#27-delete-project-image)
   - 2.8 [Reorder Projects](#28-reorder-projects)
3. [Data Models](#3-data-models)
4. [Business Rules](#4-business-rules)
5. [Error Handling](#5-error-handling)

---

## 1. Overview

The Projects API provides endpoints for managing portfolio projects, including CRUD operations and image management.

**Base Paths**:
- Public: `/api/projects`
- Admin: `/api/admin/projects`

**Features**:
- Full CRUD operations for projects
- Image upload with WebP optimization
- Automatic thumbnail generation (400x300px)
- Many-to-many relationship with tags
- Featured project flag
- Public and admin access levels

---

## 2. Endpoints

### 2.1 List Projects

Retrieve all projects (public endpoint).

**Endpoint**: `GET /api/projects`

**Authentication**: None (public)

**Query Parameters**: None

**Success Response** (200 OK):
```json
[
  {
    "id": 1,
    "title": "Portfolio Website",
    "description": "Full-stack portfolio application with Angular and Spring Boot",
    "shortDescription": "Personal portfolio with admin dashboard",
    "featured": true,
    "imageUrl": "http://localhost:8080/uploads/projects/project_1_1700000000.webp",
    "thumbnailUrl": "http://localhost:8080/uploads/projects/project_1_1700000000_thumb.webp",
    "githubUrl": "https://github.com/user/portfolio",
    "liveUrl": "https://portfolio.example.com",
    "tags": [
      {
        "id": 1,
        "name": "Angular",
        "color": "#DD0031"
      },
      {
        "id": 2,
        "name": "Spring Boot",
        "color": "#6DB33F"
      }
    ],
    "createdAt": "2025-11-15T10:30:00",
    "updatedAt": "2025-11-19T14:20:00"
  },
  {
    "id": 2,
    "title": "E-Commerce Platform",
    "description": "Online shopping platform with payment integration",
    "shortDescription": "Full-featured e-commerce solution",
    "featured": false,
    "imageUrl": null,
    "thumbnailUrl": null,
    "githubUrl": "https://github.com/user/ecommerce",
    "liveUrl": null,
    "tags": [
      {
        "id": 3,
        "name": "React",
        "color": "#61DAFB"
      }
    ],
    "createdAt": "2025-11-10T08:15:00",
    "updatedAt": "2025-11-10T08:15:00"
  }
]
```

**Example Request (curl)**:
```bash
curl -X GET http://localhost:8080/api/projects
```

**Example Request (JavaScript)**:
```javascript
const response = await fetch('http://localhost:8080/api/projects');
const projects = await response.json();
```

---

### 2.2 Get Project by ID

Retrieve a single project by its ID (public endpoint).

**Endpoint**: `GET /api/projects/{id}`

**Authentication**: None (public)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Project ID |

**Success Response** (200 OK):
```json
{
  "id": 1,
  "title": "Portfolio Website",
  "description": "Full-stack portfolio application with Angular and Spring Boot. Features include admin dashboard, JWT authentication, CV management, and project showcase.",
  "shortDescription": "Personal portfolio with admin dashboard",
  "featured": true,
  "imageUrl": "http://localhost:8080/uploads/projects/project_1_1700000000.webp",
  "thumbnailUrl": "http://localhost:8080/uploads/projects/project_1_1700000000_thumb.webp",
  "githubUrl": "https://github.com/user/portfolio",
  "liveUrl": "https://portfolio.example.com",
  "tags": [
    {
      "id": 1,
      "name": "Angular",
      "color": "#DD0031"
    },
    {
      "id": 2,
      "name": "Spring Boot",
      "color": "#6DB33F"
    }
  ],
  "createdAt": "2025-11-15T10:30:00",
  "updatedAt": "2025-11-19T14:20:00"
}
```

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Project not found with id: 999",
  "path": "/api/projects/999"
}
```

**Example Request (curl)**:
```bash
curl -X GET http://localhost:8080/api/projects/1
```

---

### 2.3 Create Project

Create a new project (admin only).

**Endpoint**: `POST /api/admin/projects`

**Authentication**: Required (ROLE_ADMIN)

**Request Body**:
```json
{
  "title": "New Project",
  "description": "Detailed description of the project",
  "shortDescription": "Brief summary",
  "featured": false,
  "githubUrl": "https://github.com/user/new-project",
  "liveUrl": "https://new-project.example.com",
  "tagIds": [1, 2, 3]
}
```

**Request Schema**:
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `title` | string | Yes | 1-200 chars | Project title |
| `description` | string | Yes | Not blank | Full description |
| `shortDescription` | string | No | Max 500 chars | Brief summary |
| `featured` | boolean | No | Default: false | Featured flag |
| `githubUrl` | string | No | Valid URL | GitHub repository |
| `liveUrl` | string | No | Valid URL | Live demo URL |
| `tagIds` | array | No | Valid tag IDs | Associated tags |

**Success Response** (201 Created):
```json
{
  "id": 3,
  "title": "New Project",
  "description": "Detailed description of the project",
  "shortDescription": "Brief summary",
  "featured": false,
  "imageUrl": null,
  "thumbnailUrl": null,
  "githubUrl": "https://github.com/user/new-project",
  "liveUrl": "https://new-project.example.com",
  "tags": [
    {
      "id": 1,
      "name": "Angular",
      "color": "#DD0031"
    }
  ],
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
      "field": "title",
      "rejectedValue": "",
      "message": "must not be blank"
    }
  ],
  "path": "/api/admin/projects"
}
```

**Tag Not Found (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Tag not found with id: 999",
  "path": "/api/admin/projects"
}
```

**Unauthorized (401)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/admin/projects"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/admin/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "title": "New Project",
    "description": "Detailed description",
    "shortDescription": "Brief summary",
    "featured": false,
    "githubUrl": "https://github.com/user/new-project",
    "tagIds": [1, 2]
  }'
```

---

### 2.4 Update Project

Update an existing project (admin only).

**Endpoint**: `PUT /api/admin/projects/{id}`

**Authentication**: Required (ROLE_ADMIN)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Project ID to update |

**Request Body**:
```json
{
  "title": "Updated Project Title",
  "description": "Updated detailed description",
  "shortDescription": "Updated summary",
  "featured": true,
  "githubUrl": "https://github.com/user/updated-project",
  "liveUrl": "https://updated-project.example.com",
  "tagIds": [1, 3]
}
```

**Request Schema**: Same as [Create Project](#23-create-project)

**Success Response** (200 OK):
```json
{
  "id": 1,
  "title": "Updated Project Title",
  "description": "Updated detailed description",
  "shortDescription": "Updated summary",
  "featured": true,
  "imageUrl": "http://localhost:8080/uploads/projects/project_1_1700000000.webp",
  "thumbnailUrl": "http://localhost:8080/uploads/projects/project_1_1700000000_thumb.webp",
  "githubUrl": "https://github.com/user/updated-project",
  "liveUrl": "https://updated-project.example.com",
  "tags": [
    {
      "id": 1,
      "name": "Angular",
      "color": "#DD0031"
    },
    {
      "id": 3,
      "name": "React",
      "color": "#61DAFB"
    }
  ],
  "createdAt": "2025-11-15T10:30:00",
  "updatedAt": "2025-11-19T16:45:00"
}
```

**Error Responses**: Same as [Create Project](#23-create-project), plus:

**Project Not Found (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Project not found with id: 999",
  "path": "/api/admin/projects/999"
}
```

**Example Request (curl)**:
```bash
curl -X PUT http://localhost:8080/api/admin/projects/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "title": "Updated Project Title",
    "description": "Updated description",
    "featured": true,
    "tagIds": [1, 3]
  }'
```

---

### 2.5 Delete Project

Delete a project (admin only).

**Endpoint**: `DELETE /api/admin/projects/{id}`

**Authentication**: Required (ROLE_ADMIN)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Project ID to delete |

**Success Response** (200 OK):
```json
{
  "message": "Project deleted successfully"
}
```

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Project not found with id: 999",
  "path": "/api/admin/projects/999"
}
```

**Note**: Deleting a project also removes:
- Associated project images (main image and thumbnail)
- Project-tag relationships (tags themselves are NOT deleted)

**Example Request (curl)**:
```bash
curl -X DELETE http://localhost:8080/api/admin/projects/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 2.6 Upload Project Image

Upload an image for a project with automatic WebP conversion and thumbnail generation.

**Endpoint**: `POST /api/admin/projects/{id}/upload-image`

**Authentication**: Required (ROLE_ADMIN)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Project ID |

**Request**: `multipart/form-data`

**Form Data**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | file | Yes | Image file (JPEG, PNG, WebP) |

**File Constraints**:
- **Max Size**: 10 MB
- **Supported MIME Types**: `image/jpeg`, `image/png`, `image/webp`
- **Validation**: Magic bytes validation (not extension-based)

**Success Response** (200 OK):
```json
{
  "imageUrl": "http://localhost:8080/uploads/projects/project_1_1700000000.webp",
  "thumbnailUrl": "http://localhost:8080/uploads/projects/project_1_1700000000_thumb.webp",
  "message": "Image uploaded successfully"
}
```

**Processing Details**:
- Original image converted to **WebP** format (quality: 0.85)
- Thumbnail generated at **400x300px** (quality: 0.8)
- Old images automatically deleted
- Files named: `project_{id}_{timestamp}.webp`

**Error Responses**:

**Invalid File Type (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid file type. Supported types: JPEG, PNG, WebP",
  "path": "/api/admin/projects/1/upload-image"
}
```

**File Too Large (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "File size exceeds maximum allowed size (10 MB)",
  "path": "/api/admin/projects/1/upload-image"
}
```

**Project Not Found (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Project not found with id: 999",
  "path": "/api/admin/projects/999/upload-image"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/admin/projects/1/upload-image \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -F "file=@/path/to/project-image.jpg"
```

**Example Request (JavaScript)**:
```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

const response = await fetch('http://localhost:8080/api/admin/projects/1/upload-image', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`
  },
  body: formData
});

const data = await response.json();
console.log('Image URL:', data.imageUrl);
console.log('Thumbnail URL:', data.thumbnailUrl);
```

---

### 2.7 Delete Project Image

Delete a project's image and thumbnail.

**Endpoint**: `DELETE /api/admin/projects/{id}/delete-image`

**Authentication**: Required (ROLE_ADMIN)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Project ID |

**Success Response** (200 OK):
```json
{
  "message": "Image deleted successfully"
}
```

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Project not found with id: 999",
  "path": "/api/admin/projects/999/delete-image"
}
```

**Note**: This endpoint:
- Deletes both main image and thumbnail files from disk
- Sets `imageUrl` and `thumbnailUrl` to `null` in database
- Returns success even if no image exists

**Example Request (curl)**:
```bash
curl -X DELETE http://localhost:8080/api/admin/projects/1/delete-image \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 2.8 Reorder Projects

Reorder projects by providing an ordered list of IDs (admin only).

**Endpoint**: `PUT /api/admin/projects/reorder`

**Authentication**: Required (ROLE_ADMIN)

**Request Body**:
```json
{
  "orderedIds": [3, 1, 2, 5, 4]
}
```

**Request Schema**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderedIds` | array | Yes | Ordered list of project IDs |

**Success Response** (204 No Content): Empty body

**Error Responses**:

**Validation Error (400 Bad Request)**:
```json
{
  "timestamp": "2026-01-15T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "orderedIds",
      "message": "must not be null"
    }
  ],
  "path": "/api/admin/projects/reorder"
}
```

**Project Not Found (404 Not Found)**:
```json
{
  "timestamp": "2026-01-15T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Project not found with id: 999",
  "path": "/api/admin/projects/reorder"
}
```

**Example Request (curl)**:
```bash
curl -X PUT http://localhost:8080/api/admin/projects/reorder \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{"orderedIds": [3, 1, 2, 5, 4]}'
```

**Note**: This endpoint updates the `displayOrder` field for each project. The cache is automatically invalidated after reordering.

---

## 3. Data Models

### ProjectResponse

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | integer | No | Project ID |
| `title` | string | No | Project title (max 200 chars) |
| `description` | string | No | Full description |
| `shortDescription` | string | Yes | Brief summary (max 500 chars) |
| `featured` | boolean | No | Featured flag (default: false) |
| `imageUrl` | string | Yes | Full-size image URL |
| `thumbnailUrl` | string | Yes | Thumbnail image URL (400x300px) |
| `githubUrl` | string | Yes | GitHub repository URL |
| `liveUrl` | string | Yes | Live demo URL |
| `displayOrder` | integer | No | Display order (default: 0) |
| `tags` | array | No | Associated tags (can be empty) |
| `createdAt` | datetime | No | Creation timestamp |
| `updatedAt` | datetime | No | Last update timestamp |

### TagResponse

| Field | Type | Description |
|-------|------|-------------|
| `id` | integer | Tag ID |
| `name` | string | Tag name |
| `color` | string | Hex color code (e.g., "#DD0031") |

---

## 4. Business Rules

### Project Creation

1. **Title Uniqueness**: Not enforced (multiple projects can have the same title)
2. **Tags**: Tags must exist before associating with project
3. **Featured Flag**: Multiple projects can be featured
4. **URLs**: Optional but must be valid URLs if provided
5. **Images**: Projects can be created without images

### Image Management

1. **One Image per Project**: Uploading a new image deletes the old one
2. **WebP Conversion**: All images converted to WebP regardless of input format
3. **Automatic Thumbnails**: Thumbnail generated automatically on upload
4. **File Naming**: `project_{id}_{timestamp}.webp` ensures uniqueness
5. **Storage Location**: `uploads/projects/` directory

### Project Deletion

1. **Cascade**: Deleting a project deletes its images from disk
2. **Tags**: Tags are NOT deleted when project is deleted
3. **Relationships**: Project-tag relationships are removed

---

## 5. Error Handling

### Common Error Scenarios

| Scenario | HTTP Status | Error Message |
|----------|-------------|---------------|
| Project not found | 404 | "Project not found with id: {id}" |
| Tag not found | 404 | "Tag not found with id: {id}" |
| Missing required field | 400 | Validation error with field details |
| Invalid URL format | 400 | "must be a valid URL" |
| Invalid image type | 400 | "Invalid file type. Supported types: JPEG, PNG, WebP" |
| Image too large | 400 | "File size exceeds maximum allowed size (10 MB)" |
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
      "field": "title",
      "rejectedValue": "",
      "message": "must not be blank"
    },
    {
      "field": "description",
      "rejectedValue": null,
      "message": "must not be blank"
    },
    {
      "field": "githubUrl",
      "rejectedValue": "not-a-url",
      "message": "must be a valid URL"
    }
  ],
  "path": "/api/admin/projects"
}
```

---

## Related Documentation

- [API Overview](./README.md) - General API documentation
- [Authentication API](./authentication.md) - JWT authentication
- [Skills API](./skills.md) - Skills management
- [Files API](./files.md) - Generic file upload
- [Features: Image Processing](../features/image-processing.md) - Image optimization details
- [Security: RBAC](../security/rbac.md) - Role-based access control
