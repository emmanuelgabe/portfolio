# SEO (Search Engine Optimization)

Dynamic meta tags, OpenGraph, Twitter Cards, and JSON-LD structured data.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Meta Tags](#meta-tags)
4. [Structured Data](#structured-data)
5. [Configuration](#configuration)
6. [Related Documentation](#related-documentation)

---

## Overview

The SEO system provides:

- **Dynamic meta tags** - Page-specific titles and descriptions
- **OpenGraph protocol** - Social media sharing optimization
- **Twitter Cards** - Enhanced Twitter previews
- **JSON-LD schemas** - Structured data for search engines
- **Canonical URLs** - Proper URL management

Key characteristics:
- Angular Meta and Title services
- Centralized SeoService
- Per-route configuration
- Article-specific schemas

---

## Architecture

### Frontend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `SeoService` | `services/` | Meta tag and schema management |
| `HomeComponent` | `components/home/` | Home page SEO |
| `ArticleDetailComponent` | `pages/blog/` | Article SEO |
| `ProjectDetailComponent` | `components/project-detail/` | Project SEO |

### Meta Tag Types

| Type | Protocol | Purpose |
|------|----------|---------|
| Standard | HTML | Basic meta description, robots |
| OpenGraph | og: | Facebook, LinkedIn sharing |
| Twitter | twitter: | Twitter card previews |
| JSON-LD | Schema.org | Structured data |

---

## Meta Tags

### Standard Meta Tags

```html
<title>Page Title | Emmanuel Gabe</title>
<meta name="description" content="Page description for search results">
<meta name="robots" content="index, follow">
<link rel="canonical" href="https://emmanuelgabe.com/page">
```

### OpenGraph Tags

```html
<meta property="og:title" content="Page Title">
<meta property="og:description" content="Page description">
<meta property="og:image" content="https://emmanuelgabe.com/assets/og-default.jpg">
<meta property="og:url" content="https://emmanuelgabe.com/page">
<meta property="og:type" content="website">
<meta property="og:site_name" content="Emmanuel Gabe">
<meta property="og:locale" content="fr_FR">
```

### Twitter Card Tags

```html
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="Page Title">
<meta name="twitter:description" content="Page description">
<meta name="twitter:image" content="https://emmanuelgabe.com/assets/og-default.jpg">
```

### Article-Specific Tags

```html
<meta property="og:type" content="article">
<meta property="article:published_time" content="2024-01-15T10:00:00Z">
<meta property="article:modified_time" content="2024-01-20T14:30:00Z">
<meta property="article:author" content="Emmanuel Gabe">
```

---

## Structured Data

### WebSite Schema (Home Page)

```json
{
  "@context": "https://schema.org",
  "@type": "WebSite",
  "@id": "https://emmanuelgabe.com/#website",
  "url": "https://emmanuelgabe.com",
  "name": "Emmanuel Gabe",
  "description": "Portfolio and blog",
  "inLanguage": "fr-FR"
}
```

### Person Schema (Home Page)

```json
{
  "@context": "https://schema.org",
  "@type": "Person",
  "@id": "https://emmanuelgabe.com/#person",
  "name": "Emmanuel Gabe",
  "jobTitle": "Full-Stack Developer",
  "description": "Developer description",
  "email": "mailto:contact@emmanuelgabe.com",
  "url": "https://emmanuelgabe.com",
  "sameAs": [
    "https://github.com/username",
    "https://linkedin.com/in/username"
  ]
}
```

### Article Schema

```json
{
  "@context": "https://schema.org",
  "@type": "Article",
  "headline": "Article Title",
  "description": "Article excerpt",
  "image": "https://emmanuelgabe.com/uploads/article-image.webp",
  "url": "https://emmanuelgabe.com/blog/article-slug",
  "datePublished": "2024-01-15T10:00:00Z",
  "dateModified": "2024-01-20T14:30:00Z",
  "author": {
    "@type": "Person",
    "name": "Emmanuel Gabe",
    "url": "https://emmanuelgabe.com"
  },
  "publisher": {
    "@type": "Person",
    "name": "Emmanuel Gabe",
    "url": "https://emmanuelgabe.com"
  },
  "mainEntityOfPage": {
    "@type": "WebPage",
    "@id": "https://emmanuelgabe.com/blog/article-slug"
  },
  "keywords": "angular, spring boot, tutorial"
}
```

---

## Configuration

### SeoService Usage

```typescript
@Injectable({ providedIn: 'root' })
export class SeoService {
  updateMetaTags(config: SeoConfig): void {
    const fullTitle = `${config.title} | ${this.siteName}`;
    this.titleService.setTitle(fullTitle);
    this.meta.updateTag({ name: 'description', content: config.description });
    // OpenGraph and Twitter tags...
    this.updateCanonicalUrl(config.url);
  }

  setJsonLd(schema: object): void {
    const script = this.document.createElement('script');
    script.type = 'application/ld+json';
    script.text = JSON.stringify(schema);
    this.document.head.appendChild(script);
  }
}
```

### Page Component Usage

```typescript
@Component({...})
export class ArticleDetailComponent {
  private readonly seoService = inject(SeoService);

  ngOnInit(): void {
    this.loadArticle();
  }

  private loadArticle(): void {
    this.articleService.getBySlug(this.slug).subscribe(article => {
      this.seoService.updateMetaTags({
        title: article.title,
        description: article.excerpt,
        image: article.coverImageUrl,
        url: `/blog/${article.slug}`,
        type: 'article',
        article: {
          publishedTime: article.publishedAt,
          modifiedTime: article.updatedAt,
          author: 'Emmanuel Gabe',
          tags: article.tags.map(t => t.name)
        }
      });

      this.seoService.setArticleSchema({
        title: article.title,
        description: article.excerpt,
        image: article.coverImageUrl,
        url: `/blog/${article.slug}`,
        publishedDate: article.publishedAt,
        modifiedDate: article.updatedAt,
        authorName: 'Emmanuel Gabe',
        tags: article.tags.map(t => t.name)
      });
    });
  }
}
```

### Environment Configuration

```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  baseUrl: 'https://emmanuelgabe.com'
};
```

---

## noindex for Admin Pages

Admin pages excluded from indexing:

```typescript
this.seoService.updateMetaTags({
  title: 'Admin Dashboard',
  description: 'Admin area',
  noIndex: true
});
```

Generates:
```html
<meta name="robots" content="noindex, nofollow">
```

---

## Related Documentation

- [Sitemap](./sitemap.md) - Dynamic sitemap generation
- [Frontend Architecture](../architecture/frontend-architecture.md) - Angular setup

