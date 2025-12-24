import { TestBed } from '@angular/core/testing';
import { Meta, Title } from '@angular/platform-browser';
import { DOCUMENT } from '@angular/common';
import { SeoService, SeoConfig } from './seo.service';

describe('SeoService', () => {
  let service: SeoService;
  let metaSpy: jasmine.SpyObj<Meta>;
  let titleSpy: jasmine.SpyObj<Title>;
  let mockDocument: Document;
  let mockCanonicalLink: HTMLLinkElement;
  let mockScript: HTMLScriptElement;

  beforeEach(() => {
    metaSpy = jasmine.createSpyObj('Meta', ['updateTag', 'removeTag']);
    titleSpy = jasmine.createSpyObj('Title', ['setTitle']);

    mockCanonicalLink = document.createElement('link');
    mockScript = document.createElement('script');

    mockDocument = {
      querySelector: jasmine.createSpy('querySelector').and.returnValue(null),
      createElement: jasmine.createSpy('createElement').and.callFake((tag: string) => {
        if (tag === 'link') return mockCanonicalLink;
        if (tag === 'script') return mockScript;
        return document.createElement(tag);
      }),
      head: {
        appendChild: jasmine.createSpy('appendChild'),
      },
    } as unknown as Document;

    TestBed.configureTestingModule({
      providers: [
        SeoService,
        { provide: Meta, useValue: metaSpy },
        { provide: Title, useValue: titleSpy },
        { provide: DOCUMENT, useValue: mockDocument },
      ],
    });

    service = TestBed.inject(SeoService);
  });

  // ========== updateMetaTags Tests ==========

  it('should_setTitle_when_updateMetaTagsCalledWithTitle', () => {
    // Arrange
    const config: SeoConfig = {
      title: 'Test Page',
      description: 'Test description',
    };

    // Act
    service.updateMetaTags(config);

    // Assert
    expect(titleSpy.setTitle).toHaveBeenCalledWith('Test Page | Emmanuel Gabe');
  });

  it('should_setDescription_when_updateMetaTagsCalledWithDescription', () => {
    // Arrange
    const config: SeoConfig = {
      title: 'Test',
      description: 'This is a test description',
    };

    // Act
    service.updateMetaTags(config);

    // Assert
    expect(metaSpy.updateTag).toHaveBeenCalledWith({
      name: 'description',
      content: 'This is a test description',
    });
  });

  it('should_setOpenGraphTags_when_updateMetaTagsCalledWithConfig', () => {
    // Arrange
    const config: SeoConfig = {
      title: 'OG Test',
      description: 'OG description',
      url: '/test-page',
      type: 'website',
    };

    // Act
    service.updateMetaTags(config);

    // Assert
    expect(metaSpy.updateTag).toHaveBeenCalledWith({
      property: 'og:title',
      content: 'OG Test',
    });
    expect(metaSpy.updateTag).toHaveBeenCalledWith({
      property: 'og:description',
      content: 'OG description',
    });
    expect(metaSpy.updateTag).toHaveBeenCalledWith({
      property: 'og:type',
      content: 'website',
    });
    expect(metaSpy.updateTag).toHaveBeenCalledWith({
      property: 'og:site_name',
      content: 'Emmanuel Gabe',
    });
    expect(metaSpy.updateTag).toHaveBeenCalledWith({
      property: 'og:locale',
      content: 'fr_FR',
    });
  });

  it('should_setTwitterCards_when_updateMetaTagsCalledWithConfig', () => {
    // Arrange
    const config: SeoConfig = {
      title: 'Twitter Test',
      description: 'Twitter description',
    };

    // Act
    service.updateMetaTags(config);

    // Assert
    expect(metaSpy.updateTag).toHaveBeenCalledWith({
      name: 'twitter:card',
      content: 'summary_large_image',
    });
    expect(metaSpy.updateTag).toHaveBeenCalledWith({
      name: 'twitter:title',
      content: 'Twitter Test',
    });
    expect(metaSpy.updateTag).toHaveBeenCalledWith({
      name: 'twitter:description',
      content: 'Twitter description',
    });
  });

  it('should_setCanonicalUrl_when_updateMetaTagsCalledWithUrl', () => {
    // Arrange
    const config: SeoConfig = {
      title: 'Test',
      description: 'Test',
      url: '/blog/article',
    };

    // Act
    service.updateMetaTags(config);

    // Assert
    expect(mockDocument.createElement).toHaveBeenCalledWith('link');
    expect(mockCanonicalLink.getAttribute('rel')).toBe('canonical');
    expect(mockDocument.head.appendChild).toHaveBeenCalled();
  });

  it('should_setArticleMetaTags_when_typeIsArticle', () => {
    // Arrange
    const config: SeoConfig = {
      title: 'Article Title',
      description: 'Article excerpt',
      type: 'article',
      article: {
        publishedTime: '2025-01-15T10:00:00Z',
        author: 'Emmanuel Gabe',
      },
    };

    // Act
    service.updateMetaTags(config);

    // Assert
    expect(metaSpy.updateTag).toHaveBeenCalledWith({
      property: 'article:published_time',
      content: '2025-01-15T10:00:00Z',
    });
    expect(metaSpy.updateTag).toHaveBeenCalledWith({
      property: 'article:author',
      content: 'Emmanuel Gabe',
    });
  });

  it('should_removeArticleMetaTags_when_typeIsNotArticle', () => {
    // Arrange
    const config: SeoConfig = {
      title: 'Website Page',
      description: 'Not an article',
      type: 'website',
    };

    // Act
    service.updateMetaTags(config);

    // Assert
    expect(metaSpy.removeTag).toHaveBeenCalledWith('property="article:published_time"');
    expect(metaSpy.removeTag).toHaveBeenCalledWith('property="article:modified_time"');
    expect(metaSpy.removeTag).toHaveBeenCalledWith('property="article:author"');
  });

  it('should_setNoIndex_when_noIndexIsTrue', () => {
    // Arrange
    const config: SeoConfig = {
      title: 'Admin Page',
      description: 'Should not be indexed',
      noIndex: true,
    };

    // Act
    service.updateMetaTags(config);

    // Assert
    expect(metaSpy.updateTag).toHaveBeenCalledWith({
      name: 'robots',
      content: 'noindex, nofollow',
    });
  });

  it('should_removeRobotsTag_when_noIndexIsFalse', () => {
    // Arrange
    const config: SeoConfig = {
      title: 'Public Page',
      description: 'Should be indexed',
      noIndex: false,
    };

    // Act
    service.updateMetaTags(config);

    // Assert
    expect(metaSpy.removeTag).toHaveBeenCalledWith('name="robots"');
  });

  // ========== setJsonLd Tests ==========

  it('should_createScript_when_setJsonLdCalled', () => {
    // Arrange
    const schema = {
      '@context': 'https://schema.org',
      '@type': 'WebSite',
      name: 'Test Site',
    };

    // Act
    service.setJsonLd(schema);

    // Assert
    expect(mockDocument.createElement).toHaveBeenCalledWith('script');
    expect(mockScript.type).toBe('application/ld+json');
    expect(mockScript.text).toBe(JSON.stringify(schema));
    expect(mockDocument.head.appendChild).toHaveBeenCalledWith(mockScript);
  });

  it('should_replaceExistingScript_when_setJsonLdCalledTwice', () => {
    // Arrange
    const existingScript = document.createElement('script');
    existingScript.remove = jasmine.createSpy('remove');
    (mockDocument.querySelector as jasmine.Spy).and.returnValue(existingScript);

    const schema = { '@type': 'Article' };

    // Act
    service.setJsonLd(schema);

    // Assert
    expect(existingScript.remove).toHaveBeenCalled();
    expect(mockDocument.head.appendChild).toHaveBeenCalled();
  });

  // ========== setHomePageSchema Tests ==========

  it('should_createPersonSchema_when_setHomePageSchemaCalled', () => {
    // Arrange
    const config = {
      name: 'Emmanuel Gabe',
      jobTitle: 'Full Stack Developer',
      description: 'Developer portfolio',
      email: 'contact@example.com',
      github: 'https://github.com/example',
      linkedin: 'https://linkedin.com/in/example',
    };

    // Act
    service.setHomePageSchema(config);

    // Assert
    expect(mockDocument.createElement).toHaveBeenCalledWith('script');
    expect(mockScript.type).toBe('application/ld+json');

    const parsedSchema = JSON.parse(mockScript.text);
    expect(parsedSchema['@context']).toBe('https://schema.org');
    expect(parsedSchema['@graph']).toBeDefined();
    expect(parsedSchema['@graph'].length).toBe(2);
  });

  // ========== setArticleSchema Tests ==========

  it('should_createArticleSchema_when_setArticleSchemaCalled', () => {
    // Arrange
    const article = {
      title: 'Test Article',
      description: 'Article description',
      url: '/blog/test-article',
      publishedDate: '2025-01-15T10:00:00Z',
      authorName: 'Emmanuel Gabe',
    };

    // Act
    service.setArticleSchema(article);

    // Assert
    expect(mockDocument.createElement).toHaveBeenCalledWith('script');

    const parsedSchema = JSON.parse(mockScript.text);
    expect(parsedSchema['@type']).toBe('Article');
    expect(parsedSchema.headline).toBe('Test Article');
    expect(parsedSchema.description).toBe('Article description');
    expect(parsedSchema.datePublished).toBe('2025-01-15T10:00:00Z');
    expect(parsedSchema.author.name).toBe('Emmanuel Gabe');
  });

  // ========== Image URL Tests ==========

  it('should_useDefaultImage_when_noImageProvided', () => {
    // Arrange
    const config: SeoConfig = {
      title: 'No Image Page',
      description: 'Page without image',
    };

    // Act
    service.updateMetaTags(config);

    // Assert
    const ogImageCall = metaSpy.updateTag.calls
      .allArgs()
      .find((args) => args[0].property === 'og:image');
    expect(ogImageCall).toBeDefined();
    expect(ogImageCall?.[0].content).toContain('/assets/og-default.jpg');
  });

  it('should_useProvidedImage_when_imageProvided', () => {
    // Arrange
    const config: SeoConfig = {
      title: 'With Image',
      description: 'Page with image',
      image: 'https://example.com/image.jpg',
    };

    // Act
    service.updateMetaTags(config);

    // Assert
    const ogImageCall = metaSpy.updateTag.calls
      .allArgs()
      .find((args) => args[0].property === 'og:image');
    expect(ogImageCall?.[0].content).toBe('https://example.com/image.jpg');
  });
});
