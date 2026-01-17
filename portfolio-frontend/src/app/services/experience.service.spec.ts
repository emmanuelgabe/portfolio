import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ExperienceService } from './experience.service';
import { LoggerService } from './logger.service';
import {
  ExperienceResponse,
  CreateExperienceRequest,
  UpdateExperienceRequest,
  ExperienceType,
} from '../models';
import { environment } from '../../environments/environment';

describe('ExperienceService', () => {
  let service: ExperienceService;
  let httpMock: HttpTestingController;
  let loggerService: jasmine.SpyObj<LoggerService>;
  const apiUrl = `${environment.apiUrl}/api/experiences`;
  const adminApiUrl = `${environment.apiUrl}/api/admin/experiences`;

  const mockExperienceResponse: ExperienceResponse = {
    id: 1,
    company: 'Test Company',
    role: 'Software Engineer',
    startDate: '2022-01-01',
    endDate: '2023-12-31',
    description: 'Test description',
    type: ExperienceType.WORK,
    showMonths: true,
    createdAt: '2024-01-01T00:00:00',
    updatedAt: '2024-01-01T00:00:00',
    ongoing: false,
  };

  beforeEach(() => {
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'error', 'debug']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ExperienceService,
        { provide: LoggerService, useValue: loggerSpy },
      ],
    });

    service = TestBed.inject(ExperienceService);
    httpMock = TestBed.inject(HttpTestingController);
    loggerService = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getAll', () => {
    it('should fetch all experiences', () => {
      const mockExperiences: ExperienceResponse[] = [mockExperienceResponse];

      service.getAll().subscribe((experiences) => {
        expect(experiences).toEqual(mockExperiences);
        expect(experiences.length).toBe(1);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockExperiences);
    });

    it('should log error when fetch fails', () => {
      service.getAll().subscribe({
        next: () => fail('should have failed'),
        error: (_error) => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Failed to fetch experiences',
            jasmine.objectContaining({
              status: 500,
            })
          );
        },
      });

      const req = httpMock.expectOne(apiUrl);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('getById', () => {
    it('should fetch experience by ID', () => {
      const experienceId = 1;

      service.getById(experienceId).subscribe((experience) => {
        expect(experience).toEqual(mockExperienceResponse);
        expect(experience.id).toBe(experienceId);
      });

      const req = httpMock.expectOne(`${apiUrl}/${experienceId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockExperienceResponse);
    });

    it('should log error when experience not found', () => {
      const experienceId = 999;

      service.getById(experienceId).subscribe({
        next: () => fail('should have failed'),
        error: (_error) => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Failed to fetch experience',
            jasmine.objectContaining({
              id: experienceId,
              status: 404,
            })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/${experienceId}`);
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('create', () => {
    it('should create experience and log success', () => {
      const createRequest: CreateExperienceRequest = {
        company: 'New Company',
        role: 'Developer',
        startDate: '2023-01-01',
        description: 'New experience description',
        type: ExperienceType.WORK,
        showMonths: true,
      };

      const createdResponse: ExperienceResponse = {
        ...mockExperienceResponse,
        id: 2,
        company: createRequest.company,
        role: createRequest.role,
      };

      service.create(createRequest).subscribe((experience) => {
        expect(experience).toEqual(createdResponse);
        expect(loggerService.info).toHaveBeenCalledWith('[HTTP_SUCCESS] Experience created', {
          id: createdResponse.id,
          company: createdResponse.company,
        });
      });

      expect(loggerService.info).toHaveBeenCalledWith('[HTTP] POST /api/admin/experiences', {
        company: createRequest.company,
        role: createRequest.role,
      });

      const req = httpMock.expectOne(adminApiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(createRequest);
      req.flush(createdResponse);
    });

    it('should log error when creation fails', () => {
      const createRequest: CreateExperienceRequest = {
        company: 'New Company',
        role: 'Developer',
        startDate: '2023-01-01',
        description: 'New experience description',
        type: ExperienceType.WORK,
        showMonths: true,
      };

      service.create(createRequest).subscribe({
        next: () => fail('should have failed'),
        error: (_error) => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Failed to create experience',
            jasmine.objectContaining({
              company: createRequest.company,
              status: 400,
            })
          );
        },
      });

      const req = httpMock.expectOne(adminApiUrl);
      req.flush('Validation failed', { status: 400, statusText: 'Bad Request' });
    });
  });

  describe('update', () => {
    it('should update experience and log success', () => {
      const experienceId = 1;
      const updateRequest: UpdateExperienceRequest = {
        company: 'Updated Company',
      };

      const updatedResponse: ExperienceResponse = {
        ...mockExperienceResponse,
        company: updateRequest.company!,
      };

      service.update(experienceId, updateRequest).subscribe((experience) => {
        expect(experience).toEqual(updatedResponse);
        expect(loggerService.info).toHaveBeenCalledWith('[HTTP_SUCCESS] Experience updated', {
          id: updatedResponse.id,
          company: updatedResponse.company,
        });
      });

      expect(loggerService.info).toHaveBeenCalledWith('[HTTP] PUT /api/admin/experiences', {
        id: experienceId,
      });

      const req = httpMock.expectOne(`${adminApiUrl}/${experienceId}`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updateRequest);
      req.flush(updatedResponse);
    });

    it('should log error when update fails', () => {
      const experienceId = 1;
      const updateRequest: UpdateExperienceRequest = {
        company: 'Updated Company',
      };

      service.update(experienceId, updateRequest).subscribe({
        next: () => fail('should have failed'),
        error: (_error) => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Failed to update experience',
            jasmine.objectContaining({
              id: experienceId,
              status: 404,
            })
          );
        },
      });

      const req = httpMock.expectOne(`${adminApiUrl}/${experienceId}`);
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('delete', () => {
    it('should delete experience and log success', () => {
      const experienceId = 1;

      service.delete(experienceId).subscribe(() => {
        expect(loggerService.info).toHaveBeenCalledWith('[HTTP_SUCCESS] Experience deleted', {
          id: experienceId,
        });
      });

      expect(loggerService.info).toHaveBeenCalledWith('[HTTP] DELETE /api/admin/experiences', {
        id: experienceId,
      });

      const req = httpMock.expectOne(`${adminApiUrl}/${experienceId}`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should log error when deletion fails', () => {
      const experienceId = 1;

      service.delete(experienceId).subscribe({
        next: () => fail('should have failed'),
        error: (_error) => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Failed to delete experience',
            jasmine.objectContaining({
              id: experienceId,
              status: 404,
            })
          );
        },
      });

      const req = httpMock.expectOne(`${adminApiUrl}/${experienceId}`);
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getByType', () => {
    it('should fetch experiences by type', () => {
      const experienceType = ExperienceType.WORK;
      const mockExperiences: ExperienceResponse[] = [mockExperienceResponse];

      service.getByType(experienceType).subscribe((experiences) => {
        expect(experiences).toEqual(mockExperiences);
        expect(experiences[0].type).toBe(experienceType);
      });

      const req = httpMock.expectOne(`${apiUrl}/type/${experienceType}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockExperiences);
    });

    it('should log error when fetch by type fails', () => {
      const experienceType = ExperienceType.EDUCATION;

      service.getByType(experienceType).subscribe({
        next: () => fail('should have failed'),
        error: (_error) => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Failed to fetch experiences by type',
            jasmine.objectContaining({
              type: experienceType,
              status: 500,
            })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/type/${experienceType}`);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('getOngoing', () => {
    it('should fetch ongoing experiences', () => {
      const ongoingExperience: ExperienceResponse = {
        ...mockExperienceResponse,
        endDate: undefined,
        ongoing: true,
      };
      const mockExperiences: ExperienceResponse[] = [ongoingExperience];

      service.getOngoing().subscribe((experiences) => {
        expect(experiences).toEqual(mockExperiences);
        expect(experiences[0].ongoing).toBe(true);
      });

      const req = httpMock.expectOne(`${apiUrl}/ongoing`);
      expect(req.request.method).toBe('GET');
      req.flush(mockExperiences);
    });

    it('should log error when fetch ongoing fails', () => {
      service.getOngoing().subscribe({
        next: () => fail('should have failed'),
        error: (_error) => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Failed to fetch ongoing experiences',
            jasmine.objectContaining({
              status: 500,
            })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/ongoing`);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('getRecent', () => {
    it('should fetch recent experiences with default limit', () => {
      const mockExperiences: ExperienceResponse[] = [mockExperienceResponse];

      service.getRecent().subscribe((experiences) => {
        expect(experiences).toEqual(mockExperiences);
      });

      const req = httpMock.expectOne(`${apiUrl}/recent?limit=3`);
      expect(req.request.method).toBe('GET');
      req.flush(mockExperiences);
    });

    it('should fetch recent experiences with custom limit', () => {
      const customLimit = 5;
      const mockExperiences: ExperienceResponse[] = [mockExperienceResponse];

      service.getRecent(customLimit).subscribe((experiences) => {
        expect(experiences).toEqual(mockExperiences);
      });

      const req = httpMock.expectOne(`${apiUrl}/recent?limit=${customLimit}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockExperiences);
    });

    it('should log error when fetch recent fails', () => {
      const limit = 3;

      service.getRecent(limit).subscribe({
        next: () => fail('should have failed'),
        error: (_error) => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Failed to fetch recent experiences',
            jasmine.objectContaining({
              limit,
              status: 500,
            })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/recent?limit=${limit}`);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });
});
