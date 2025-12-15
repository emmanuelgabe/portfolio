import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { DOCUMENT } from '@angular/common';
import { environment } from '../../environments/environment';

export interface SeoConfig {
  title: string;
  description: string;
  image?: string;
  url?: string;
  type?: 'website' | 'article' | 'profile';
  article?: {
    publishedTime?: string;
    modifiedTime?: string;
    author?: string;
    tags?: string[];
  };
  noIndex?: boolean;
}

export interface PersonSchema {
  name: string;
  jobTitle: string;
  description: string;
  image?: string;
  email: string;
  github?: string;
  linkedin?: string;
}

export interface ArticleSchema {
  title: string;
  description: string;
  image?: string;
  url: string;
  publishedDate: string;
  modifiedDate?: string;
  authorName: string;
  tags?: string[];
}

@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly meta = inject(Meta);
  private readonly titleService = inject(Title);
  private readonly document = inject(DOCUMENT);

  private readonly siteName = 'Emmanuel Gabe';
  private readonly locale = 'fr_FR';

  updateMetaTags(config: SeoConfig): void {
    const fullTitle = config.title ? `${config.title} | ${this.siteName}` : this.siteName;
    const fullUrl = config.url ? `${environment.baseUrl}${config.url}` : environment.baseUrl;
    const image = config.image
      ? this.ensureAbsoluteUrl(config.image)
      : `${environment.baseUrl}/assets/og-default.jpg`;

    this.titleService.setTitle(fullTitle);

    this.meta.updateTag({ name: 'description', content: config.description });

    if (config.noIndex) {
      this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
    } else {
      this.meta.removeTag('name="robots"');
    }

    this.meta.updateTag({ property: 'og:title', content: config.title });
    this.meta.updateTag({ property: 'og:description', content: config.description });
    this.meta.updateTag({ property: 'og:image', content: image });
    this.meta.updateTag({ property: 'og:url', content: fullUrl });
    this.meta.updateTag({ property: 'og:type', content: config.type || 'website' });
    this.meta.updateTag({ property: 'og:site_name', content: this.siteName });
    this.meta.updateTag({ property: 'og:locale', content: this.locale });

    if (config.type === 'article' && config.article) {
      this.setArticleMetaTags(config.article);
    } else {
      this.removeArticleMetaTags();
    }

    this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.meta.updateTag({ name: 'twitter:title', content: config.title });
    this.meta.updateTag({ name: 'twitter:description', content: config.description });
    this.meta.updateTag({ name: 'twitter:image', content: image });

    this.updateCanonicalUrl(fullUrl);
  }

  private setArticleMetaTags(article: SeoConfig['article']): void {
    if (!article) return;

    if (article.publishedTime) {
      this.meta.updateTag({
        property: 'article:published_time',
        content: article.publishedTime,
      });
    }
    if (article.modifiedTime) {
      this.meta.updateTag({
        property: 'article:modified_time',
        content: article.modifiedTime,
      });
    }
    if (article.author) {
      this.meta.updateTag({
        property: 'article:author',
        content: article.author,
      });
    }
  }

  private removeArticleMetaTags(): void {
    this.meta.removeTag('property="article:published_time"');
    this.meta.removeTag('property="article:modified_time"');
    this.meta.removeTag('property="article:author"');
  }

  private updateCanonicalUrl(url: string): void {
    let link: HTMLLinkElement | null = this.document.querySelector('link[rel="canonical"]');

    if (!link) {
      link = this.document.createElement('link');
      link.setAttribute('rel', 'canonical');
      this.document.head.appendChild(link);
    }

    link.setAttribute('href', url);
  }

  private ensureAbsoluteUrl(url: string): string {
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url;
    }
    return `${environment.baseUrl}${url.startsWith('/') ? '' : '/'}${url}`;
  }

  setJsonLd(schema: object): void {
    const existingScript = this.document.querySelector('script[type="application/ld+json"]');
    if (existingScript) {
      existingScript.remove();
    }

    const script = this.document.createElement('script');
    script.type = 'application/ld+json';
    script.text = JSON.stringify(schema);
    this.document.head.appendChild(script);
  }

  setHomePageSchema(config: PersonSchema): void {
    const schema = {
      '@context': 'https://schema.org',
      '@graph': [
        {
          '@type': 'WebSite',
          '@id': `${environment.baseUrl}/#website`,
          url: environment.baseUrl,
          name: this.siteName,
          description: config.description,
          inLanguage: 'fr-FR',
        },
        {
          '@type': 'Person',
          '@id': `${environment.baseUrl}/#person`,
          name: config.name,
          jobTitle: config.jobTitle,
          description: config.description,
          image: config.image ? this.ensureAbsoluteUrl(config.image) : undefined,
          email: `mailto:${config.email}`,
          url: environment.baseUrl,
          sameAs: [config.github, config.linkedin].filter(Boolean),
        },
      ],
    };
    this.setJsonLd(schema);
  }

  setArticleSchema(article: ArticleSchema): void {
    const schema = {
      '@context': 'https://schema.org',
      '@type': 'Article',
      headline: article.title,
      description: article.description,
      image: article.image
        ? this.ensureAbsoluteUrl(article.image)
        : `${environment.baseUrl}/assets/og-default.jpg`,
      url: `${environment.baseUrl}${article.url}`,
      datePublished: article.publishedDate,
      dateModified: article.modifiedDate || article.publishedDate,
      author: {
        '@type': 'Person',
        name: article.authorName,
        url: environment.baseUrl,
      },
      publisher: {
        '@type': 'Person',
        name: article.authorName,
        url: environment.baseUrl,
      },
      mainEntityOfPage: {
        '@type': 'WebPage',
        '@id': `${environment.baseUrl}${article.url}`,
      },
      keywords: article.tags?.join(', '),
    };
    this.setJsonLd(schema);
  }
}
