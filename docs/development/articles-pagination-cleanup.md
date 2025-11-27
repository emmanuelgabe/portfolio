# Articles - Pagination & Image Cleanup

---

## Table of Contents

1. [Overview](#overview)
2. [Pagination Implementation](#pagination-implementation)
3. [Automatic Image Cleanup](#automatic-image-cleanup)
4. [Configuration](#configuration)

---

## Overview

The Article feature provides a complete blog system with:
- Markdown content with GFM extensions (tables, strikethrough, task lists, autolinks)
- Draft/Publish workflow
- Tag categorization
- Image uploads with automatic WebP optimization
- Pagination support
- Automatic cleanup of orphaned images on article deletion

**Technology Stack**:
- Backend: Spring Boot 3.x, Spring Data JPA
- Markdown: Commonmark-java with GFM extensions
- Image Processing: Thumbnailator library
- Database: PostgreSQL

---

## Pagination Implementation

### Repository Layer

**ArticleRepository.java**:
```java
@Query("SELECT a FROM Article a WHERE a.draft = false AND a.publishedAt <= :now ORDER BY a.publishedAt DESC")
Page<Article> findPublished(@Param("now") LocalDateTime now, Pageable pageable);

Page<Article> findAllByOrderByPublishedAtDesc(Pageable pageable);
```

### Service Layer

**ArticleService.java**:
```java
Page<ArticleResponse> getAllPublishedPaginated(Pageable pageable);
Page<ArticleResponse> getAllArticlesPaginated(Pageable pageable);
```

### Controller Endpoints

**ArticleController.java**:

1. **GET `/api/articles/paginated`** - Public endpoint
   - Query params: `page` (default: 0), `size` (default: 10), `sort` (default: publishedAt,desc)
   - Returns: `Page<ArticleResponse>` with published articles only

2. **GET `/api/articles/admin/paginated`** - Admin endpoint
   - Same pagination parameters
   - Returns: All articles (drafts + published)

### Response Format

```json
{
  "content": [/* array of ArticleResponse */],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0
  },
  "totalPages": 5,
  "totalElements": 42,
  "first": true,
  "last": false,
  "numberOfElements": 10
}
```

### Usage Examples

```bash
# Public: Get first page of published articles
GET /api/articles/paginated?page=0&size=10&sort=publishedAt,desc

# Admin: Get all articles with pagination
GET /api/articles/admin/paginated?page=0&size=20&sort=updatedAt,desc
```

---

## Automatic Image Cleanup

### Entity Listener

**ArticleEntityListener.java**:
```java
@Component
@Slf4j
public class ArticleEntityListener {

    @Autowired
    public void setImageService(@Lazy ImageService imageService) {
        this.imageService = imageService;
    }

    @PreRemove
    public void preRemove(Article article) {
        for (ArticleImage image : article.getImages()) {
            imageService.deleteArticleImage(image.getImageUrl());
        }
    }
}
```

**Article.java**:
```java
@Entity
@EntityListeners(ArticleEntityListener.class)
public class Article {
    // Entity fields
}
```

### How It Works

When an article is deleted:
1. JPA triggers `@PreRemove` callback before database deletion
2. Listener iterates through all associated `ArticleImage` entities
3. Calls `ImageService.deleteArticleImage()` to remove files from filesystem
4. Both optimized image and thumbnail files are deleted
5. Database cascade delete removes `ArticleImage` records
6. Article entity is finally deleted from database

**Note**: Uses `@Lazy` injection to avoid circular dependency during JPA entity initialization.

### Files Deleted

- Optimized image: `article_{articleId}_image_{imageId}.webp`
- Thumbnail: `article_{articleId}_image_{imageId}_thumb.webp`

---

## Configuration

### Pagination Defaults

**ArticleController.java**:
```java
@PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC)
```

**Default Values**:
- Page size: 10 articles
- Page number: 0 (first page)
- Sort: `publishedAt,desc`

### Application Properties

**application.yml**:
```yaml
# Image storage (existing configuration)
storage:
  base-path: /uploads
  upload-dir: ${UPLOAD_DIR:./uploads}
  max-file-size: 10MB
  image-max-width: 1200
  thumbnail-size: 300

# Markdown processing
markdown:
  extensions:
    - tables
    - strikethrough
    - task-lists
    - autolinks
```

### Logging Configuration

**Categories used**:
- `[CREATE_ARTICLE]` - Article creation
- `[UPDATE_ARTICLE]` - Article updates
- `[DELETE_ARTICLE]` - Article deletion
- `[LIST_ARTICLES]` - Article listing
- `[ARTICLE_LIFECYCLE]` - Entity lifecycle events (@PreRemove)
- `[UPLOAD_ARTICLE_IMAGE]` - Image uploads
- `[DELETE_ARTICLE_IMAGE]` - Image deletions

### Security Configuration

**SecurityConfig.java**:
```java
// Public endpoints
http.authorizeHttpRequests(auth -> auth
    .requestMatchers(GET, "/api/articles").permitAll()
    .requestMatchers(GET, "/api/articles/paginated").permitAll()
    .requestMatchers(GET, "/api/articles/{slug}").permitAll()

    // Admin endpoints
    .requestMatchers("/api/articles/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/admin/articles/**").hasRole("ADMIN")
);
```

---

## Related Documentation

- [API: Articles](../api/articles-api.md) - Complete API reference with all endpoints
- [Development: Logging Conventions](./logging-conventions.md) - Logging format
