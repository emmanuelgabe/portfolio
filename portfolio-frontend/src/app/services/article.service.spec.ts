import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ArticleService } from './article.service';
import {
  ArticleResponse,
  CreateArticleRequest,
  UpdateArticleRequest,
} from '../models/article.model';
import { provideHttpClient } from '@angular/common/http';

describe('ArticleService', () => {
  let service: ArticleService;
  let httpMock: HttpTestingController;

  const mockArticle: ArticleResponse = {
    id: 1,
    title: 'Test Article',
    slug: 'test-article',
    content: 'Test content',
    contentHtml: '<p>Test content</p>',
    excerpt: 'Test excerpt',
    draft: false,
    publishedAt: '2025-01-01T00:00:00',
    createdAt: '2025-01-01T00:00:00',
    updatedAt: '2025-01-01T00:00:00',
    readingTimeMinutes: 5,
    authorName: 'Admin',
    tags: [],
    images: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ArticleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAll', () => {
    it('should return all published articles', () => {
      const mockArticles: ArticleResponse[] = [mockArticle];

      service.getAll().subscribe((articles) => {
        expect(articles).toEqual(mockArticles);
        expect(articles.length).toBe(1);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/articles') && request.method === 'GET'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockArticles);
    });

    it('should handle error when fetching articles fails', () => {
      service.getAll().subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(500);
        },
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/articles'));
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('getBySlug', () => {
    it('should return an article by slug', () => {
      service.getBySlug('test-article').subscribe((article) => {
        expect(article).toEqual(mockArticle);
        expect(article.slug).toBe('test-article');
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/articles/test-article') && request.method === 'GET'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockArticle);
    });

    it('should handle 404 error when article not found', () => {
      service.getBySlug('nonexistent').subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes('/api/articles/nonexistent')
      );
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getAllAdmin', () => {
    it('should return all articles including drafts', () => {
      const mockArticles: ArticleResponse[] = [mockArticle];

      service.getAllAdmin().subscribe((articles) => {
        expect(articles).toEqual(mockArticles);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/admin/articles') && request.method === 'GET'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockArticles);
    });
  });

  describe('getById', () => {
    it('should return an article by ID', () => {
      service.getById(1).subscribe((article) => {
        expect(article).toEqual(mockArticle);
        expect(article.id).toBe(1);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/admin/articles/1') && request.method === 'GET'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockArticle);
    });
  });

  describe('create', () => {
    it('should create a new article', () => {
      const createRequest: CreateArticleRequest = {
        title: 'New Article',
        content: 'New content',
        excerpt: 'New excerpt',
        draft: true,
        tagIds: [1, 2],
      };

      service.create(createRequest).subscribe((article) => {
        expect(article).toEqual(mockArticle);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/admin/articles') && request.method === 'POST'
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(createRequest);
      req.flush(mockArticle);
    });

    it('should handle validation error when creating article', () => {
      const createRequest: CreateArticleRequest = {
        title: '',
        content: '',
      };

      service.create(createRequest).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);
        },
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/admin/articles'));
      req.flush('Validation error', { status: 400, statusText: 'Bad Request' });
    });
  });

  describe('update', () => {
    it('should update an existing article', () => {
      const updateRequest: UpdateArticleRequest = {
        title: 'Updated Article',
        content: 'Updated content',
      };

      service.update(1, updateRequest).subscribe((article) => {
        expect(article).toEqual(mockArticle);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/admin/articles/1') && request.method === 'PUT'
      );
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updateRequest);
      req.flush(mockArticle);
    });

    it('should handle 404 error when updating nonexistent article', () => {
      const updateRequest: UpdateArticleRequest = {
        title: 'Updated Article',
      };

      service.update(999, updateRequest).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/admin/articles/999'));
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('delete', () => {
    it('should delete an article', () => {
      service.delete(1).subscribe((response) => {
        expect(response).toBeNull();
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/admin/articles/1') && request.method === 'DELETE'
      );
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should handle 404 error when deleting nonexistent article', () => {
      service.delete(999).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/admin/articles/999'));
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('publish', () => {
    it('should publish an article', () => {
      service.publish(1).subscribe((article) => {
        expect(article).toEqual(mockArticle);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url.includes('/api/admin/articles/1/publish') && request.method === 'PUT'
      );
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({});
      req.flush(mockArticle);
    });
  });

  describe('unpublish', () => {
    it('should unpublish an article', () => {
      const draftArticle = { ...mockArticle, draft: true };

      service.unpublish(1).subscribe((article) => {
        expect(article).toEqual(draftArticle);
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url.includes('/api/admin/articles/1/unpublish') && request.method === 'PUT'
      );
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({});
      req.flush(draftArticle);
    });
  });
});
