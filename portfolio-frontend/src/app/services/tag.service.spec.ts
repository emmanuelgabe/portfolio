import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TagService } from './tag.service';
import { CreateTagRequest, TagResponse, UpdateTagRequest } from '../models/tag.model';
import { provideHttpClient } from '@angular/common/http';

describe('TagService', () => {
  let service: TagService;
  let httpMock: HttpTestingController;

  const mockTag: TagResponse = {
    id: 1,
    name: 'Angular',
    color: '#DD0031',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TagService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAll', () => {
    it('should return all tags', () => {
      const mockTags: TagResponse[] = [mockTag, { id: 2, name: 'TypeScript', color: '#3178C6' }];

      service.getAll().subscribe((tags) => {
        expect(tags).toEqual(mockTags);
        expect(tags.length).toBe(2);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/tags') && request.method === 'GET'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockTags);
    });

    it('should handle error when fetching tags fails', () => {
      service.getAll().subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(500);
        },
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/tags'));
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('getById', () => {
    it('should return a tag by ID', () => {
      service.getById(1).subscribe((tag) => {
        expect(tag).toEqual(mockTag);
        expect(tag.id).toBe(1);
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/tags/1'));
      expect(req.request.method).toBe('GET');
      req.flush(mockTag);
    });

    it('should handle error when tag not found', () => {
      service.getById(999).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/tags/999'));
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('create', () => {
    it('should create a new tag', () => {
      const createRequest: CreateTagRequest = { name: 'React', color: '#61DAFB' };
      const createdTag: TagResponse = { id: 3, name: 'React', color: '#61DAFB' };

      service.create(createRequest).subscribe((tag) => {
        expect(tag).toEqual(createdTag);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/admin/tags') && request.method === 'POST'
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(createRequest);
      req.flush(createdTag);
    });

    it('should handle error when creating tag fails', () => {
      const createRequest: CreateTagRequest = { name: '', color: 'invalid' };

      service.create(createRequest).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);
        },
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/admin/tags'));
      req.flush('Bad request', { status: 400, statusText: 'Bad Request' });
    });
  });

  describe('update', () => {
    it('should update an existing tag', () => {
      const updateRequest: UpdateTagRequest = { name: 'Angular Updated', color: '#FF0000' };
      const updatedTag: TagResponse = { id: 1, name: 'Angular Updated', color: '#FF0000' };

      service.update(1, updateRequest).subscribe((tag) => {
        expect(tag).toEqual(updatedTag);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/admin/tags/1') && request.method === 'PUT'
      );
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updateRequest);
      req.flush(updatedTag);
    });

    it('should handle error when updating tag fails', () => {
      const updateRequest: UpdateTagRequest = { name: 'Updated' };

      service.update(999, updateRequest).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/admin/tags/999'));
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('delete', () => {
    it('should delete a tag', () => {
      service.delete(1).subscribe(() => {
        expect(true).toBe(true);
      });

      const req = httpMock.expectOne(
        (request) => request.url.includes('/api/admin/tags/1') && request.method === 'DELETE'
      );
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should handle error when deleting tag fails', () => {
      service.delete(999).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/admin/tags/999'));
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });
});
