# Experiences API

---

## Table of Contents
1. [Overview](#1-overview)
2. [Endpoints](#2-endpoints)
   - 2.1 [List All Experiences](#21-list-all-experiences)
   - 2.2 [Get Experience by ID](#22-get-experience-by-id)
   - 2.3 [Create Experience](#23-create-experience)
   - 2.4 [Update Experience](#24-update-experience)
   - 2.5 [Delete Experience](#25-delete-experience)
   - 2.6 [Reorder Experiences](#26-reorder-experiences)
   - 2.7 [Get Experiences by Type](#27-get-experiences-by-type)
   - 2.8 [Get Ongoing Experiences](#28-get-ongoing-experiences)
   - 2.9 [Get Recent Experiences](#29-get-recent-experiences)
3. [Data Models](#3-data-models)
4. [Business Rules](#4-business-rules)
5. [Error Handling](#5-error-handling)

---

## 1. Overview

The Experiences API provides endpoints for managing professional, educational, certification, and volunteering experiences. Used to build a timeline of career and educational milestones.

**Base Paths**:
- Public: `/api/experiences`
- Admin: `/api/admin/experiences`

**Features**:
- Full CRUD operations for experiences
- Five experience types: WORK, STAGE, EDUCATION, CERTIFICATION, VOLUNTEERING
- Ongoing experiences tracking (null endDate)
- Manual display order with reorder endpoint
- Type-based filtering
- Recent experiences retrieval with configurable limit
- Sorting by display order then start date descending
- Public read access, admin-only write access

---

## 2. Endpoints

### 2.1 List All Experiences

Retrieve all experiences ordered by display order then start date descending (public endpoint).

**Endpoint**: `GET /api/experiences`

**Authentication**: None (public)

**Query Parameters**: None

**Success Response** (200 OK):
```json
[
  {
    "id": 1,
    "company": "Acme Corporation",
    "role": "Senior Software Engineer",
    "startDate": "2023-01-15",
    "endDate": null,
    "description": "Leading development of microservices architecture using Spring Boot and Kubernetes. Mentoring junior developers and conducting code reviews.",
    "type": "WORK",
    "showMonths": true,
    "displayOrder": 0,
    "ongoing": true,
    "createdAt": "2025-11-01T10:30:00",
    "updatedAt": "2025-11-01T10:30:00"
  },
  {
    "id": 2,
    "company": "Tech University",
    "role": "Master's Degree in Computer Science",
    "startDate": "2020-09-01",
    "endDate": "2022-06-30",
    "description": "Specialized in distributed systems and cloud computing. Thesis on microservices optimization strategies.",
    "type": "EDUCATION",
    "showMonths": true,
    "displayOrder": 1,
    "ongoing": false,
    "createdAt": "2025-10-15T08:20:00",
    "updatedAt": "2025-10-15T08:20:00"
  },
  {
    "id": 3,
    "company": null,
    "role": null,
    "startDate": null,
    "endDate": null,
    "description": "Contributed to open-source projects and community development initiatives.",
    "type": null,
    "showMonths": true,
    "displayOrder": 2,
    "ongoing": true,
    "createdAt": "2025-10-10T14:00:00",
    "updatedAt": "2025-10-10T14:00:00"
  }
]
```

**Example Request (curl)**:
```bash
curl -X GET http://localhost:8080/api/experiences
```

**Example Request (JavaScript)**:
```javascript
const response = await fetch('http://localhost:8080/api/experiences');
const experiences = await response.json();
```

---

### 2.2 Get Experience by ID

Retrieve a single experience by its ID (public endpoint).

**Endpoint**: `GET /api/experiences/{id}`

**Authentication**: None (public)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Experience ID |

**Success Response** (200 OK):
```json
{
  "id": 1,
  "company": "Acme Corporation",
  "role": "Senior Software Engineer",
  "startDate": "2023-01-15",
  "endDate": null,
  "description": "Leading development of microservices architecture using Spring Boot and Kubernetes. Mentoring junior developers and conducting code reviews.",
  "type": "WORK",
  "showMonths": true,
  "displayOrder": 0,
  "ongoing": true,
  "createdAt": "2025-11-01T10:30:00",
  "updatedAt": "2025-11-01T10:30:00"
}
```

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Experience not found with id: 999",
  "path": "/api/experiences/999"
}
```

**Example Request (curl)**:
```bash
curl -X GET http://localhost:8080/api/experiences/1
```

---

### 2.3 Create Experience

Create a new experience (admin only).

**Endpoint**: `POST /api/admin/experiences`

**Authentication**: Required (ROLE_ADMIN)

**Request Body**:
```json
{
  "company": "Acme Corporation",
  "role": "Senior Software Engineer",
  "startDate": "2023-01-15",
  "endDate": null,
  "description": "Leading development of microservices architecture using Spring Boot and Kubernetes.",
  "type": "WORK",
  "showMonths": true
}
```

**Request Schema**:
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `company` | string | No | max 200 chars | Company or organization name |
| `role` | string | No | max 200 chars | Job title, degree, or certification name |
| `startDate` | date | No | ISO 8601, past/present | Experience start date |
| `endDate` | date | No | ISO 8601, past/present | Experience end date (null for ongoing) |
| `description` | string | Yes | 10-2000 chars | Detailed description |
| `type` | enum | No | See Experience Types | WORK, STAGE, EDUCATION, CERTIFICATION, VOLUNTEERING |
| `showMonths` | boolean | No | Default true | Display months in date range |

**Success Response** (201 Created):
```json
{
  "id": 4,
  "company": "Acme Corporation",
  "role": "Senior Software Engineer",
  "startDate": "2023-01-15",
  "endDate": null,
  "description": "Leading development of microservices architecture using Spring Boot and Kubernetes.",
  "type": "WORK",
  "showMonths": true,
  "displayOrder": null,
  "ongoing": true,
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
      "field": "description",
      "rejectedValue": "Too short",
      "message": "Description must be between 10 and 2000 characters"
    },
    {
      "field": "startDate",
      "rejectedValue": "2030-01-01",
      "message": "Start date cannot be in the future"
    }
  ],
  "path": "/api/admin/experiences"
}
```

**Unauthorized (401)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/admin/experiences"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/admin/experiences \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "company": "Acme Corporation",
    "role": "Senior Software Engineer",
    "startDate": "2023-01-15",
    "endDate": null,
    "description": "Leading development of microservices architecture.",
    "type": "WORK",
    "showMonths": true
  }'
```

---

### 2.4 Update Experience

Update an existing experience (admin only).

**Endpoint**: `PUT /api/admin/experiences/{id}`

**Authentication**: Required (ROLE_ADMIN)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Experience ID to update |

**Request Body**:
```json
{
  "company": "Acme Corporation",
  "role": "Lead Software Architect",
  "startDate": "2023-01-15",
  "endDate": "2025-12-31",
  "description": "Promoted to Lead Architect. Designing enterprise-scale systems and leading technical strategy.",
  "type": "WORK",
  "showMonths": true
}
```

**Request Schema**: Same as [Create Experience](#23-create-experience), all fields are optional (partial updates supported).

**Success Response** (200 OK):
```json
{
  "id": 1,
  "company": "Acme Corporation",
  "role": "Lead Software Architect",
  "startDate": "2023-01-15",
  "endDate": "2025-12-31",
  "description": "Promoted to Lead Architect. Designing enterprise-scale systems and leading technical strategy.",
  "type": "WORK",
  "showMonths": true,
  "displayOrder": 0,
  "ongoing": false,
  "createdAt": "2025-11-01T10:30:00",
  "updatedAt": "2025-11-19T16:45:00"
}
```

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Experience not found with id: 999",
  "path": "/api/admin/experiences/999"
}
```

**Example Request (curl)**:
```bash
curl -X PUT http://localhost:8080/api/admin/experiences/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "role": "Lead Software Architect",
    "description": "Promoted to Lead Architect."
  }'
```

---

### 2.5 Delete Experience

Delete an experience (admin only).

**Endpoint**: `DELETE /api/admin/experiences/{id}`

**Authentication**: Required (ROLE_ADMIN)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Experience ID to delete |

**Success Response** (204 No Content):
No response body.

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Experience not found with id: 999",
  "path": "/api/admin/experiences/999"
}
```

**Example Request (curl)**:
```bash
curl -X DELETE http://localhost:8080/api/admin/experiences/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 2.6 Reorder Experiences

Reorder experiences by providing an ordered list of IDs (admin only).

**Endpoint**: `POST /api/admin/experiences/reorder`

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
| `orderedIds` | array | Yes | Ordered list of experience IDs |

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
  "path": "/api/admin/experiences/reorder"
}
```

**Experience Not Found (404 Not Found)**:
```json
{
  "timestamp": "2026-01-15T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Experience not found with id: 999",
  "path": "/api/admin/experiences/reorder"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/admin/experiences/reorder \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{"orderedIds": [3, 1, 2, 5, 4]}'
```

**Note**: This endpoint updates the `displayOrder` field for each experience. The cache is automatically invalidated after reordering.

---

### 2.7 Get Experiences by Type

Retrieve experiences filtered by type (public endpoint).

**Endpoint**: `GET /api/experiences/type/{type}`

**Authentication**: None (public)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `type` | enum | Experience type: WORK, STAGE, EDUCATION, CERTIFICATION, VOLUNTEERING |

**Success Response** (200 OK):
```json
[
  {
    "id": 1,
    "company": "Acme Corporation",
    "role": "Senior Software Engineer",
    "startDate": "2023-01-15",
    "endDate": null,
    "description": "Leading development of microservices architecture.",
    "type": "WORK",
    "showMonths": true,
    "displayOrder": 0,
    "ongoing": true,
    "createdAt": "2025-11-01T10:30:00",
    "updatedAt": "2025-11-01T10:30:00"
  }
]
```

**Error Response (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid experience type: INVALID_TYPE",
  "path": "/api/experiences/type/INVALID_TYPE"
}
```

**Example Requests (curl)**:
```bash
# Get all work experiences
curl -X GET http://localhost:8080/api/experiences/type/WORK

# Get all internship experiences
curl -X GET http://localhost:8080/api/experiences/type/STAGE

# Get all education experiences
curl -X GET http://localhost:8080/api/experiences/type/EDUCATION

# Get all certifications
curl -X GET http://localhost:8080/api/experiences/type/CERTIFICATION

# Get all volunteering experiences
curl -X GET http://localhost:8080/api/experiences/type/VOLUNTEERING
```

---

### 2.8 Get Ongoing Experiences

Retrieve all ongoing experiences (where endDate is null).

**Endpoint**: `GET /api/experiences/ongoing`

**Authentication**: None (public)

**Query Parameters**: None

**Success Response** (200 OK):
```json
[
  {
    "id": 1,
    "company": "Acme Corporation",
    "role": "Senior Software Engineer",
    "startDate": "2023-01-15",
    "endDate": null,
    "description": "Leading development of microservices architecture.",
    "type": "WORK",
    "showMonths": true,
    "displayOrder": 0,
    "ongoing": true,
    "createdAt": "2025-11-01T10:30:00",
    "updatedAt": "2025-11-01T10:30:00"
  },
  {
    "id": 5,
    "company": "Local Tech Community",
    "role": "Volunteer Mentor",
    "startDate": "2024-06-01",
    "endDate": null,
    "description": "Mentoring aspiring developers in web development and software engineering best practices.",
    "type": "VOLUNTEERING",
    "showMonths": true,
    "displayOrder": 1,
    "ongoing": true,
    "createdAt": "2025-10-05T12:15:00",
    "updatedAt": "2025-10-05T12:15:00"
  }
]
```

**Example Request (curl)**:
```bash
curl -X GET http://localhost:8080/api/experiences/ongoing
```

---

### 2.9 Get Recent Experiences

Retrieve the N most recent experiences ordered by start date descending. Used for displaying a summary on the home page.

**Endpoint**: `GET /api/experiences/recent`

**Authentication**: None (public)

**Query Parameters**:
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `limit` | integer | No | 3 | Maximum number of experiences to return |

**Success Response** (200 OK):
```json
[
  {
    "id": 1,
    "company": "Acme Corporation",
    "role": "Senior Software Engineer",
    "startDate": "2023-01-15",
    "endDate": null,
    "description": "Leading development of microservices architecture.",
    "type": "WORK",
    "showMonths": true,
    "displayOrder": 0,
    "ongoing": true,
    "createdAt": "2025-11-01T10:30:00",
    "updatedAt": "2025-11-01T10:30:00"
  },
  {
    "id": 3,
    "company": "Oracle",
    "role": "Oracle Certified Professional Java SE 17 Developer",
    "startDate": "2023-05-10",
    "endDate": "2023-05-10",
    "description": "Certified in advanced Java programming concepts.",
    "type": "CERTIFICATION",
    "showMonths": true,
    "displayOrder": null,
    "ongoing": false,
    "createdAt": "2025-10-10T14:00:00",
    "updatedAt": "2025-10-10T14:00:00"
  }
]
```

**Example Requests (curl)**:
```bash
# Get 3 most recent experiences (default)
curl -X GET http://localhost:8080/api/experiences/recent

# Get 5 most recent experiences
curl -X GET "http://localhost:8080/api/experiences/recent?limit=5"
```

**Example Request (JavaScript)**:
```javascript
// Get 3 most recent experiences for home page
const response = await fetch('http://localhost:8080/api/experiences/recent?limit=3');
const recentExperiences = await response.json();
```

---

## 3. Data Models

### ExperienceResponse

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | integer | No | Experience ID |
| `company` | string | Yes | Company or organization name (max 200 chars) |
| `role` | string | Yes | Job title, degree, or certification name (max 200 chars) |
| `startDate` | date | Yes | Experience start date (ISO 8601 format) |
| `endDate` | date | Yes | Experience end date (null for ongoing) |
| `description` | string | No | Detailed description (10-2000 chars) |
| `type` | enum | Yes | Experience type (see below) |
| `showMonths` | boolean | No | Show months in the displayed date range |
| `displayOrder` | integer | Yes | Manual display order |
| `ongoing` | boolean | No | True if endDate is null |
| `createdAt` | datetime | No | Creation timestamp |
| `updatedAt` | datetime | No | Last update timestamp |

### Experience Types

| Type | Description | Use Cases |
|------|-------------|-----------|
| `WORK` | Professional work experience | Jobs, freelance projects |
| `STAGE` | Internship experience | Internships, trainee positions |
| `EDUCATION` | Educational experience | Degrees, diplomas, courses |
| `CERTIFICATION` | Professional certifications | Technical certifications, qualifications |
| `VOLUNTEERING` | Volunteer work | Community service, open source contributions |

---

## 4. Business Rules

### Experience Creation

1. **Optional Fields**: `company`, `role`, `startDate`, and `type` are optional
2. **Required Field**: Only `description` is required (10-2000 chars)
3. **Date Validation**: Start and end dates must be in the past or present (not future)
4. **Date Logic**: End date cannot be before start date (validated server-side)
5. **Ongoing Experiences**: Set `endDate` to `null` for ongoing experiences
6. **Show Months**: Defaults to `true` if not specified

### Experience Sorting

1. **Default Order**: Sorted by `displayOrder` ascending, then `startDate` descending
2. **Display Order**: Experiences with `displayOrder` set appear first
3. **Null Display Order**: Experiences without `displayOrder` are sorted by `startDate`

### Partial Updates

1. **UpdateExperienceRequest**: All fields are optional
2. **Null Handling**: Only provided fields are updated (null values ignored using MapStruct's NullValuePropertyMappingStrategy.IGNORE)
3. **Empty Strings**: Empty strings for `company` and `role` are normalized to `null`
4. **Date Validation**: If updating dates, same validation rules apply

---

## 5. Error Handling

### Common Error Scenarios

| Scenario | HTTP Status | Error Message |
|----------|-------------|---------------|
| Experience not found | 404 | "Experience not found with id: {id}" |
| Missing description | 400 | "Description is required" |
| Invalid experience type | 400 | "Invalid experience type: {type}" |
| Start date in future | 400 | "Start date cannot be in the future" |
| End date in future | 400 | "End date cannot be in the future" |
| End date before start date | 400 | "End date cannot be before start date" |
| Description too short | 400 | "Description must be between 10 and 2000 characters" |
| Missing Authorization | 401 | "Missing or invalid Authorization header" |
| Insufficient permissions | 403 | "Access denied: insufficient permissions" |

---

## Related Documentation

- [API Overview](./README.md) - General API documentation
- [Authentication API](./authentication.md) - JWT authentication
- [Skills API](./skills.md) - Skills management
- [Projects API](./projects.md) - Projects management
- [Features: Experience Management](../features/experience-management.md) - Experience feature details
- [Security: RBAC](../security/rbac.md) - Role-based access control
