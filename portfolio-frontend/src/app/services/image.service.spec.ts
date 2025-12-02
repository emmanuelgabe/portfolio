import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ImageService } from './image.service';
import { ImageUploadResponse } from '../models/image-upload-response.model';
import { provideHttpClient } from '@angular/common/http';
import { HttpEvent, HttpEventType } from '@angular/common/http';
import { LoggerService } from './logger.service';

describe('ImageService', () => {
  let service: ImageService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const mockImageUploadResponse: ImageUploadResponse = {
    imageUrl: '/uploads/projects/project_1_123.webp',
    thumbnailUrl: '/uploads/projects/project_1_123_thumb.webp',
    fileSize: 102400,
    uploadedAt: '2024-01-01T12:00:00',
  };

  beforeEach(() => {
    const spy = jasmine.createSpyObj('LoggerService', ['info', 'error', 'warn', 'debug']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LoggerService, useValue: spy },
      ],
    });
    service = TestBed.inject(ImageService);
    httpMock = TestBed.inject(HttpTestingController);
    loggerSpy = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('uploadProjectImage', () => {
    it('should upload an image without progress tracking', () => {
      const projectId = 1;
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });

      service.uploadProjectImage(projectId, mockFile, false).subscribe((response) => {
        expect(response).toEqual(mockImageUploadResponse);
        expect((response as ImageUploadResponse).imageUrl).toBe(
          '/uploads/projects/project_1_123.webp'
        );
        expect((response as ImageUploadResponse).thumbnailUrl).toBe(
          '/uploads/projects/project_1_123_thumb.webp'
        );
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url.includes(`/api/admin/projects/${projectId}/image`) &&
          request.method === 'POST'
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body instanceof FormData).toBe(true);
      expect(req.request.reportProgress).toBe(false);
      req.flush(mockImageUploadResponse);

      // Verify logging
      expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP] POST /api/admin/projects/{id}/image', {
        projectId: 1,
        fileName: 'test.jpg',
      });
    });

    it('should log upload request with progress tracking', (done) => {
      const projectId = 1;
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });

      service.uploadProjectImage(projectId, mockFile, true).subscribe({
        complete: () => {
          // Verify logging
          expect(loggerSpy.info).toHaveBeenCalledWith(
            '[HTTP] POST /api/admin/projects/{id}/image',
            {
              projectId: 1,
              fileName: 'test.jpg',
            }
          );
          done();
        },
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url.includes(`/api/admin/projects/${projectId}/image`) &&
          request.method === 'POST'
      );
      req.flush(mockImageUploadResponse);
    });

    it('should track upload progress with events', (done) => {
      const projectId = 1;
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const events: HttpEvent<ImageUploadResponse>[] = [];

      service.uploadProjectImage(projectId, mockFile, true).subscribe((event) => {
        events.push(event);

        // Check if we received the final response
        if (event.type === HttpEventType.Response) {
          expect(events.length).toBeGreaterThan(0);
          expect(event.body).toEqual(mockImageUploadResponse);
          done();
        }
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url.includes(`/api/admin/projects/${projectId}/image`) &&
          request.method === 'POST'
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body instanceof FormData).toBe(true);
      expect(req.request.reportProgress).toBe(true);

      // Simulate upload progress
      req.event({ type: HttpEventType.Sent });
      req.event({ type: HttpEventType.UploadProgress, loaded: 50, total: 100 });
      req.event({ type: HttpEventType.UploadProgress, loaded: 100, total: 100 });
      req.flush(mockImageUploadResponse);
    });

    it('should handle upload error and log it', () => {
      const projectId = 1;
      const mockFile = new File(['test'], 'test.txt', { type: 'text/plain' });

      service.uploadProjectImage(projectId, mockFile, false).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);

          // Verify error logging
          expect(loggerSpy.error).toHaveBeenCalledWith(
            '[HTTP] Upload project image failed',
            jasmine.objectContaining({
              projectId: 1,
              fileName: 'test.txt',
              status: 400,
            })
          );
        },
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes(`/api/admin/projects/${projectId}/image`)
      );
      req.flush({ message: 'File type not allowed' }, { status: 400, statusText: 'Bad Request' });
    });

    it('should handle project not found error and log it', () => {
      const projectId = 999;
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });

      service.uploadProjectImage(projectId, mockFile, false).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);

          // Verify error logging
          expect(loggerSpy.error).toHaveBeenCalledWith(
            '[HTTP] Upload project image failed',
            jasmine.objectContaining({
              projectId: 999,
              fileName: 'test.jpg',
              status: 404,
            })
          );
        },
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes(`/api/admin/projects/${projectId}/image`)
      );
      req.flush({ message: 'Project not found' }, { status: 404, statusText: 'Not Found' });
    });

    it('should handle file size exceeded error and log it', () => {
      const projectId = 1;
      const mockFile = new File(['x'.repeat(11 * 1024 * 1024)], 'large.jpg', {
        type: 'image/jpeg',
      });

      service.uploadProjectImage(projectId, mockFile, false).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);

          // Verify error logging
          expect(loggerSpy.error).toHaveBeenCalledWith(
            '[HTTP] Upload project image failed',
            jasmine.objectContaining({
              projectId: 1,
              fileName: 'large.jpg',
              status: 400,
            })
          );
        },
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes(`/api/admin/projects/${projectId}/image`)
      );
      req.flush(
        { message: 'File size exceeds maximum allowed size' },
        { status: 400, statusText: 'Bad Request' }
      );
    });

    it('should create FormData with correct file field', () => {
      const projectId = 1;
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });

      service.uploadProjectImage(projectId, mockFile, false).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url.includes(`/api/admin/projects/${projectId}/image`)
      );
      const formData = req.request.body as FormData;
      expect(formData.has('file')).toBe(true);
      req.flush(mockImageUploadResponse);
    });
  });

  describe('deleteProjectImage', () => {
    it('should delete a project image and log it', () => {
      const projectId = 1;

      service.deleteProjectImage(projectId).subscribe(() => {
        expect(true).toBe(true);

        // Verify logging
        expect(loggerSpy.info).toHaveBeenCalledWith(
          '[HTTP] DELETE /api/admin/projects/{id}/image',
          {
            projectId: 1,
          }
        );
      });

      const req = httpMock.expectOne(
        (request) =>
          request.url.includes(`/api/admin/projects/${projectId}/image`) &&
          request.method === 'DELETE'
      );
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should handle delete error when project not found and log it', () => {
      const projectId = 999;

      service.deleteProjectImage(projectId).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);

          // Verify error logging
          expect(loggerSpy.error).toHaveBeenCalledWith(
            '[HTTP] Delete project image failed',
            jasmine.objectContaining({
              projectId: 999,
              status: 404,
            })
          );
        },
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes(`/api/admin/projects/${projectId}/image`)
      );
      req.flush({ message: 'Project not found' }, { status: 404, statusText: 'Not Found' });
    });

    it('should successfully delete when project has no images', () => {
      const projectId = 1;

      service.deleteProjectImage(projectId).subscribe(() => {
        expect(true).toBe(true);

        // Verify logging
        expect(loggerSpy.info).toHaveBeenCalledWith(
          '[HTTP] DELETE /api/admin/projects/{id}/image',
          {
            projectId: 1,
          }
        );
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes(`/api/admin/projects/${projectId}/image`)
      );
      req.flush(null, { status: 204, statusText: 'No Content' });
    });
  });
});
