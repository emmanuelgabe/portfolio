# File Storage Architecture

---

## Table of Contents
1. [Overview](#1-overview)
2. [Storage Structure](#2-storage-structure)
3. [File Naming Conventions](#3-file-naming-conventions)
4. [Security Validation](#4-security-validation)

---

## 1. Overview

The File Storage system provides a secure, organized structure for managing uploaded files in the Portfolio application.

**Key Features**:
- Organized directory structure
- Timestamped filenames (prevents collisions)
- MIME type validation (magic bytes)
- Configurable storage paths
- File size limits
- Old file cleanup

**Supported File Types**:
- **Images**: JPEG, PNG, WebP (for projects)
- **Documents**: PDF (for CVs)

---

## 2. Storage Structure

### Directory Organization

```
uploads/
├── projects/                          # Project images
│   ├── project_1_1700000000.webp     # Full image
│   ├── project_1_1700000000_thumb.webp # Thumbnail
│   ├── project_2_1700100000.webp
│   ├── project_2_1700100000_thumb.webp
│   └── ...
│
├── cvs/                               # CV documents
│   ├── cv_1_1700000000.pdf
│   ├── cv_2_1700100000.pdf
│   └── ...
│
└── (future directories)
    ├── avatars/                       # User avatars (future)
    ├── attachments/                   # Generic attachments (future)
    └── ...
```

### Directory Purpose

| Directory | Purpose | File Types | Access |
|-----------|---------|------------|--------|
| `uploads/projects/` | Project images | WebP | Public read |
| `uploads/cvs/` | CV documents | PDF | Public read (current only) |

### Storage Locations by Environment

**Local Development**:
```
Working directory: C:\Users\...\portfolio\
Storage: C:\Users\...\portfolio\uploads\
```

**Docker (Development)**:
```
Container: /app/
Storage: /app/uploads/
Volume: portfolio-uploads (persisted)
```

**Production (Recommended)**:
```
Application: /var/www/portfolio/
Storage: /var/www/uploads/ (separate partition)
OR
Cloud Storage: AWS S3, Azure Blob, GCP Cloud Storage
```

---

## 3. File Naming Conventions

### Naming Pattern

**Format**: `{type}_{id}_{timestamp}.{extension}`

**Examples**:
```
project_1_1700000000.webp
project_1_1700000000_thumb.webp
cv_3_1700200000.pdf
```

### Components

| Component | Description | Example |
|-----------|-------------|---------|
| `{type}` | File category | `project`, `cv` |
| `{id}` | Entity ID | `1`, `2`, `3` |
| `{timestamp}` | Unix timestamp (ms) | `1700000000` |
| `{extension}` | File extension | `webp`, `pdf` |

**Special Suffix**:
- `_thumb`: Indicates thumbnail version

### Why Timestamps?

**Benefits**:
1. **Uniqueness**: Prevents filename collisions
2. **Sorting**: Files naturally sorted by upload time
3. **Tracking**: Easy to identify when file was uploaded
4. **Caching**: Browser cache busting (URL changes on update)

**Example Collision Prevention**:
```
Without timestamp:
- project_1.webp (uploaded 9:00 AM)
- project_1.webp (uploaded 9:05 AM) ← OVERWRITES!

With timestamp:
- project_1_1700000000.webp (uploaded 9:00 AM)
- project_1_1700000300.webp (uploaded 9:05 AM) [OK] Both preserved
```

---

## 4. Security Validation

### MIME Type Validation

**Two-Layer Validation**:

1. **Content-Type Header** (client-provided, untrusted)
2. **Magic Bytes** (file content, trusted)

**Magic Bytes Lookup**:

| File Type | Magic Bytes | Hex Representation |
|-----------|-------------|---------------------|
| **JPEG** | `ÿØÿ` | `FF D8 FF` |
| **PNG** | `‰PNG\r\n\x1A\n` | `89 50 4E 47 0D 0A 1A 0A` |
| **WebP** | `RIFF....WEBP` | `52 49 46 46 ... 57 45 42 50` |
| **PDF** | `%PDF` | `25 50 44 46` |

**Implementation**:
```java
public boolean validateMagicBytes(MultipartFile file, String expectedType) throws IOException {
    byte[] header = new byte[12];
    file.getInputStream().read(header);

    switch (expectedType) {
        case "image/jpeg":
            return header[0] == (byte) 0xFF &&
                   header[1] == (byte) 0xD8 &&
                   header[2] == (byte) 0xFF;

        case "image/png":
            return header[0] == (byte) 0x89 &&
                   header[1] == 0x50 &&
                   header[2] == 0x4E &&
                   header[3] == 0x47;

        case "image/webp":
            return header[8] == 0x57 &&
                   header[9] == 0x45 &&
                   header[10] == 0x42 &&
                   header[11] == 0x50;

        case "application/pdf":
            return header[0] == 0x25 &&
                   header[1] == 0x50 &&
                   header[2] == 0x44 &&
                   header[3] == 0x46;

        default:
            return false;
    }
}
```

### File Size Limits

**Current Limits**:
| File Type | Max Size | Enforced By |
|-----------|----------|-------------|
| Project Images | 10 MB | Spring Boot config |
| CV Documents | 10 MB | Spring Boot config |

**Configuration** (application.yml):
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

**Backend Validation**:
```java
public void validateFileSize(MultipartFile file, long maxSizeBytes) {
    if (file.getSize() > maxSizeBytes) {
        throw new FileSizeLimitExceededException(
            "File size exceeds maximum allowed size: " + maxSizeBytes + " bytes"
        );
    }
}
```

### Path Traversal Prevention

**Vulnerable Code** (DO NOT USE):
```java
// DANGEROUS: User can specify path like "../../etc/passwd"
String filename = request.getParameter("filename");
Path path = Paths.get("uploads/" + filename);
Files.write(path, data);
```

**Secure Code** (USED):
```java
// SAFE: Sanitize filename and use absolute paths
String sanitizedFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
Path targetPath = uploadDir.resolve(sanitizedFilename).normalize();

// Verify target path is within upload directory
if (!targetPath.startsWith(uploadDir)) {
    throw new SecurityException("Invalid file path");
}

Files.write(targetPath, data);
```

---

## Related Documentation

- [API: Files](../api/files.md) - File upload API
- [Features: Image Processing](./image-processing.md) - Image optimization
- [Features: CV Management](./cv-management.md) - CV storage
- [Deployment: CI/CD](../deployment/ci-cd.md) - Deployment pipeline
