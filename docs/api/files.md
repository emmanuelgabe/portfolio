# File Upload API

---

## Table of Contents
1. [Overview](#1-overview)
2. [Endpoints](#2-endpoints)
   - 2.1 [Generic File Upload](#21-generic-file-upload)
3. [File Validation](#3-file-validation)
4. [Supported File Types](#4-supported-file-types)
5. [Error Handling](#5-error-handling)

---

## 1. Overview

The File Upload API provides a generic endpoint for uploading files with security validation.

**Base Path**: `/api/admin/upload`

**Features**:
- Generic file upload for admin users
- MIME type validation using magic bytes
- File size limits
- Configurable upload paths
- Security-first validation

**Note**: This is a generic upload endpoint. For specific use cases, prefer dedicated endpoints:
- Project images: `POST /api/admin/projects/{id}/upload-image`
- CV files: `POST /api/admin/cv/upload`

---

## 2. Endpoints

### 2.1 Generic File Upload

Upload a file to the server (admin only).

**Endpoint**: `POST /api/admin/upload`

**Authentication**: Required (ROLE_ADMIN)

**Request**: `multipart/form-data`

**Form Data**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | file | Yes | File to upload |

**File Constraints**:
- **Max Size**: 10 MB (configurable)
- **Supported MIME Types**: See [Supported File Types](#4-supported-file-types)
- **Validation Method**: Magic bytes validation (not extension-based)

**Success Response** (200 OK):
```json
{
  "filename": "document_1700000000.pdf",
  "filePath": "/uploads/document_1700000000.pdf",
  "fileSize": 245760,
  "mimeType": "application/pdf",
  "uploadedAt": "2025-11-19T15:30:00"
}
```

**Response Schema**:
| Field | Type | Description |
|-------|------|-------------|
| `filename` | string | Generated filename with timestamp |
| `filePath` | string | Server file path (relative to upload root) |
| `fileSize` | integer | File size in bytes |
| `mimeType` | string | Detected MIME type |
| `uploadedAt` | datetime | Upload timestamp |

**Error Responses**:

**Invalid File Type (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid file type: text/html. Supported types: PDF, JPEG, PNG, WebP",
  "path": "/api/admin/upload"
}
```

**File Too Large (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "File size exceeds maximum allowed size (10 MB)",
  "path": "/api/admin/upload"
}
```

**No File Provided (400 Bad Request)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "File is required",
  "path": "/api/admin/upload"
}
```

**Unauthorized (401)**:
```json
{
  "timestamp": "2025-11-19T14:23:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/admin/upload"
}
```

**Example Request (curl)**:
```bash
curl -X POST http://localhost:8080/api/admin/upload \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -F "file=@/path/to/document.pdf"
```

**Example Request (JavaScript)**:
```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

const response = await fetch('http://localhost:8080/api/admin/upload', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`
  },
  body: formData
});

const uploadedFile = await response.json();
console.log('File uploaded:', uploadedFile.filename);
console.log('File path:', uploadedFile.filePath);
console.log('File size:', uploadedFile.fileSize, 'bytes');
```

**Example with Progress Tracking (JavaScript)**:
```javascript
async function uploadFileWithProgress(file, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('file', file);

    // Progress tracking
    xhr.upload.addEventListener('progress', (e) => {
      if (e.lengthComputable) {
        const percentComplete = (e.loaded / e.total) * 100;
        onProgress(percentComplete);
      }
    });

    // Success handler
    xhr.addEventListener('load', () => {
      if (xhr.status === 200) {
        resolve(JSON.parse(xhr.responseText));
      } else {
        reject(new Error(`Upload failed: ${xhr.statusText}`));
      }
    });

    // Error handler
    xhr.addEventListener('error', () => {
      reject(new Error('Upload failed'));
    });

    // Send request
    xhr.open('POST', 'http://localhost:8080/api/admin/upload');
    xhr.setRequestHeader('Authorization', `Bearer ${accessToken}`);
    xhr.send(formData);
  });
}

// Usage
await uploadFileWithProgress(file, (progress) => {
  console.log(`Upload progress: ${progress.toFixed(2)}%`);
});
```

---

## 3. File Validation

### Magic Bytes Validation

The API validates files using **magic bytes** (file signatures), not file extensions. This prevents malicious files from being disguised with fake extensions.

**Example**:
```
Filename: innocent.pdf
Actual content: HTML file (magic bytes: 3C 21 44 4F 43 54 59 50 45)
Result: REJECTED (Invalid MIME type detected)
```

**Validation Process**:
1. Read first bytes of uploaded file
2. Detect actual MIME type from magic bytes
3. Compare against whitelist of allowed types
4. Reject if MIME type not allowed

### File Size Limits

**Default**: 10 MB (10,485,760 bytes)

**Configuration** (application.yml):
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

**Override for Specific Endpoints**:
- Project images: 10 MB
- CV files: 10 MB
- Generic uploads: 10 MB

---

## 4. Supported File Types

The following MIME types are supported:

### Documents

| MIME Type | File Extension | Description |
|-----------|----------------|-------------|
| `application/pdf` | .pdf | PDF documents |

### Images

| MIME Type | File Extension | Description |
|-----------|----------------|-------------|
| `image/jpeg` | .jpg, .jpeg | JPEG images |
| `image/png` | .png | PNG images |
| `image/webp` | .webp | WebP images |

**Note**: Images uploaded via generic endpoint are NOT automatically converted to WebP. For automatic WebP conversion and thumbnails, use the dedicated project image upload endpoint.

### Unsupported Types

The following types are **explicitly blocked**:

| Type | Reason |
|------|--------|
| Executables (.exe, .bat, .sh) | Security risk |
| Scripts (.js, .py, .rb) | Security risk |
| Archives (.zip, .rar, .tar) | Potential for malicious content |
| Office files (.docx, .xlsx) | Not needed for portfolio |
| HTML files (.html, .htm) | XSS risk |

---

## 5. Error Handling

### Common Error Scenarios

| Scenario | HTTP Status | Error Message |
|----------|-------------|---------------|
| Invalid MIME type | 400 | "Invalid file type: {type}. Supported types: PDF, JPEG, PNG, WebP" |
| File too large | 400 | "File size exceeds maximum allowed size (10 MB)" |
| No file provided | 400 | "File is required" |
| Empty file | 400 | "File is empty" |
| Malformed multipart request | 400 | "Invalid multipart request" |
| Disk full | 500 | "Failed to store file: insufficient disk space" |
| Permission denied | 500 | "Failed to store file: permission denied" |
| Missing Authorization | 401 | "Missing or invalid Authorization header" |
| Insufficient permissions | 403 | "Access denied: insufficient permissions" |

### Frontend Validation

**Recommended Pre-Upload Checks**:
```javascript
const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'image/jpeg',
  'image/png',
  'image/webp'
];

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

function validateFile(file) {
  const errors = [];

  // Check if file exists
  if (!file) {
    errors.push('File is required');
    return { valid: false, errors };
  }

  // Check file size
  if (file.size === 0) {
    errors.push('File is empty');
  }

  if (file.size > MAX_FILE_SIZE) {
    errors.push(`File size must be less than ${MAX_FILE_SIZE / 1024 / 1024} MB`);
  }

  // Check MIME type
  if (!ALLOWED_MIME_TYPES.includes(file.type)) {
    errors.push(`Invalid file type: ${file.type}. Allowed types: PDF, JPEG, PNG, WebP`);
  }

  return {
    valid: errors.length === 0,
    errors
  };
}

// Usage
const validation = validateFile(fileInput.files[0]);
if (!validation.valid) {
  alert('Validation errors:\n' + validation.errors.join('\n'));
} else {
  // Proceed with upload
  await uploadFile(fileInput.files[0]);
}
```

### Handling Upload Errors

```javascript
async function safeUploadFile(file) {
  // Client-side validation
  const validation = validateFile(file);
  if (!validation.valid) {
    return {
      success: false,
      errors: validation.errors
    };
  }

  // Upload
  try {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch('http://localhost:8080/api/admin/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      },
      body: formData
    });

    if (!response.ok) {
      const error = await response.json();
      return {
        success: false,
        errors: [error.message]
      };
    }

    const result = await response.json();
    return {
      success: true,
      data: result
    };
  } catch (error) {
    return {
      success: false,
      errors: ['Network error: ' + error.message]
    };
  }
}

// Usage
const result = await safeUploadFile(file);
if (result.success) {
  console.log('Upload successful:', result.data);
} else {
  console.error('Upload failed:', result.errors);
}
```

---

## Related Documentation

- [API Overview](./README.md) - General API documentation
- [Authentication API](./authentication.md) - JWT authentication
- [Projects API](./projects.md) - Project image upload (specialized)
- [CV API](./cv.md) - CV upload (specialized)
- [Features: File Storage](../features/file-storage.md) - File storage architecture
- [Features: Image Processing](../features/image-processing.md) - Image optimization
- [Security: RBAC](../security/rbac.md) - Role-based access control
