# Blog Articles Feature

---

## Table of Contents
1. [Overview](#1-overview)
2. [Article Structure](#2-article-structure)
3. [Markdown Rendering](#3-markdown-rendering)
4. [Draft and Publish Workflow](#4-draft-and-publish-workflow)
5. [Automatic Content Processing](#5-automatic-content-processing)
6. [Tag Categorization](#6-tag-categorization)
7. [Image Management](#7-image-management)
8. [Pagination Support](#8-pagination-support)
9. [Configuration](#9-configuration)

---

## 1. Overview

The Blog Articles feature provides a comprehensive content management system for publishing technical articles and blog posts. Built with Markdown support and GitHub Flavored Markdown extensions.

**Key Capabilities**:
- Markdown-based content authoring with GitHub Flavored Markdown extensions
- Draft/Publish workflow for content management
- Automatic slug generation from titles
- Reading time calculation (200 words/minute)
- Excerpt generation from Markdown content
- Tag-based categorization
- Embedded image management
- Pagination for large article collections
- Author attribution

**Public Access**:
- View published articles
- Filter articles by tags
- Paginated article listing

**Admin Access**:
- Create, update, and delete articles
- Publish and unpublish articles
- Manage article drafts
- Upload and manage images within articles

---

## 2. Article Structure

### Entity Model

**Article Entity** (`com.emmanuelgabe.portfolio.entity.Article`):

```java
@Entity
@Table(name = "articles")
public class Article {
    private Long id;
    private String title;              // 3-200 characters
    private String slug;                // 3-200 characters, unique, URL-friendly
    private String content;             // Markdown format
    private String excerpt;             // Max 500 characters
    private User author;                // ManyToOne relationship
    private boolean draft;              // True for drafts, false for published
    private LocalDateTime publishedAt;  // Publication timestamp
    private Integer readingTimeMinutes; // Auto-calculated
    private Set<Tag> tags;              // ManyToMany relationship
    private Set<ArticleImage> images;   // OneToMany relationship
}
```

### Field Details

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| `id` | Long | Primary key | Auto-generated |
| `title` | String | Article title | Required, 3-200 chars |
| `slug` | String | URL-friendly identifier | Required, 3-200 chars, unique |
| `content` | String | Markdown content | Required |
| `excerpt` | String | Short summary | Optional, max 500 chars |
| `author` | User | Article author | Required, ManyToOne |
| `draft` | Boolean | Draft status | Default: true |
| `publishedAt` | LocalDateTime | Publication timestamp | Set on first publish |
| `readingTimeMinutes` | Integer | Estimated reading time | Auto-calculated |
| `tags` | Set<Tag> | Associated tags | ManyToMany |
| `images` | Set<ArticleImage> | Embedded images | OneToMany, cascade |

---

## 3. Markdown Rendering

### Flexmark Integration

The system uses **Flexmark** library for Markdown-to-HTML conversion with full GitHub Flavored Markdown (GFM) support.

**MarkdownService** (`com.emmanuelgabe.portfolio.service.MarkdownService`):

```java
@Service
public class MarkdownService {
    public String renderToHtml(String markdown);
    public String extractExcerpt(String markdown, int maxLength);
}
```

### Supported GitHub Flavored Markdown Extensions

1. **Tables** (`TablesExtension`):
```markdown
| Header 1 | Header 2 |
|----------|----------|
| Cell 1   | Cell 2   |
```

2. **Strikethrough** (`StrikethroughExtension`):
```markdown
~~strikethrough text~~
```

3. **Task Lists** (`TaskListExtension`):
```markdown
- [x] Completed task
- [ ] Pending task
```

4. **Autolinks** (`AutolinkExtension`):
```markdown
https://example.com automatically becomes a link
```

### Rendering Configuration

```java
MutableDataSet options = new MutableDataSet()
    .set(Parser.EXTENSIONS, Arrays.asList(
        TablesExtension.create(),
        StrikethroughExtension.create(),
        TaskListExtension.create(),
        AutolinkExtension.create()
    ))
    .set(HtmlRenderer.SOFT_BREAK, "<br />\n");
```

### Excerpt Generation

```java
public String extractExcerpt(String markdown, int maxLength) {
    String html = renderToHtml(markdown);
    String text = html.replaceAll("<[^>]*>", "");  // Strip HTML tags
    text = text.trim().replaceAll("\\s+", " ");    // Normalize whitespace
    if (text.length() <= maxLength) return text;
    return text.substring(0, maxLength).trim() + "...";
}
```

---

## 4. Draft and Publish Workflow

### Draft State

**Default State**: All new articles start as drafts (`draft = true`)

**Characteristics**:
- Not visible to public users
- Accessible only to admins
- Can be edited without affecting published content
- No publication timestamp

### Publishing

**Endpoint**: `PUT /api/articles/admin/{id}/publish`

**Process**:
```java
public void publish() {
    this.draft = false;
    if (this.publishedAt == null) {
        this.publishedAt = LocalDateTime.now();
    }
}
```

**Effects**:
- Sets `draft = false`
- Records `publishedAt` timestamp (only on first publish)
- Makes article visible to public users
- Article appears in public listings

### Unpublishing

**Endpoint**: `PUT /api/articles/admin/{id}/unpublish`

**Process**:
```java
public void unpublish() {
    this.draft = true;
}
```

**Effects**:
- Sets `draft = true`
- Retains original `publishedAt` timestamp
- Hides article from public users
- Article remains accessible to admins

### Publication Check

```java
public boolean isPublished() {
    return !this.draft
        && this.publishedAt != null
        && !this.publishedAt.isAfter(LocalDateTime.now());
}
```

**Validation**:
- Must not be a draft
- Must have a publication timestamp
- Publication time must not be in the future (supports scheduled publishing)

---

## 5. Automatic Content Processing

### Slug Generation

**Trigger**: Automatically called when slug is null or blank

**Algorithm**:
```java
public void generateSlug() {
    if (this.slug == null || this.slug.isBlank()) {
        this.slug = this.title
            .toLowerCase()
            .trim()
            .replaceAll("[^a-z0-9\\s-]", "")  // Remove special chars
            .replaceAll("\\s+", "-")           // Replace spaces with hyphens
            .replaceAll("-+", "-")              // Collapse multiple hyphens
            .replaceAll("^-|-$", "");           // Remove leading/trailing hyphens
    }
}
```

**Examples**:
- `"Introduction to Spring Boot"` → `"introduction-to-spring-boot"`
- `"Java 21: New Features!"` → `"java-21-new-features"`
- `"How to Build APIs  with REST"` → `"how-to-build-apis-with-rest"`

### Reading Time Calculation

**Trigger**: Automatically called on article creation and updates

**Algorithm**:
```java
public void calculateReadingTime() {
    if (this.content != null && !this.content.isBlank()) {
        int wordCount = this.content.split("\\s+").length;
        this.readingTimeMinutes = Math.max(1, (int) Math.ceil(wordCount / 200.0));
    }
}
```

**Assumptions**:
- Average reading speed: 200 words per minute
- Minimum reading time: 1 minute
- Word count calculated from Markdown content (includes code blocks)

**Examples**:
- 150 words → 1 minute
- 400 words → 2 minutes
- 1000 words → 5 minutes

---

## 6. Tag Categorization

### Many-to-Many Relationship

**Join Table**: `article_tags`

**Columns**:
- `article_id` (FK → articles.id)
- `tag_id` (FK → tags.id)

### Tag Operations

**Adding Tags**:
```java
public void addTag(Tag tag) {
    this.tags.add(tag);
}
```

**Removing Tags**:
```java
public void removeTag(Tag tag) {
    this.tags.remove(tag);
}
```

### Cascade Behavior

**Article Creation/Update**:
- `CascadeType.PERSIST`: Tags are persisted when article is created
- `CascadeType.MERGE`: Tags are merged when article is updated

**Article Deletion**:
- Tags are NOT deleted (only relationship removed)
- Tags can be reused across multiple articles

**Tag Deletion**:
- Article-tag relationships are removed
- Articles remain intact

---

## 7. Image Management

### ArticleImage Entity

**Relationship**: OneToMany with cascade and orphan removal

**Entity Listener**: `ArticleEntityListener`

**Lifecycle Hook** (`@PreRemove`):
```java
@PreRemove
public void preRemove(Article article) {
    if (article.getImages() != null && !article.getImages().isEmpty()) {
        for (ArticleImage image : article.getImages()) {
            imageService.deleteArticleImage(image.getImageUrl());
        }
    }
}
```

### Automatic Cleanup

**When Article is Deleted**:
1. `@PreRemove` hook triggered
2. All associated `ArticleImage` records fetched
3. Each image file deleted from filesystem via `ImageService`
4. Database records removed via orphan removal

**Benefits**:
- Prevents orphaned image files
- Automatic storage management
- No manual cleanup required

### Image Operations

**Adding Images**:
```java
public void addImage(ArticleImage image) {
    this.images.add(image);
    image.setArticle(this);  // Maintain bidirectional relationship
}
```

**Removing Images**:
```java
public void removeImage(ArticleImage image) {
    this.images.remove(image);
    image.setArticle(null);  // Break bidirectional relationship
}
```

---

## 8. Pagination Support

### Paginated Endpoints

**Public Endpoint**:
```
GET /api/articles/paginated?page=0&size=10&sort=publishedAt,desc
```

**Admin Endpoint**:
```
GET /api/articles/admin/paginated?page=0&size=10&sort=publishedAt,desc
```

### Default Configuration

```java
@PageableDefault(size = 10, sort = "publishedAt")
Pageable pageable
```

**Defaults**:
- Page number: 0 (first page)
- Page size: 10 articles
- Sort field: `publishedAt`
- Sort direction: Descending (newest first)

### Response Structure

```json
{
  "content": [
    {
      "id": 1,
      "title": "Article Title",
      "slug": "article-title",
      "excerpt": "Article summary...",
      "publishedAt": "2025-11-19T10:00:00",
      "readingTimeMinutes": 5,
      "tags": [...]
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {...}
  },
  "totalElements": 45,
  "totalPages": 5,
  "last": false,
  "first": true,
  "numberOfElements": 10
}
```

### Query Parameters

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `page` | integer | Page number (zero-indexed) | `page=0` |
| `size` | integer | Number of articles per page | `size=20` |
| `sort` | string | Sort field and direction | `sort=title,asc` |

---

## 9. Configuration

### Flexmark Configuration

**Library**: `com.vladsch.flexmark:flexmark-all`

**Extensions**:
```xml
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark-all</artifactId>
    <version>0.64.8</version>
</dependency>
```

### Entity Listeners

**Registration**:
```java
@EntityListeners(ArticleEntityListener.class)
public class Article { ... }
```

**Component Injection**:
```java
@Component
public class ArticleEntityListener {
    @Autowired
    public void setImageService(@Lazy ImageService imageService) {
        this.imageService = imageService;
    }
}
```

**Note**: `@Lazy` injection prevents circular dependency issues during entity initialization.

---

## Related Documentation

- [Articles API](../api/articles-api.md) - Complete API reference
- [Tags API](../api/tags.md) - Tag management
- [Features: Image Processing](./image-processing.md) - Image upload and optimization
- [Architecture: Database Schema](../architecture/database-schema.md) - Database structure
