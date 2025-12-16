import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ProjectImageService } from './project-image.service';
import { LoggerService } from './logger.service';
import {
  ProjectImageResponse,
  UpdateProjectImageRequest,
  ReorderProjectImagesRequest,
} from '../models/project-image.model';
import { environment } from '../../environments/environment';

describe('ProjectImageService', () => {
  let service: ProjectImageService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const mockImageResponse: ProjectImageResponse = {
    id: 1,
    imageUrl: '/uploads/projects/project_1_123.webp',
    thumbnailUrl: '/uploads/projects/project_1_123_thumb.webp',
    altText: 'Test image',
    caption: 'Test caption',
    displayOrder: 0,
    primary: true,
    uploadedAt: '2024-01-01T12:00:00',
    status: 'READY',
  };

  const mockImagesArray: ProjectImageResponse[] = [
    mockImageResponse,
    {
      id: 2,
      imageUrl: '/uploads/projects/project_1_456.webp',
      thumbnailUrl: '/uploads/projects/project_1_456_thumb.webp',
      altText: 'Second image',
      caption: 'Second caption',
      displayOrder: 1,
      primary: false,
      uploadedAt: '2024-01-02T12:00:00',
      status: 'READY',
    },
  ];

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'error', 'warn', 'debug']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ProjectImageService,
        { provide: LoggerService, useValue: loggerSpy },
      ],
    });

    service = TestBed.inject(ProjectImageService);
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
    const projectId = 1;
    const mockFile = new File(['test content'], 'test.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(projectId, mockFile).subscribe((response) => {
      // Assert
      expect(response).toEqual(mockImageResponse);
    });

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/projects/${projectId}/images`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush(mockImageResponse);
  });

  it('should_logRequest_when_uploadImageCalled', () => {
    // Arrange
    const projectId = 1;
    const mockFile = new File(['test'], 'image.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(projectId, mockFile).subscribe();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP] POST /api/admin/projects/:id/images', {
      projectId: 1,
      fileName: 'image.jpg',
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/projects/${projectId}/images`);
    req.flush(mockImageResponse);
  });

  it('should_includeAltText_when_uploadImageCalledWithAltText', () => {
    // Arrange
    const projectId = 1;
    const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
    const altText = 'Image description';

    // Act
    service.uploadImage(projectId, mockFile, altText).subscribe();

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/projects/${projectId}/images`);
    const formData = req.request.body as FormData;
    expect(formData.get('altText')).toBe(altText);
    req.flush(mockImageResponse);
  });

  it('should_includeCaption_when_uploadImageCalledWithCaption', () => {
    // Arrange
    const projectId = 1;
    const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
    const caption = 'Image caption';

    // Act
    service.uploadImage(projectId, mockFile, undefined, caption).subscribe();

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/projects/${projectId}/images`);
    const formData = req.request.body as FormData;
    expect(formData.get('caption')).toBe(caption);
    req.flush(mockImageResponse);
  });

  it('should_logError_when_uploadImageFails', () => {
    // Arrange
    const projectId = 1;
    const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(projectId, mockFile).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(400);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Upload project image failed',
          jasmine.objectContaining({
            projectId: 1,
          })
        );
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/projects/${projectId}/images`);
    req.flush({ message: 'Invalid file' }, { status: 400, statusText: 'Bad Request' });
  });

  // ========== getImages Tests ==========

  it('should_returnImages_when_getImagesCalled', () => {
    // Arrange
    const projectId = 1;

    // Act
    service.getImages(projectId).subscribe((images) => {
      // Assert
      expect(images).toEqual(mockImagesArray);
      expect(images.length).toBe(2);
    });

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/projects/${projectId}/images`);
    expect(req.request.method).toBe('GET');
    req.flush(mockImagesArray);
  });

  it('should_logError_when_getImagesFails', () => {
    // Arrange
    const projectId = 999;

    // Act
    service.getImages(projectId).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(404);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Get project images failed',
          jasmine.objectContaining({
            projectId: 999,
          })
        );
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/projects/${projectId}/images`);
    req.flush({ message: 'Project not found' }, { status: 404, statusText: 'Not Found' });
  });

  // ========== deleteImage Tests ==========

  it('should_deleteImage_when_deleteImageCalledWithValidIds', () => {
    // Arrange
    const projectId = 1;
    const imageId = 5;

    // Act
    service.deleteImage(projectId, imageId).subscribe(() => {
      // Assert
      expect(true).toBeTrue();
    });

    // Assert
    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/projects/${projectId}/images/${imageId}`
    );
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should_logRequest_when_deleteImageCalled', () => {
    // Arrange
    const projectId = 1;
    const imageId = 5;

    // Act
    service.deleteImage(projectId, imageId).subscribe();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[HTTP] DELETE /api/admin/projects/:projectId/images/:imageId',
      {
        projectId: 1,
        imageId: 5,
      }
    );

    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/projects/${projectId}/images/${imageId}`
    );
    req.flush(null);
  });

  it('should_logError_when_deleteImageFails', () => {
    // Arrange
    const projectId = 1;
    const imageId = 999;

    // Act
    service.deleteImage(projectId, imageId).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(404);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Delete project image failed',
          jasmine.objectContaining({
            projectId: 1,
            imageId: 999,
          })
        );
      },
    });

    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/projects/${projectId}/images/${imageId}`
    );
    req.flush({ message: 'Image not found' }, { status: 404, statusText: 'Not Found' });
  });

  // ========== updateImage Tests ==========

  it('should_updateImage_when_updateImageCalledWithValidRequest', () => {
    // Arrange
    const projectId = 1;
    const imageId = 5;
    const request: UpdateProjectImageRequest = {
      altText: 'Updated alt text',
      caption: 'Updated caption',
    };

    // Act
    service.updateImage(projectId, imageId, request).subscribe((response) => {
      // Assert
      expect(response.altText).toBe('Updated alt text');
    });

    // Assert
    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/projects/${projectId}/images/${imageId}`
    );
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush({ ...mockImageResponse, altText: 'Updated alt text' });
  });

  it('should_logRequest_when_updateImageCalled', () => {
    // Arrange
    const projectId = 1;
    const imageId = 5;
    const request: UpdateProjectImageRequest = { altText: 'New alt' };

    // Act
    service.updateImage(projectId, imageId, request).subscribe();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[HTTP] PUT /api/admin/projects/:projectId/images/:imageId',
      {
        projectId: 1,
        imageId: 5,
      }
    );

    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/projects/${projectId}/images/${imageId}`
    );
    req.flush(mockImageResponse);
  });

  it('should_logError_when_updateImageFails', () => {
    // Arrange
    const projectId = 1;
    const imageId = 999;
    const request: UpdateProjectImageRequest = { altText: 'New alt' };

    // Act
    service.updateImage(projectId, imageId, request).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(404);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Update project image failed',
          jasmine.objectContaining({
            projectId: 1,
            imageId: 999,
          })
        );
      },
    });

    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/projects/${projectId}/images/${imageId}`
    );
    req.flush({ message: 'Image not found' }, { status: 404, statusText: 'Not Found' });
  });

  // ========== setPrimaryImage Tests ==========

  it('should_setPrimaryImage_when_setPrimaryImageCalled', () => {
    // Arrange
    const projectId = 1;
    const imageId = 5;

    // Act
    service.setPrimaryImage(projectId, imageId).subscribe(() => {
      // Assert
      expect(true).toBeTrue();
    });

    // Assert
    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/projects/${projectId}/images/${imageId}/primary`
    );
    expect(req.request.method).toBe('PUT');
    req.flush(null);
  });

  it('should_logRequest_when_setPrimaryImageCalled', () => {
    // Arrange
    const projectId = 1;
    const imageId = 5;

    // Act
    service.setPrimaryImage(projectId, imageId).subscribe();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[HTTP] PUT /api/admin/projects/:projectId/images/:imageId/primary',
      {
        projectId: 1,
        imageId: 5,
      }
    );

    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/projects/${projectId}/images/${imageId}/primary`
    );
    req.flush(null);
  });

  it('should_logError_when_setPrimaryImageFails', () => {
    // Arrange
    const projectId = 1;
    const imageId = 999;

    // Act
    service.setPrimaryImage(projectId, imageId).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(404);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Set primary image failed',
          jasmine.objectContaining({
            projectId: 1,
            imageId: 999,
          })
        );
      },
    });

    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/projects/${projectId}/images/${imageId}/primary`
    );
    req.flush({ message: 'Image not found' }, { status: 404, statusText: 'Not Found' });
  });

  // ========== reorderImages Tests ==========

  it('should_reorderImages_when_reorderImagesCalled', () => {
    // Arrange
    const projectId = 1;
    const request: ReorderProjectImagesRequest = {
      imageIds: [3, 1, 2],
    };

    // Act
    service.reorderImages(projectId, request).subscribe(() => {
      // Assert
      expect(true).toBeTrue();
    });

    // Assert
    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/projects/${projectId}/images/reorder`
    );
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush(null);
  });

  it('should_logRequest_when_reorderImagesCalled', () => {
    // Arrange
    const projectId = 1;
    const request: ReorderProjectImagesRequest = {
      imageIds: [3, 1, 2],
    };

    // Act
    service.reorderImages(projectId, request).subscribe();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[HTTP] PUT /api/admin/projects/:projectId/images/reorder',
      {
        projectId: 1,
        count: 3,
      }
    );

    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/projects/${projectId}/images/reorder`
    );
    req.flush(null);
  });

  it('should_logError_when_reorderImagesFails', () => {
    // Arrange
    const projectId = 999;
    const request: ReorderProjectImagesRequest = {
      imageIds: [1, 2],
    };

    // Act
    service.reorderImages(projectId, request).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(404);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Reorder images failed',
          jasmine.objectContaining({
            projectId: 999,
          })
        );
      },
    });

    const req = httpMock.expectOne(
      `${environment.apiUrl}/api/admin/projects/${projectId}/images/reorder`
    );
    req.flush({ message: 'Project not found' }, { status: 404, statusText: 'Not Found' });
  });
});
