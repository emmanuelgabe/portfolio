import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { FileUploadService, FileUploadResponse } from './file-upload.service';
import { LoggerService } from './logger.service';
import { environment } from '../../environments/environment';

describe('FileUploadService', () => {
  let service: FileUploadService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const mockUploadResponse: FileUploadResponse = {
    fileName: 'uploaded-image.webp',
    fileUrl: '/uploads/images/uploaded-image.webp',
    fileType: 'image/webp',
    fileSize: 102400,
  };

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'error', 'warn', 'debug']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        FileUploadService,
        { provide: LoggerService, useValue: loggerSpy },
      ],
    });

    service = TestBed.inject(FileUploadService);
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
    const mockFile = new File(['test content'], 'test.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(mockFile).subscribe((response) => {
      // Assert
      expect(response).toEqual(mockUploadResponse);
      expect(response.fileName).toBe('uploaded-image.webp');
      expect(response.fileUrl).toBe('/uploads/images/uploaded-image.webp');
    });

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/upload/image`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush(mockUploadResponse);
  });

  it('should_logRequest_when_uploadImageCalled', () => {
    // Arrange
    const mockFile = new File(['test'], 'image.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(mockFile).subscribe();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP_REQUEST] Uploading image', {
      fileName: 'image.jpg',
      fileSize: 4,
      fileType: 'image/jpeg',
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/upload/image`);
    req.flush(mockUploadResponse);
  });

  it('should_logSuccess_when_uploadImageSucceeds', () => {
    // Arrange
    const mockFile = new File(['test'], 'image.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(mockFile).subscribe();

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/upload/image`);
    req.flush(mockUploadResponse);

    expect(loggerSpy.info).toHaveBeenCalledWith('[HTTP_SUCCESS] Image uploaded successfully', {
      fileName: 'uploaded-image.webp',
      fileUrl: '/uploads/images/uploaded-image.webp',
    });
  });

  it('should_logError_when_uploadImageFails', () => {
    // Arrange
    const mockFile = new File(['test'], 'test.txt', { type: 'text/plain' });

    // Act
    service.uploadImage(mockFile).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(400);
        expect(loggerSpy.error).toHaveBeenCalledWith(
          '[HTTP_ERROR] Failed to upload image',
          jasmine.objectContaining({
            fileName: 'test.txt',
            status: 400,
          })
        );
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/upload/image`);
    req.flush({ message: 'File type not allowed' }, { status: 400, statusText: 'Bad Request' });
  });

  it('should_includeFileInFormData_when_uploadImageCalled', () => {
    // Arrange
    const mockFile = new File(['test content'], 'test.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(mockFile).subscribe();

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/upload/image`);
    const formData = req.request.body as FormData;
    expect(formData.has('file')).toBeTrue();
    req.flush(mockUploadResponse);
  });

  it('should_handleServerError_when_uploadImageReturns500', () => {
    // Arrange
    const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(mockFile).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(500);
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/upload/image`);
    req.flush(
      { message: 'Internal server error' },
      { status: 500, statusText: 'Internal Server Error' }
    );
  });

  it('should_handleUnauthorized_when_uploadImageCalledWithoutAuth', () => {
    // Arrange
    const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(mockFile).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(401);
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/upload/image`);
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
  });

  it('should_handleFileTooLarge_when_uploadImageExceedsLimit', () => {
    // Arrange
    const largeFile = new File(['x'.repeat(11 * 1024 * 1024)], 'large.jpg', { type: 'image/jpeg' });

    // Act
    service.uploadImage(largeFile).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        // Assert
        expect(error.status).toBe(413);
      },
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/upload/image`);
    req.flush(
      { message: 'File size exceeds maximum allowed size' },
      { status: 413, statusText: 'Payload Too Large' }
    );
  });

  it('should_returnCorrectFileType_when_uploadImageSucceeds', () => {
    // Arrange
    const mockFile = new File(['test'], 'test.png', { type: 'image/png' });
    const pngResponse: FileUploadResponse = {
      ...mockUploadResponse,
      fileName: 'uploaded-image.webp',
      fileType: 'image/webp',
    };

    // Act
    service.uploadImage(mockFile).subscribe((response) => {
      // Assert
      expect(response.fileType).toBe('image/webp');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/admin/upload/image`);
    req.flush(pngResponse);
  });
});
