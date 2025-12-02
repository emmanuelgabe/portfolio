# Site Configuration API

REST API endpoints for managing site-wide configuration including identity, hero section, SEO metadata, and social links.

## Table of Contents

- [Overview](#overview)
- [Public Endpoints](#public-endpoints)
- [Admin Endpoints](#admin-endpoints)
- [Data Models](#data-models)
- [Error Handling](#error-handling)

---

## Overview

The Site Configuration API manages all configurable site settings:

- **Personal Identity**: Full name and email
- **Hero Section**: Landing page title and description
- **SEO Metadata**: Site title and meta description
- **Social Links**: GitHub and LinkedIn URLs
- **Profile Image**: Profile photo upload and management

**Base URLs:**
- Public: `/api/configuration`
- Admin: `/api/admin/configuration`

**Authentication:**
- Public endpoints: No authentication required
- Admin endpoints: JWT Bearer token with ADMIN role

---

## Public Endpoints

### Get Site Configuration

Retrieves the current site configuration for public display.

```
GET /api/configuration
```

**Response:** `200 OK`

```json
{
  "id": 1,
  "fullName": "Emmanuel Gabe",
  "email": "contact@emmanuelgabe.com",
  "heroTitle": "Developpeur Backend",
  "heroDescription": "Je cree des applications web modernes et performantes.",
  "siteTitle": "Portfolio - Emmanuel Gabe",
  "seoDescription": "Portfolio de Emmanuel Gabe, developpeur backend specialise en Java et Spring Boot.",
  "profileImageUrl": "/uploads/profile/profile.webp",
  "githubUrl": "https://github.com/emmanuelgabe",
  "linkedinUrl": "https://linkedin.com/in/egabe",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-06-20T14:45:00"
}
```

**cURL Example:**

```bash
curl -X GET http://localhost:8080/api/configuration
```

**JavaScript Example:**

```javascript
const response = await fetch('/api/configuration');
const config = await response.json();
```

---

## Admin Endpoints

All admin endpoints require authentication with JWT Bearer token.

### Get Site Configuration (Admin)

Retrieves the site configuration for admin editing.

```
GET /api/admin/configuration
```

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Response:** `200 OK`

Returns the same structure as the public endpoint.

---

### Update Site Configuration

Updates the site configuration fields.

```
PUT /api/admin/configuration
```

**Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body:**

```json
{
  "fullName": "Emmanuel Gabe",
  "email": "contact@emmanuelgabe.com",
  "heroTitle": "Developpeur Backend",
  "heroDescription": "Je cree des applications web modernes et performantes.",
  "siteTitle": "Portfolio - Emmanuel Gabe",
  "seoDescription": "Portfolio de Emmanuel Gabe, developpeur backend.",
  "githubUrl": "https://github.com/emmanuelgabe",
  "linkedinUrl": "https://linkedin.com/in/egabe"
}
```

**Response:** `200 OK`

```json
{
  "id": 1,
  "fullName": "Emmanuel Gabe",
  "email": "contact@emmanuelgabe.com",
  "heroTitle": "Developpeur Backend",
  "heroDescription": "Je cree des applications web modernes et performantes.",
  "siteTitle": "Portfolio - Emmanuel Gabe",
  "seoDescription": "Portfolio de Emmanuel Gabe, developpeur backend.",
  "profileImageUrl": "/uploads/profile/profile.webp",
  "githubUrl": "https://github.com/emmanuelgabe",
  "linkedinUrl": "https://linkedin.com/in/egabe",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-06-20T15:00:00"
}
```

**cURL Example:**

```bash
curl -X PUT http://localhost:8080/api/admin/configuration \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Emmanuel Gabe",
    "email": "contact@emmanuelgabe.com",
    "heroTitle": "Developpeur Backend",
    "heroDescription": "Je cree des applications web modernes.",
    "siteTitle": "Portfolio - Emmanuel Gabe",
    "seoDescription": "Portfolio de Emmanuel Gabe.",
    "githubUrl": "https://github.com/emmanuelgabe",
    "linkedinUrl": "https://linkedin.com/in/egabe"
  }'
```

---

### Upload Profile Image

Uploads or replaces the profile image. Images are automatically converted to WebP format.

```
POST /api/admin/configuration/profile-image
```

**Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: multipart/form-data
```

**Request Body:**
- `file`: Image file (JPEG, PNG, GIF, WebP)

**Constraints:**
- Maximum file size: 5MB
- Allowed formats: JPEG, PNG, GIF, WebP
- Output format: WebP (automatic conversion)

**Response:** `200 OK`

```json
{
  "id": 1,
  "fullName": "Emmanuel Gabe",
  "email": "contact@emmanuelgabe.com",
  "heroTitle": "Developpeur Backend",
  "heroDescription": "Je cree des applications web modernes.",
  "siteTitle": "Portfolio - Emmanuel Gabe",
  "seoDescription": "Portfolio de Emmanuel Gabe.",
  "profileImageUrl": "/uploads/profile/profile.webp",
  "githubUrl": "https://github.com/emmanuelgabe",
  "linkedinUrl": "https://linkedin.com/in/egabe",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-06-20T15:10:00"
}
```

**cURL Example:**

```bash
curl -X POST http://localhost:8080/api/admin/configuration/profile-image \
  -H "Authorization: Bearer <jwt_token>" \
  -F "file=@/path/to/profile.jpg"
```

---

### Delete Profile Image

Removes the current profile image.

```
DELETE /api/admin/configuration/profile-image
```

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Response:** `200 OK`

Returns the updated configuration with `profileImageUrl` set to `null`.

**cURL Example:**

```bash
curl -X DELETE http://localhost:8080/api/admin/configuration/profile-image \
  -H "Authorization: Bearer <jwt_token>"
```

---

## Data Models

### SiteConfigurationResponse

Response model for site configuration.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Configuration ID (always 1) |
| `fullName` | String | Full name displayed on site |
| `email` | String | Contact email address |
| `heroTitle` | String | Hero section title |
| `heroDescription` | String | Hero section description |
| `siteTitle` | String | Browser tab title |
| `seoDescription` | String | Meta description for SEO |
| `profileImageUrl` | String | Profile image URL (nullable) |
| `githubUrl` | String | GitHub profile URL |
| `linkedinUrl` | String | LinkedIn profile URL |
| `createdAt` | String | ISO 8601 creation timestamp |
| `updatedAt` | String | ISO 8601 last update timestamp |

### UpdateSiteConfigurationRequest

Request model for updating site configuration.

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `fullName` | String | Yes | 2-100 characters |
| `email` | String | Yes | Valid email, max 255 characters |
| `heroTitle` | String | Yes | 3-200 characters |
| `heroDescription` | String | Yes | 10-1000 characters |
| `siteTitle` | String | Yes | 3-100 characters |
| `seoDescription` | String | Yes | 10-300 characters |
| `githubUrl` | String | Yes | Valid URL, max 500 characters |
| `linkedinUrl` | String | Yes | Valid URL, max 500 characters |

---

## Error Handling

### Error Response Format

```json
{
  "error": "Error code",
  "message": "Human-readable error message",
  "timestamp": "2024-06-20T15:00:00"
}
```

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `CONFIG_001` | 404 | Site configuration not found |
| `CONFIG_002` | 400 | Invalid configuration data |
| `CONFIG_003` | 400 | Profile image upload failed |
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `UNAUTHORIZED` | 401 | Missing or invalid JWT token |
| `FORBIDDEN` | 403 | Insufficient permissions |

### Validation Errors

When validation fails, the response includes field-specific errors:

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "errors": {
    "fullName": "Full name must be between 2 and 100 characters",
    "email": "Email must be valid"
  },
  "timestamp": "2024-06-20T15:00:00"
}
```

---

## Related Documentation

- [Site Configuration Feature](../features/site-configuration.md) - Architecture and implementation details
- [Image Processing](../features/image-processing.md) - WebP conversion and optimization
- [Authentication API](./authentication.md) - JWT token management
