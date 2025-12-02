# Article Images API

REST API endpoints for managing images associated with blog articles.

---

## Table of Contents

1. [Overview](#overview)
2. [Admin Endpoints](#admin-endpoints)
3. [Data Models](#data-models)
4. [Error Codes](#error-codes)

---

## Overview

The Article Images API provides endpoints for uploading and managing images within blog articles. Images are automatically processed (WebP conversion, thumbnail generation) upon upload.

**Base URL:** `/api/admin/articles`

**Authentication:** All endpoints require ADMIN role.

---

## Admin Endpoints

### Upload Article Image

Uploads an image for a specific article.

```
POST /api/admin/articles/{id}/images
```

**Authentication:** Required (ADMIN role)

**Headers:**
```
Authorization: Bearer <access_token>
Content-Type: multipart/form-data
```

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Article ID |

**Request Body:**

| Field | Type | Description |
|-------|------|-------------|
| `file` | MultipartFile | Image file (JPEG, PNG, WebP) |

**Example (cURL):**

```bash
curl -X POST "http://localhost:8080/api/admin/articles/1/images" \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/image.jpg"
```

**Response:**

```json
{
  "id": 1,
  "imageUrl": "/uploads/articles/1/abc123.webp",
  "thumbnailUrl": "/uploads/articles/1/abc123_thumb.webp",
  "uploadedAt": "2024-06-20T14:45:00"
}
```

**Status Codes:**

| Code | Description |
|------|-------------|
| 201 | Image uploaded successfully |
| 400 | Invalid file format or size |
| 401 | Unauthorized |
| 403 | Forbidden (not admin) |
| 404 | Article not found |

---

### Delete Article Image

Removes an image from an article.

```
DELETE /api/admin/articles/{articleId}/images/{imageId}
```

**Authentication:** Required (ADMIN role)

**Headers:**
```
Authorization: Bearer <access_token>
```

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `articleId` | Long | Article ID |
| `imageId` | Long | Image ID |

**Response:** No content (204)

**Status Codes:**

| Code | Description |
|------|-------------|
| 204 | Image deleted successfully |
| 401 | Unauthorized |
| 403 | Forbidden (not admin) |
| 404 | Article or image not found |

---

## Data Models

### ArticleImageResponse

```json
{
  "id": "Long",
  "imageUrl": "String",
  "thumbnailUrl": "String",
  "uploadedAt": "LocalDateTime"
}
```

### Image Processing

Uploaded images are automatically processed:

| Process | Description |
|---------|-------------|
| Format conversion | Converted to WebP |
| Thumbnail generation | 300x300px thumbnail created |
| Quality optimization | 80% quality setting |

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| IMG_001 | Article not found | Specified article does not exist |
| IMG_002 | Image not found | Specified image does not exist |
| IMG_003 | Invalid file type | File is not a valid image format |
| IMG_004 | File too large | File exceeds maximum size limit |
| IMG_005 | Upload failed | Server error during upload |

---

## Related Documentation

- [Articles API](./articles-api.md) - Blog articles endpoints
- [Image Processing](../features/image-processing.md) - Image processing feature
- [File Storage](../features/file-storage.md) - File storage architecture
