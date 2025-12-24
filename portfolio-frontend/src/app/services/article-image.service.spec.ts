import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ArticleImageService } from './article-image.service';
import { LoggerService } from './logger.service';
import { ArticleImageResponse } from '../models/article.model';
import { environment } from '../../environments/environment';

describe('ArticleImageService', () => {
  let service: ArticleImageService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const mockImageResponse: ArticleImageResponse = {
    id: 1,
    imageUrl: '/uploads/articles/article_1_123.webp',
    thumbnailUrl: '/uploads/articles/article_1_123_thumb.webp',
    uploadedAt: '2024-01-01T12:00:00',
    status: 'PROCESSING',
  };

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'error', 'warn', 'debug']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ArticleImageService,
        { provide: LoggerService, useValue: loggerSpy },
      ],
    });

    service = TestBed.inject(ArticleImageService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ========== Initialization Tests ==========

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ========== uploadImage Tests ==========

  it('should_uploadImage_when_uploadImageCalledWithValidFile', () => {
    // Arrange
    const articleId = 1;
    const mockFile = new File(['test content'], 'test.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(articleId, mockFile).subscribe((response) => {
      // Assert
      expect(response).toEqual(mockImageResponse);
    });

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/articles/${articleId}/images`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush(mockImageResponse);
  });

  it('should_logRequest_when_uploadImageCalled', () => {
    // Arrange
    const articleId = 1;
    const mockFile = new File(['test'], 'image.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(articleId, mockFile).subscribe();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[HTTP_REQUEST] POST /api/admin/articles/:id/images',
      {
        articleId: 1,
        fileName: 'image.jpg',
        fileSize: 4,
      }
    );

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/articles/${articleId}/images`);
    req.flush(mockImageResponse);
  });

  it('should_logError_when_uploadImageFails', () => {
    // Arrange
    const articleId = 1;
    const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(articleId, mockFile).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(400);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Upload article image failed',
          jasmine.objectContaining({
            articleId: 1,
            fileName: 'test.jpg',
          })
        );
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/articles/${articleId}/images`);
    req.flush({ message: 'File type not allowed' }, { status: 400, statusText: 'Bad Request' });
  });

  it('should_includeFileInFormData_when_uploadImageCalled', () => {
    // Arrange
    const articleId = 1;
    const mockFile = new File(['test content'], 'test.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(articleId, mockFile).subscribe();

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/articles/${articleId}/images`);
    const formData = req.request.body as FormData;
    expect(formData.has('file')).toBeTrue();
    req.flush(mockImageResponse);
  });

  it('should_handleServerError_when_uploadImageReturns500', () => {
    // Arrange
    const articleId = 1;
    const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(articleId, mockFile).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(500);
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/articles/${articleId}/images`);
    req.flush(
      { message: 'Internal server error' },
      { status: 500, statusText: 'Internal Server Error' }
    );
  });

  // ========== deleteImage Tests ==========

  it('should_deleteImage_when_deleteImageCalledWithValidIds', () => {
    // Arrange
    const articleId = 1;
    const imageId = 5;

    // Act
    service.deleteImage(articleId, imageId).subscribe(() => {
      // Assert - request completed successfully
      expect(true).toBeTrue();
    });

    // Assert
    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/articles/${articleId}/images/${imageId}`
    );
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should_logRequest_when_deleteImageCalled', () => {
    // Arrange
    const articleId = 1;
    const imageId = 5;

    // Act
    service.deleteImage(articleId, imageId).subscribe();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[HTTP_REQUEST] DELETE /api/admin/articles/:articleId/images/:imageId',
      {
        articleId: 1,
        imageId: 5,
      }
    );

    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/articles/${articleId}/images/${imageId}`
    );
    req.flush(null);
  });

  it('should_logError_when_deleteImageFails', () => {
    // Arrange
    const articleId = 1;
    const imageId = 999;

    // Act
    service.deleteImage(articleId, imageId).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(404);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Delete article image failed',
          jasmine.objectContaining({
            articleId: 1,
            imageId: 999,
          })
        );
      },
    });

    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/articles/${articleId}/images/${imageId}`
    );
    req.flush({ message: 'Image not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('should_handleUnauthorized_when_deleteImageCalledWithoutAuth', () => {
    // Arrange
    const articleId = 1;
    const imageId = 5;

    // Act
    service.deleteImage(articleId, imageId).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(401);
      },
    });

    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/articles/${articleId}/images/${imageId}`
    );
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
  });
});
