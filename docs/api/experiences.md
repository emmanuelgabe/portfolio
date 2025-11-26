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
   - 2.6 [Get Experiences by Type](#26-get-experiences-by-type)
   - 2.7 [Get Ongoing Experiences](#27-get-ongoing-experiences)
   - 2.8 [Get Recent Experiences](#28-get-recent-experiences)
3. [Data Models](#3-data-models)
4. [Business Rules](#4-business-rules)
5. [Error Handling](#5-error-handling)

---

## 1. Overview

The Experiences API provides endpoints for managing professional, educational, certification, and volunteering experiences. Used to build a timeline of career and educational milestones.

**Base Paths**:
- Public: `/api/experiences`
- Admin: `/api/experiences/admin`

**Features**:
- Full CRUD operations for experiences
- Four experience types: WORK, EDUCATION, CERTIFICATION, VOLUNTEERING
- Ongoing experiences tracking (null endDate)
- Type-based filtering
- Recent experiences retrieval with configurable limit
- Chronological sorting by start date (descending)
- Public read access, admin-only write access

---

## 2. Endpoints

### 2.1 List All Experiences

Retrieve all experiences ordered by start date descending (public endpoint).

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
    "ongoing": false,
    "createdAt": "2025-10-15T08:20:00",
    "updatedAt": "2025-10-15T08:20:00"
  },
  {
    "id": 3,
    "company": "Oracle",
    "role": "Oracle Certified Professional Java SE 17 Developer",
    "startDate": "2023-05-10",
    "endDate": "2023-05-10",
    "description": "Certified in advanced Java programming concepts including concurrency, streams, and modular programming.",
    "type": "CERTIFICATION",
    "ongoing": false,
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

**Endpoint**: `POST /api/experiences/admin`

**Authentication**: Required (ROLE_ADMIN)

**Request Body**:
```json
{
  "company": "Acme Corporation",
  "role": "Senior Software Engineer",
  "startDate": "2023-01-15",
  "endDate": null,
  "description": "Leading development of microservices architecture using Spring Boot and Kubernetes.",
  "type": "WORK"
}
```

**Request Schema**:
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `company` | string | Yes | 2-200 chars | Company or organization name |
| `role` | string | Yes | 2-200 chars | Job title, degree, or certification name |
| `startDate` | date | Yes | ISO 8601, past/present | Experience start date |
| `endDate` | date | No | ISO 8601, past/present | Experience end date (null for ongoing) |
| `description` | string | Yes | 10-2000 chars | Detailed description |
| `type` | enum | Yes | See Experience Types | WORK, EDUCATION, CERTIFICATION, VOLUNTEERING |

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
      "field": "company",
      "rejectedValue": "",
      "message": "Company/Organization is required"
    },
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
  "path": "/api/experiences/admin"
}
```

**Unauthorized (401)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/experiences/admin"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/experiences/admin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "company": "Acme Corporation",
    "role": "Senior Software Engineer",
    "startDate": "2023-01-15",
    "endDate": null,
    "description": "Leading development of microservices architecture.",
    "type": "WORK"
  }'
```

---

### 2.4 Update Experience

Update an existing experience (admin only).

**Endpoint**: `PUT /api/experiences/admin/{id}`

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
  "type": "WORK"
}
```

**Request Schema**: Same as [Create Experience](#23-create-experience), but all fields are optional (partial updates supported).

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
  "path": "/api/experiences/admin/999"
}
```

**Example Request (curl)**:
```bash
curl -X PUT http://localhost:8080/api/experiences/admin/1 \
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

**Endpoint**: `DELETE /api/experiences/admin/{id}`

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
  "path": "/api/experiences/admin/999"
}
```

**Example Request (curl)**:
```bash
curl -X DELETE http://localhost:8080/api/experiences/admin/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 2.6 Get Experiences by Type

Retrieve experiences filtered by type (public endpoint).

**Endpoint**: `GET /api/experiences/type/{type}`

**Authentication**: None (public)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `type` | enum | Experience type: WORK, EDUCATION, CERTIFICATION, VOLUNTEERING |

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

# Get all education experiences
curl -X GET http://localhost:8080/api/experiences/type/EDUCATION

# Get all certifications
curl -X GET http://localhost:8080/api/experiences/type/CERTIFICATION

# Get all volunteering experiences
curl -X GET http://localhost:8080/api/experiences/type/VOLUNTEERING
```

---

### 2.7 Get Ongoing Experiences

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

### 2.8 Get Recent Experiences

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
    "ongoing": false,
    "createdAt": "2025-10-10T14:00:00",
    "updatedAt": "2025-10-10T14:00:00"
  },
  {
    "id": 2,
    "company": "Tech University",
    "role": "Master's Degree in Computer Science",
    "startDate": "2020-09-01",
    "endDate": "2022-06-30",
    "description": "Specialized in distributed systems and cloud computing.",
    "type": "EDUCATION",
    "ongoing": false,
    "createdAt": "2025-10-15T08:20:00",
    "updatedAt": "2025-10-15T08:20:00"
  }
]
```

**Example Requests (curl)**:
```bash
# Get 3 most recent experiences (default)
curl -X GET http://localhost:8080/api/experiences/recent

# Get 5 most recent experiences
curl -X GET "http://localhost:8080/api/experiences/recent?limit=5"

# Get 10 most recent experiences
curl -X GET "http://localhost:8080/api/experiences/recent?limit=10"
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
| `company` | string | No | Company or organization name (2-200 chars) |
| `role` | string | No | Job title, degree, or certification name (2-200 chars) |
| `startDate` | date | No | Experience start date (ISO 8601 format) |
| `endDate` | date | Yes | Experience end date (null for ongoing) |
| `description` | string | No | Detailed description (10-2000 chars) |
| `type` | enum | No | Experience type (see below) |
| `ongoing` | boolean | No | True if endDate is null |
| `createdAt` | datetime | No | Creation timestamp |
| `updatedAt` | datetime | No | Last update timestamp |

### Experience Types

| Type | Description | Use Cases |
|------|-------------|-----------|
| `WORK` | Professional work experience | Jobs, internships, freelance projects |
| `EDUCATION` | Educational experience | Degrees, diplomas, courses |
| `CERTIFICATION` | Professional certifications | Technical certifications, qualifications |
| `VOLUNTEERING` | Volunteer work | Community service, open source contributions |

---

## 4. Business Rules

### Experience Creation

1. **Start Date Validation**: Start date must be in the past or present (not future)
2. **End Date Validation**: End date must be in the past or present (not future)
3. **Date Logic**: End date cannot be before start date (validated server-side)
4. **Ongoing Experiences**: Set `endDate` to `null` for ongoing experiences
5. **Type Required**: Experience type must be one of the four enum values
6. **Description Length**: Description must be meaningful (10-2000 characters)

### Experience Sorting

1. **Default Order**: All list endpoints return experiences ordered by `startDate` descending (most recent first)
2. **Type Filtering**: Type-based filtering maintains chronological order
3. **Ongoing Experiences**: Ongoing experiences appear first (newest startDate to oldest)

### Partial Updates

1. **UpdateExperienceRequest**: All fields are optional
2. **Null Handling**: Only provided fields are updated (null values ignored using MapStruct's NullValuePropertyMappingStrategy.IGNORE)
3. **Date Validation**: If updating dates, same validation rules apply

---

## 5. Error Handling

### Common Error Scenarios

| Scenario | HTTP Status | Error Message |
|----------|-------------|---------------|
| Experience not found | 404 | "Experience not found with id: {id}" |
| Missing required field | 400 | Field-specific validation message |
| Invalid experience type | 400 | "Invalid experience type: {type}" |
| Start date in future | 400 | "Start date cannot be in the future" |
| End date in future | 400 | "End date cannot be in the future" |
| End date before start date | 400 | "End date cannot be before start date" |
| Description too short | 400 | "Description must be between 10 and 2000 characters" |
| Company name too short | 400 | "Company/Organization must be between 2 and 200 characters" |
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
      "field": "company",
      "rejectedValue": "A",
      "message": "Company/Organization must be between 2 and 200 characters"
    },
    {
      "field": "role",
      "rejectedValue": "",
      "message": "Role/Position is required"
    },
    {
      "field": "startDate",
      "rejectedValue": "2030-01-01",
      "message": "Start date cannot be in the future"
    },
    {
      "field": "description",
      "rejectedValue": "Short",
      "message": "Description must be between 10 and 2000 characters"
    },
    {
      "field": "type",
      "rejectedValue": null,
      "message": "Experience type is required"
    }
  ],
  "path": "/api/experiences/admin"
}
```

---

## Related Documentation

- [API Overview](./README.md) - General API documentation
- [Authentication API](./authentication.md) - JWT authentication
- [Skills API](./skills.md) - Skills management
- [Projects API](./projects.md) - Projects management
- [Features: Experience Management](../features/experience-management.md) - Experience feature details
- [Security: RBAC](../security/rbac.md) - Role-based access control
