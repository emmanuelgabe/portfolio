# Image Processing System

---

## Table of Contents
1. [Overview](#1-overview)
2. [WebP Conversion](#2-webp-conversion)
3. [Thumbnail Generation](#3-thumbnail-generation)
4. [Architecture](#4-architecture)
5. [Performance](#5-performance)
6. [Configuration](#6-configuration)

---

## 1. Overview

The Image Processing System automatically converts uploaded project images to WebP format and generates optimized thumbnails.

**Key Features**:
- Automatic WebP conversion (from JPEG, PNG, WebP)
- Thumbnail generation (400x300px)
- Configurable quality settings
- MIME type validation (magic bytes)
- Old image cleanup
- Optimized for web performance

**Technology**: Thumbnailator library (Java)

---

## 2. WebP Conversion

### Why WebP?

**Benefits**:
- 25-35% smaller file size vs JPEG (same quality)
- Supports transparency (like PNG)
- Faster page load times
- Modern browser support (95%+ globally)

**Comparison**:
| Format | File Size (example) | Transparency | Browser Support |
|--------|---------------------|--------------|-----------------|
| JPEG | 500 KB | No | 100% |
| PNG | 800 KB | Yes | 100% |
| **WebP** | **350 KB** | **Yes** | **95%+** |

### Conversion Process

**Flow**:
```
1. User uploads image (JPEG/PNG/WebP)
   ↓
2. MIME type validation (magic bytes)
   ↓
3. Load image with Thumbnailator
   ↓
4. Convert to WebP format
   ↓
5. Apply quality compression (0.85)
   ↓
6. Save as project_{id}_{timestamp}.webp
   ↓
7. Delete old image (if exists)
   ↓
8. Update project.imageUrl in database
```

### Quality Settings

**Full-Size Image**:
- **Quality**: 0.85 (85%)
- **Format**: WebP
- **Naming**: `project_{id}_{timestamp}.webp`

**Thumbnail**:
- **Quality**: 0.8 (80%)
- **Size**: 400x300px (fixed dimensions)
- **Format**: WebP
- **Naming**: `project_{id}_{timestamp}_thumb.webp`

**Quality Scale**:
- 1.0 = Maximum quality (large file)
- 0.85 = High quality (recommended for full images)
- 0.8 = Good quality (recommended for thumbnails)
- 0.5 = Medium quality (noticeable compression)

---

## 3. Thumbnail Generation

### Thumbnail Specifications

**Dimensions**: 400×300px (fixed aspect ratio)

**Resize Mode**: `FIT` (maintains aspect ratio, may add letterboxing)

**Quality**: 0.8 (80%)

**Purpose**:
- Fast loading on list/grid views
- Reduced bandwidth usage
- Better mobile performance

### Resize Modes

**Available Modes**:

1. **FIT** (used in this project):
   - Maintains aspect ratio
   - Fits within 400×300px
   - May result in smaller dimensions
   - No cropping

2. **CROP** (alternative):
   - Forces exact 400×300px
   - Crops excess areas
   - Always fills entire space

**Example**:
```
Original image: 1920×1080px (16:9)

FIT mode:
→ 400×225px (maintains 16:9, fits within 400×300)

CROP mode:
→ 400×300px (crops to 4:3, may lose content)
```

### Use Cases

**Full Image** (`imageUrl`):
- Project detail page
- Lightbox/modal view
- High-quality display

**Thumbnail** (`thumbnailUrl`):
- Project list/grid
- Homepage featured projects
- Mobile views
- Fast previews

---

## 4. Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│                  ProjectController                      │
│                                                         │
│  POST /api/admin/projects/{id}/upload-image            │
│  ├─ Validate multipart file                            │
│  ├─ Call ImageService.uploadProjectImage()             │
│  └─ Return ImageUploadResponse                         │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                   ImageService                          │
│                                                         │
│  uploadProjectImage(projectId, file)                   │
│  ├─ Load project from database                         │
│  ├─ Validate MIME type (magic bytes)                   │
│  ├─ Generate filenames (main + thumbnail)              │
│  ├─ Convert to WebP (quality 0.85)                     │
│  ├─ Generate thumbnail (400×300, quality 0.8)          │
│  ├─ Delete old images (if exist)                       │
│  ├─ Update project imageUrl & thumbnailUrl             │
│  └─ Return URLs                                         │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
┌─────────────────┐      ┌────────────────┐
│ FileStorage     │      │ Thumbnailator  │
│ Service         │      │ Library        │
│                 │      │                │
│ - Store files   │      │ - Resize       │
│ - Delete files  │      │ - Convert      │
│ - Validate      │      │ - Compress     │
└─────────────────┘      └────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│                    File System                          │
│                                                         │
│  uploads/projects/                                      │
│  ├── project_1_1700000000.webp                         │
│  ├── project_1_1700000000_thumb.webp                   │
│  ├── project_2_1700100000.webp                         │
│  └── project_2_1700100000_thumb.webp                   │
└─────────────────────────────────────────────────────────┘
```

### Processing Pipeline

```
┌─────────────────┐
│ Upload JPEG/PNG │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ MIME Validation │ ── Magic bytes check (not extension)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Load Image      │ ── Thumbnailator reads original
└────────┬────────┘
         │
         ├─────────────────────────────────────┐
         ▼                                     ▼
┌──────────────────┐              ┌──────────────────┐
│ Full Image       │              │ Thumbnail        │
│                  │              │                  │
│ - Keep original  │              │ - Resize 400×300 │
│   dimensions     │              │ - Quality 0.8    │
│ - Quality 0.85   │              │ - Format: WebP   │
│ - Format: WebP   │              │                  │
└────────┬─────────┘              └────────┬─────────┘
         │                                 │
         ▼                                 ▼
┌──────────────────┐              ┌──────────────────┐
│ Save to disk     │              │ Save to disk     │
│ project_X.webp   │              │ project_X_thumb  │
└────────┬─────────┘              └────────┬─────────┘
         │                                 │
         └────────────┬────────────────────┘
                      ▼
          ┌──────────────────┐
          │ Delete old files │ ── Cleanup previous images
          └──────────┬───────┘
                     ▼
          ┌──────────────────┐
          │ Update database  │ ── Save URLs to project
          │ - imageUrl       │
          │ - thumbnailUrl   │
          └──────────────────┘
```

---

## 5. Performance

### File Size Comparison

**Example**: Project screenshot (1920×1080)

| Format | File Size | Quality | Savings |
|--------|-----------|---------|---------|
| Original PNG | 2.1 MB | Lossless | - |
| JPEG (quality 90) | 850 KB | High | 59% |
| **WebP (quality 0.85)** | **580 KB** | **High** | **72%** |
| WebP Thumbnail (400×300) | **45 KB** | **Good** | **98%** |

### Processing Time

**Benchmarks** (average, on modern hardware):

| Operation | Time | File |
|-----------|------|------|
| Upload 2 MB image | ~50ms | - |
| WebP conversion | ~200ms | Full image |
| Thumbnail generation | ~100ms | 400×300px |
| **Total processing** | **~350ms** | **Complete flow** |

**Optimization Tips**:
- Use async processing for large batches
- Consider CDN for static file serving
- Implement lazy loading on frontend

### Bandwidth Savings

**Example**: Portfolio with 10 projects

**Before Optimization** (PNG/JPEG):
- Full images: 10 × 1.5 MB = 15 MB
- No thumbnails (load full images)
- **Total**: ~15 MB per page load

**After Optimization** (WebP):
- Thumbnails (list): 10 × 50 KB = 500 KB
- Full image (detail): 1 × 600 KB = 600 KB
- **Total**: ~1.1 MB per typical session

**Savings**: 93% reduction in bandwidth

---

## 6. Configuration

### Dependencies

**build.gradle**:
```gradle
dependencies {
    // Image processing
    implementation 'net.coobird:thumbnailator:0.4.19'

    // Spring Boot (already included)
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

### Application Properties

**application.yml**:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

file:
  upload:
    base-path: uploads
    projects-path: projects
    allowed-image-types:
      - image/jpeg
      - image/png
      - image/webp

image:
  processing:
    full-image-quality: 0.85
    thumbnail-quality: 0.8
    thumbnail-width: 400
    thumbnail-height: 300
```

### Static Resource Serving

**WebMvcConfig.java**:
```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(3600); // 1 hour cache
    }
}
```

### Production Recommendations

**1. Use Cloud Storage**:
```java
// AWS S3 example
@Service
public class S3ImageService {

    private final AmazonS3 s3Client;

    public String uploadToS3(MultipartFile file, String bucketName, String key) {
        // Convert to WebP
        byte[] webpData = convertToWebP(file);

        // Upload to S3
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/webp");
        metadata.setContentLength(webpData.length);

        s3Client.putObject(bucketName, key, new ByteArrayInputStream(webpData), metadata);

        return s3Client.getUrl(bucketName, key).toString();
    }
}
```

**2. Enable CDN**:
- Use CloudFront (AWS) or Cloudflare
- Cache images at edge locations
- Reduce server load
- Faster global delivery

**3. Lazy Loading (Frontend)**:
```html
<img [src]="project.thumbnailUrl"
     [attr.data-full]="project.imageUrl"
     loading="lazy"
     alt="{{ project.title }}">
```

**4. Responsive Images**:
```html
<img [srcset]="project.thumbnailUrl + ' 400w, ' + project.imageUrl + ' 1920w'"
     sizes="(max-width: 768px) 400px, 1920px"
     [src]="project.imageUrl"
     alt="{{ project.title }}">
```

---

## Related Documentation

- [API: Projects](../api/projects.md) - Project image upload API
- [Features: File Storage](./file-storage.md) - File storage architecture
- [Development: Setup](../development/setup.md) - Development environment
