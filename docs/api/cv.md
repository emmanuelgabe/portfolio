# CV Management API

---

## Table of Contents
1. [Overview](#1-overview)
2. [Endpoints](#2-endpoints)
   - 2.1 [Get Current CV](#21-get-current-cv)
   - 2.2 [Download CV](#22-download-cv)
   - 2.3 [Upload CV](#23-upload-cv)
   - 2.4 [List All CVs](#24-list-all-cvs)
   - 2.5 [Set Current CV](#25-set-current-cv)
   - 2.6 [Delete CV](#26-delete-cv)
3. [Data Models](#3-data-models)
4. [Versioning System](#4-versioning-system)
5. [Error Handling](#5-error-handling)

---

## 1. Overview

The CV Management API provides endpoints for uploading, managing, and versioning CV/resume files.

**Base Paths**:
- Public: `/api/cv`
- Admin: `/api/admin/cv`

**Features**:
- CV upload with PDF validation
- Version control system
- Single "current" CV designation
- User tracking (who uploaded which CV)
- Public access to current CV
- Admin-only upload and management

---

## 2. Endpoints

### 2.1 Get Current CV

Retrieve the currently active CV (public endpoint).

**Endpoint**: `GET /api/cv/current`

**Authentication**: None (public)

**Query Parameters**: None

**Success Response** (200 OK):
```json
{
  "id": 3,
  "filename": "cv_emmanuel_gabe_2025.pdf",
  "filePath": "/uploads/cvs/cv_3_1700000000.pdf",
  "uploadedAt": "2025-11-19T10:30:00",
  "isCurrent": true,
  "uploadedBy": {
    "id": 1,
    "username": "admin"
  }
}
```

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "No current CV available",
  "path": "/api/cv/current"
}
```

**Example Request (curl)**:
```bash
curl -X GET http://localhost:8080/api/cv/current
```

**Example Request (JavaScript)**:
```javascript
const response = await fetch('http://localhost:8080/api/cv/current');
if (response.ok) {
  const currentCv = await response.json();
  console.log('Current CV:', currentCv.filename);
} else {
  console.log('No CV available');
}
```

---

### 2.2 Download CV

Download a specific CV file (public endpoint).

**Endpoint**: `GET /api/cv/download/{id}`

**Authentication**: None (public)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | CV ID to download |

**Success Response** (200 OK):
- **Content-Type**: `application/pdf`
- **Content-Disposition**: `attachment; filename="cv_emmanuel_gabe_2025.pdf"`
- **Body**: Binary PDF file

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "CV not found with id: 999",
  "path": "/api/cv/download/999"
}
```

**File Not Found (500 Internal Server Error)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "CV file not found on disk",
  "path": "/api/cv/download/3"
}
```

**Example Request (curl)**:
```bash
# Download CV
curl -X GET http://localhost:8080/api/cv/download/3 \
  --output cv_download.pdf

# Download current CV (chain requests)
CV_ID=$(curl -s http://localhost:8080/api/cv/current | jq -r '.id')
curl -X GET http://localhost:8080/api/cv/download/$CV_ID \
  --output current_cv.pdf
```

**Example Request (JavaScript)**:
```javascript
// Download CV with progress
async function downloadCv(cvId) {
  const response = await fetch(`http://localhost:8080/api/cv/download/${cvId}`);

  if (!response.ok) {
    throw new Error('CV not found');
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = response.headers.get('Content-Disposition')
    ?.split('filename=')[1]
    ?.replace(/"/g, '') || 'cv.pdf';
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

// Usage
await downloadCv(3);
```

---

### 2.3 Upload CV

Upload a new CV (admin only).

**Endpoint**: `POST /api/admin/cv/upload`

**Authentication**: Required (ROLE_ADMIN)

**Request**: `multipart/form-data`

**Form Data**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | file | Yes | PDF file (CV/resume) |

**File Constraints**:
- **Max Size**: 10 MB
- **Supported MIME Type**: `application/pdf` only
- **Validation**: Magic bytes validation (checks actual file content, not extension)

**Success Response** (201 Created):
```json
{
  "id": 4,
  "filename": "cv_emmanuel_gabe_2025_v2.pdf",
  "filePath": "/uploads/cvs/cv_4_1700001000.pdf",
  "uploadedAt": "2025-11-19T15:30:00",
  "isCurrent": false,
  "uploadedBy": {
    "id": 1,
    "username": "admin"
  }
}
```

**Processing Details**:
- File stored in `uploads/cvs/` directory
- File renamed to `cv_{id}_{timestamp}.pdf`
- Original filename preserved in `filename` field
- `isCurrent` set to `false` by default (use "Set Current CV" endpoint to activate)
- Upload tracked to authenticated user

**Error Responses**:

**Invalid File Type (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid file type. Only PDF files are allowed",
  "path": "/api/admin/cv/upload"
}
```

**File Too Large (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "File size exceeds maximum allowed size (10 MB)",
  "path": "/api/admin/cv/upload"
}
```

**No File Provided (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "File is required",
  "path": "/api/admin/cv/upload"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/admin/cv/upload \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -F "file=@/path/to/cv_emmanuel_gabe.pdf"
```

**Example Request (JavaScript)**:
```javascript
const formData = new FormData();
formData.append('file', cvFileInput.files[0]);

const response = await fetch('http://localhost:8080/api/admin/cv/upload', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`
  },
  body: formData
});

const uploadedCv = await response.json();
console.log('CV uploaded:', uploadedCv.filename);
console.log('CV ID:', uploadedCv.id);
```

---

### 2.4 List All CVs

Retrieve all uploaded CVs (admin only).

**Endpoint**: `GET /api/admin/cv/all`

**Authentication**: Required (ROLE_ADMIN)

**Query Parameters**: None

**Success Response** (200 OK):
```json
[
  {
    "id": 4,
    "filename": "cv_emmanuel_gabe_2025_v2.pdf",
    "filePath": "/uploads/cvs/cv_4_1700001000.pdf",
    "uploadedAt": "2025-11-19T15:30:00",
    "isCurrent": true,
    "uploadedBy": {
      "id": 1,
      "username": "admin"
    }
  },
  {
    "id": 3,
    "filename": "cv_emmanuel_gabe_2025.pdf",
    "filePath": "/uploads/cvs/cv_3_1700000000.pdf",
    "uploadedAt": "2025-11-18T10:30:00",
    "isCurrent": false,
    "uploadedBy": {
      "id": 1,
      "username": "admin"
    }
  },
  {
    "id": 2,
    "filename": "cv_emmanuel_gabe_2024.pdf",
    "filePath": "/uploads/cvs/cv_2_1699000000.pdf",
    "uploadedAt": "2025-11-01T09:00:00",
    "isCurrent": false,
    "uploadedBy": {
      "id": 2,
      "username": "editor"
    }
  }
]
```

**Note**: CVs are ordered by upload date (most recent first).

**Example Request (curl)**:
```bash
curl -X GET http://localhost:8080/api/admin/cv/all \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 2.5 Set Current CV

Set a specific CV as the current active CV (admin only).

**Endpoint**: `PUT /api/admin/cv/{id}/set-current`

**Authentication**: Required (ROLE_ADMIN)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | CV ID to set as current |

**Success Response** (200 OK):
```json
{
  "id": 4,
  "filename": "cv_emmanuel_gabe_2025_v2.pdf",
  "filePath": "/uploads/cvs/cv_4_1700001000.pdf",
  "uploadedAt": "2025-11-19T15:30:00",
  "isCurrent": true,
  "uploadedBy": {
    "id": 1,
    "username": "admin"
  }
}
```

**Business Rule**: Only ONE CV can be current at any time. Setting a CV as current automatically unsets all other CVs.

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "CV not found with id: 999",
  "path": "/api/admin/cv/999/set-current"
}
```

**Example Request (curl)**:
```bash
curl -X PUT http://localhost:8080/api/admin/cv/4/set-current \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**Example Workflow (JavaScript)**:
```javascript
// 1. Upload new CV
const formData = new FormData();
formData.append('file', newCvFile);

const uploadResponse = await fetch('http://localhost:8080/api/admin/cv/upload', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${accessToken}` },
  body: formData
});

const newCv = await uploadResponse.json();

// 2. Set as current
await fetch(`http://localhost:8080/api/admin/cv/${newCv.id}/set-current`, {
  method: 'PUT',
  headers: { 'Authorization': `Bearer ${accessToken}` }
});

console.log('New CV is now active');
```

---

### 2.6 Delete CV

Delete a CV (admin only).

**Endpoint**: `DELETE /api/admin/cv/{id}`

**Authentication**: Required (ROLE_ADMIN)

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | CV ID to delete |

**Success Response** (200 OK):
```json
{
  "message": "CV deleted successfully"
}
```

**Business Rule**: You can delete the current CV. If deleted, there will be no current CV until another is set.

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "CV not found with id: 999",
  "path": "/api/admin/cv/999"
}
```

**Note**: Deleting a CV:
- Removes the database record
- Deletes the physical PDF file from disk
- If it was the current CV, there will be no current CV

**Example Request (curl)**:
```bash
curl -X DELETE http://localhost:8080/api/admin/cv/2 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## 3. Data Models

### CvResponse

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | integer | No | CV ID |
| `filename` | string | No | Original filename (e.g., "cv_john_doe.pdf") |
| `filePath` | string | No | Server file path (e.g., "/uploads/cvs/cv_3_1700000000.pdf") |
| `uploadedAt` | datetime | No | Upload timestamp |
| `isCurrent` | boolean | No | Whether this is the active CV |
| `uploadedBy` | object | No | User who uploaded the CV |

### UserResponse (Nested in CvResponse)

| Field | Type | Description |
|-------|------|-------------|
| `id` | integer | User ID |
| `username` | string | Username |

---

## 4. Versioning System

### How Versioning Works

1. **Multiple CVs Allowed**: You can upload multiple CV versions
2. **One Current CV**: Only one CV can be marked as "current" at any time
3. **Automatic Deactivation**: Setting a CV as current automatically deactivates all others
4. **Historical Tracking**: All previous CVs remain accessible unless explicitly deleted
5. **User Tracking**: Each CV tracks which admin user uploaded it

### Typical Workflow

```
1. Upload CV v1 → isCurrent: false
2. Set CV v1 as current → isCurrent: true
3. Upload CV v2 → isCurrent: false (v1 still current)
4. Set CV v2 as current → v2.isCurrent: true, v1.isCurrent: false
5. Upload CV v3 → isCurrent: false (v2 still current)
6. Delete CV v1 → Only v2 (current) and v3 remain
```

### Database Constraint

**UNIQUE Constraint**: Only one row can have `is_current = true`

```sql
CREATE UNIQUE INDEX unique_current_cv ON cvs (is_current) WHERE is_current = true;
```

This ensures data integrity at the database level.

---

## 5. Error Handling

### Common Error Scenarios

| Scenario | HTTP Status | Error Message |
|----------|-------------|---------------|
| CV not found | 404 | "CV not found with id: {id}" |
| No current CV exists | 404 | "No current CV available" |
| File not PDF | 400 | "Invalid file type. Only PDF files are allowed" |
| File too large | 400 | "File size exceeds maximum allowed size (10 MB)" |
| No file provided | 400 | "File is required" |
| Physical file missing | 500 | "CV file not found on disk" |
| Missing Authorization | 401 | "Missing or invalid Authorization header" |
| Insufficient permissions | 403 | "Access denied: insufficient permissions" |

### Frontend File Validation

**Recommended Pre-Upload Validation**:
```javascript
function validateCvFile(file) {
  const errors = [];

  // Check file type
  if (file.type !== 'application/pdf') {
    errors.push('Only PDF files are allowed');
  }

  // Check file size (10 MB = 10 * 1024 * 1024 bytes)
  const maxSize = 10 * 1024 * 1024;
  if (file.size > maxSize) {
    errors.push('File size must be less than 10 MB');
  }

  // Check if file exists
  if (!file || file.size === 0) {
    errors.push('File is required');
  }

  return {
    valid: errors.length === 0,
    errors
  };
}

// Usage
const validation = validateCvFile(fileInput.files[0]);
if (!validation.valid) {
  alert(validation.errors.join('\n'));
} else {
  // Proceed with upload
}
```

### Handling Download Errors

```javascript
async function safeDownloadCv(cvId) {
  try {
    const response = await fetch(`http://localhost:8080/api/cv/download/${cvId}`);

    if (response.status === 404) {
      console.error('CV not found');
      return null;
    }

    if (response.status === 500) {
      console.error('CV file missing on server');
      return null;
    }

    if (!response.ok) {
      throw new Error(`Download failed: ${response.statusText}`);
    }

    const blob = await response.blob();
    return blob;
  } catch (error) {
    console.error('Download error:', error);
    return null;
  }
}
```

---

## Related Documentation

- [API Overview](./README.md) - General API documentation
- [Authentication API](./authentication.md) - JWT authentication
- [Files API](./files.md) - Generic file upload
- [Features: CV Management](../features/cv-management.md) - CV system architecture
- [Features: File Storage](../features/file-storage.md) - File storage details
- [Security: RBAC](../security/rbac.md) - Role-based access control
