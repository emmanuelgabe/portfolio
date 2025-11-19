import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CvService } from './cv.service';
import { CvResponse } from '../models/cv.model';
import { provideHttpClient } from '@angular/common/http';

describe('CvService', () => {
  let service: CvService;
  let httpMock: HttpTestingController;
  const apiUrl = '/api/cv';

  const mockCvResponse: CvResponse = {
    id: 1,
    fileName: 'cv_20240101_120000_abc123.pdf',
    originalFileName: 'my_cv.pdf',
    fileUrl: '/uploads/cvs/cv_20240101_120000_abc123.pdf',
    fileSize: 1024,
    uploadedAt: '2024-01-01T12:00:00',
    current: true,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CvService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getCurrentCv', () => {
    it('should return the current CV', () => {
      service.getCurrentCv().subscribe((cv) => {
        expect(cv).toEqual(mockCvResponse);
        expect(cv.id).toBe(1);
        expect(cv.current).toBe(true);
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/cv/current') && request.method === 'GET');
      expect(req.request.method).toBe('GET');
      req.flush(mockCvResponse);
    });

    it('should handle error when no CV exists', () => {
      service.getCurrentCv().subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/cv/current'));
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('downloadCv', () => {
    it('should call window.open with correct URL', () => {
      spyOn(window, 'open');

      service.downloadCv();

      expect(window.open).toHaveBeenCalledWith('/api/cv/download', '_blank');
    });
  });

  describe('getDownloadUrl', () => {
    it('should return the correct download URL', () => {
      const url = service.getDownloadUrl();

      expect(url).toBe('/api/cv/download');
    });
  });

  describe('uploadCv', () => {
    it('should upload a CV file', () => {
      const mockFile = new File(['test'], 'test_cv.pdf', { type: 'application/pdf' });

      service.uploadCv(mockFile).subscribe((cv) => {
        expect(cv).toEqual(mockCvResponse);
        expect(cv.fileName).toBe('cv_20240101_120000_abc123.pdf');
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/cv/upload') && request.method === 'POST');
      expect(req.request.method).toBe('POST');
      expect(req.request.body instanceof FormData).toBe(true);
      req.flush(mockCvResponse);
    });

    it('should handle upload error', () => {
      const mockFile = new File(['test'], 'test.txt', { type: 'text/plain' });

      service.uploadCv(mockFile).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);
        },
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/cv/upload'));
      req.flush('Bad Request', { status: 400, statusText: 'Bad Request' });
    });
  });

  describe('getAllCvs', () => {
    it('should return all CV versions', () => {
      const mockCvs: CvResponse[] = [
        { ...mockCvResponse, id: 1, current: true },
        { ...mockCvResponse, id: 2, current: false },
      ];

      service.getAllCvs().subscribe((cvs) => {
        expect(cvs).toEqual(mockCvs);
        expect(cvs.length).toBe(2);
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/cv/all') && request.method === 'GET');
      expect(req.request.method).toBe('GET');
      req.flush(mockCvs);
    });
  });

  describe('setCurrentCv', () => {
    it('should set a CV as current', () => {
      service.setCurrentCv(2).subscribe((cv) => {
        expect(cv).toEqual(mockCvResponse);
        expect(cv.current).toBe(true);
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/cv/2/set-current') && request.method === 'PUT');
      expect(req.request.method).toBe('PUT');
      req.flush(mockCvResponse);
    });
  });

  describe('deleteCv', () => {
    it('should delete a CV', () => {
      service.deleteCv(2).subscribe(() => {
        expect(true).toBe(true);
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/cv/2') && request.method === 'DELETE');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should handle delete error', () => {
      service.deleteCv(999).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne((request) => request.url.includes('/api/cv/999'));
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });
    });
  });
});
