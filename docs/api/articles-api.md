# Articles API Documentation

REST API endpoints for managing blog articles with Markdown content, draft/publish workflow, tags, and images.

---

## Base URL

```
/api/articles
```

---

## Public Endpoints

### Get All Published Articles

Retrieves all published articles visible to the public.

**Endpoint:** `GET /api/articles`

**Authentication:** None required

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "title": "Getting Started with Spring Boot",
    "slug": "getting-started-with-spring-boot",
    "content": "# Introduction\n\nThis is a comprehensive guide...",
    "contentHtml": "<h1>Introduction</h1><p>This is a comprehensive guide...</p>",
    "excerpt": "Learn how to build REST APIs with Spring Boot",
    "authorName": "Admin",
    "draft": false,
    "publishedAt": "2025-01-15T10:30:00",
    "createdAt": "2025-01-10T14:20:00",
    "updatedAt": "2025-01-15T10:30:00",
    "viewsCount": 142,
    "readingTimeMinutes": 8,
    "tags": [
      {
        "id": 1,
        "name": "Spring Boot",
        "slug": "spring-boot"
      }
    ],
    "images": [
      {
        "id": 1,
        "imageUrl": "/uploads/articles/article-1-image-abc123.webp",
        "thumbnailUrl": "/uploads/articles/article-1-thumb-abc123.webp",
        "uploadedAt": "2025-01-10T15:00:00"
      }
    ]
  }
]
```

---

### Get Published Articles (Paginated)

Retrieves published articles with pagination support.

**Endpoint:** `GET /api/articles/paginated`

**Authentication:** None required

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (zero-indexed) |
| `size` | integer | 10 | Number of articles per page |
| `sort` | string | publishedAt,desc | Sort field and direction |

**Example:** `GET /api/articles/paginated?page=0&size=5&sort=publishedAt,desc`

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "title": "Article Title",
      "slug": "article-title",
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 5,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 3,
  "totalElements": 15,
  "last": false,
  "first": true,
  "size": 5,
  "number": 0,
  "numberOfElements": 5,
  "empty": false
}
```

---

### Get Article by Slug

Retrieves a single published article by its URL-friendly slug. Increments view count.

**Endpoint:** `GET /api/articles/{slug}`

**Authentication:** None required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `slug` | string | URL-friendly article identifier |

**Example:** `GET /api/articles/getting-started-with-spring-boot`

**Response:** `200 OK`

```json
{
  "id": 1,
  "title": "Getting Started with Spring Boot",
  "slug": "getting-started-with-spring-boot",
  "content": "# Introduction\n\nThis is markdown content...",
  "contentHtml": "<h1>Introduction</h1><p>This is markdown content...</p>",
  "excerpt": "Learn Spring Boot basics",
  "authorName": "Admin",
  "draft": false,
  "publishedAt": "2025-01-15T10:30:00",
  "createdAt": "2025-01-10T14:20:00",
  "updatedAt": "2025-01-15T10:30:00",
  "viewsCount": 143,
  "readingTimeMinutes": 8,
  "tags": [],
  "images": []
}
```

**Error Response:** `404 Not Found`

```json
{
  "status": 404,
  "message": "Article not found with slug: nonexistent-slug",
  "timestamp": "2025-01-20T10:30:00"
}
```

---

## Admin Endpoints

**Authentication:** Required (ADMIN role)

**Headers:**
```
Authorization: Bearer <jwt_token>
```

---

### Get All Articles (Admin)

Retrieves all articles including drafts.

**Endpoint:** `GET /api/articles/admin`

**Authentication:** ADMIN required

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "title": "Published Article",
    "draft": false,
    ...
  },
  {
    "id": 2,
    "title": "Draft Article",
    "draft": true,
    "publishedAt": null,
    ...
  }
]
```

**Error Response:** `403 Forbidden`

---

### Get All Articles Paginated (Admin)

Retrieves all articles with pagination (admin only).

**Endpoint:** `GET /api/articles/admin/paginated`

**Authentication:** ADMIN required

**Query Parameters:** Same as public paginated endpoint

**Response:** `200 OK` (Page object with all articles including drafts)

---

### Get Article by ID (Admin)

Retrieves an article by ID, including draft status.

**Endpoint:** `GET /api/articles/admin/{id}`

**Authentication:** ADMIN required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Article database ID |

**Response:** `200 OK`

**Error Response:** `404 Not Found`

---

### Create Article

Creates a new article (draft by default).

**Endpoint:** `POST /api/articles/admin`

**Authentication:** ADMIN required

**Request Body:**

```json
{
  "title": "New Article Title",
  "content": "# Markdown Content\n\nThis is the article body...",
  "excerpt": "Short description for previews (optional)",
  "tagIds": [1, 2, 3],
  "draft": true
}
```

**Field Validation:**

| Field | Required | Constraints |
|-------|----------|-------------|
| `title` | Yes | 3-200 characters |
| `content` | Yes | Not blank |
| `excerpt` | No | Max 500 characters |
| `tagIds` | No | Valid tag IDs |
| `draft` | No | Default: true |

**Response:** `201 Created`

```json
{
  "id": 5,
  "title": "New Article Title",
  "slug": "new-article-title",
  "content": "# Markdown Content...",
  "contentHtml": "<h1>Markdown Content</h1>...",
  "excerpt": "Short description",
  "authorName": "Admin",
  "draft": true,
  "publishedAt": null,
  "createdAt": "2025-01-20T11:00:00",
  "updatedAt": "2025-01-20T11:00:00",
  "viewsCount": 0,
  "readingTimeMinutes": 2,
  "tags": [
    {"id": 1, "name": "Tag1", "slug": "tag1"}
  ],
  "images": []
}
```

**Error Response:** `400 Bad Request`

```json
{
  "status": 400,
  "message": "Title is required",
  "timestamp": "2025-01-20T11:00:00"
}
```

---

### Update Article

Updates an existing article. Only provided fields are updated.

**Endpoint:** `PUT /api/articles/admin/{id}`

**Authentication:** ADMIN required

**Request Body:**

```json
{
  "title": "Updated Title",
  "content": "Updated markdown content",
  "excerpt": "Updated excerpt",
  "tagIds": [1, 2],
  "draft": false
}
```

**Note:** All fields are optional. Only provided fields will be updated.

**Response:** `200 OK`

```json
{
  "id": 5,
  "title": "Updated Title",
  "slug": "updated-title",
  ...
}
```

**Error Response:** `404 Not Found`

---

### Delete Article

Permanently deletes an article and all associated images from filesystem.

**Endpoint:** `DELETE /api/articles/admin/{id}`

**Authentication:** ADMIN required

**Response:** `204 No Content`

**Error Response:** `404 Not Found`

**Note:** This action is irreversible. All article images are automatically deleted from the filesystem via `@PreRemove` listener.

---

### Publish Article

Publishes a draft article (sets `draft=false` and `publishedAt=now`).

**Endpoint:** `PUT /api/articles/admin/{id}/publish`

**Authentication:** ADMIN required

**Response:** `200 OK`

```json
{
  "id": 5,
  "title": "Article Title",
  "draft": false,
  "publishedAt": "2025-01-20T12:00:00",
  ...
}
```

---

### Unpublish Article

Unpublishes an article (sets `draft=true`).

**Endpoint:** `PUT /api/articles/admin/{id}/unpublish`

**Authentication:** ADMIN required

**Response:** `200 OK`

```json
{
  "id": 5,
  "title": "Article Title",
  "draft": true,
  "publishedAt": "2025-01-20T12:00:00",
  ...
}
```

---

## Article Image Management

### Upload Article Image

Uploads an image file to an article.

**Endpoint:** `POST /api/admin/articles/{id}/images`

**Authentication:** ADMIN required

**Request:** `multipart/form-data`

**Form Data:**

| Field | Type | Description |
|-------|------|-------------|
| `file` | File | Image file (JPEG, PNG, WebP) |

**Example:**
```bash
curl -X POST \
  -H "Authorization: Bearer <token>" \
  -F "file=@image.jpg" \
  http://localhost:8080/api/admin/articles/1/images
```

**Response:** `201 Created`

```json
{
  "id": 3,
  "imageUrl": "/uploads/articles/article-1-image-abc123.webp",
  "thumbnailUrl": "/uploads/articles/article-1-thumb-abc123.webp",
  "uploadedAt": "2025-01-20T13:00:00"
}
```

**Image Processing:**
- Converted to WebP format
- Optimized to max width 1200px
- Thumbnail generated (300x300px crop)
- Magic bytes validation (JPEG, PNG, WebP)
- Path traversal protection

**Error Response:** `400 Bad Request` - Invalid file format

---

### Delete Article Image

Deletes an image from an article and removes file from filesystem.

**Endpoint:** `DELETE /api/admin/articles/{articleId}/images/{imageId}`

**Authentication:** ADMIN required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `articleId` | integer | Article ID |
| `imageId` | integer | Image ID |

**Response:** `204 No Content`

**Error Response:** `404 Not Found`

---

## Data Models

### ArticleResponse

Complete article data with rendered HTML.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Article unique identifier |
| `title` | String | Article title (3-200 chars) |
| `slug` | String | URL-friendly identifier |
| `content` | String | Markdown source content |
| `contentHtml` | String | Rendered HTML from Markdown |
| `excerpt` | String | Short description (max 500 chars) |
| `authorName` | String | Author display name |
| `draft` | Boolean | Draft status (true = unpublished) |
| `publishedAt` | DateTime | Publication timestamp (null if draft) |
| `createdAt` | DateTime | Creation timestamp |
| `updatedAt` | DateTime | Last update timestamp |
| `viewsCount` | Integer | Number of views |
| `readingTimeMinutes` | Integer | Estimated reading time |
| `tags` | Array[Tag] | Associated tags |
| `images` | Array[Image] | Uploaded images |

---

## Business Rules

### Slug Generation
- Auto-generated from title (lowercase, hyphenated)
- Unique constraint enforced
- Collision resolution: append UUID suffix (8 chars)

### Reading Time Calculation
- Based on average reading speed: 200 words/minute
- Calculated automatically on create/update

### Publication Workflow
1. **Create** - Article created as draft (`draft=true`)
2. **Edit** - Update content while draft
3. **Publish** - Sets `draft=false`, `publishedAt=now()`
4. **Unpublish** - Sets `draft=true` (keeps `publishedAt`)

### View Counter
- Incremented automatically when article accessed via slug
- Only counts public access (GET `/api/articles/{slug}`)
- Not incremented for admin access

### Image Cleanup
- Images automatically deleted when article is deleted
- Handled by `ArticleEntityListener.@PreRemove`
- Deletes both optimized image and thumbnail

---

## Markdown Support

### Supported Extensions (Flexmark GFM)
- **Tables** - GitHub-style tables
- **Strikethrough** - `~~text~~`
- **Task Lists** - `- [ ] task` / `- [x] done`
- **Autolinks** - Automatic URL detection

### Security
- XSS protection via Flexmark sanitization
- No inline HTML execution
- Safe rendering of user content

---

## Error Responses

All errors follow consistent format:

```json
{
  "status": 404,
  "message": "Article not found with ID: 999",
  "timestamp": "2025-01-20T14:00:00"
}
```

**Common Status Codes:**
- `200 OK` - Success
- `201 Created` - Resource created
- `204 No Content` - Success with no response body
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Missing/invalid JWT
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## Examples

### Create and Publish Article

```bash
# 1. Create draft
POST /api/articles/admin
{
  "title": "Spring Security Guide",
  "content": "# Introduction\n\nSecurity basics...",
  "tagIds": [1, 2],
  "draft": true
}

# 2. Upload images
POST /api/admin/articles/5/images
-F "file=@diagram.png"

# 3. Publish
PUT /api/articles/admin/5/publish
```

### Paginated Public List

```bash
GET /api/articles/paginated?page=0&size=10&sort=publishedAt,desc
```

### Admin Dashboard

```bash
# Get all articles (drafts + published) with pagination
GET /api/articles/admin/paginated?page=0&size=20&sort=updatedAt,desc
```

---

**API Version:** 1.0
**Last Updated:** 2025-01-20
**Base Path:** `/api/articles`
**Swagger UI:** `http://localhost:8080/swagger-ui.html`
