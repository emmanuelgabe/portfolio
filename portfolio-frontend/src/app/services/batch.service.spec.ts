import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BatchService, ImageReprocessingStats, LastJobInfo, JobRunResult } from './batch.service';
import { LoggerService } from './logger.service';
import { environment } from '../../environments/environment';

describe('BatchService', () => {
  let service: BatchService;
  let httpMock: HttpTestingController;
  let loggerService: jasmine.SpyObj<LoggerService>;
  const apiUrl = `${environment.apiUrl}/api/admin/batch`;

  const mockReprocessingStats: ImageReprocessingStats = {
    projectImagesEligible: 10,
    articleImagesEligible: 5,
    totalEligible: 15,
    timestamp: '2025-01-15T10:30:00',
  };

  const mockLastJobInfo: LastJobInfo = {
    lastJobId: 123,
    lastJobStatus: 'COMPLETED',
    lastJobDate: '2025-01-15T10:30:00',
    exitCode: 'COMPLETED',
    processedCount: 15,
    errorCount: 2,
  };

  const mockJobRunResult: JobRunResult = {
    jobId: 124,
    status: 'COMPLETED',
    startTime: '2025-01-15T11:00:00',
    exitCode: 'COMPLETED',
  };

  beforeEach(() => {
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'error', 'debug']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        BatchService,
        { provide: LoggerService, useValue: loggerSpy },
      ],
    });

    service = TestBed.inject(BatchService);
    httpMock = TestBed.inject(HttpTestingController);
    loggerService = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ========== getReprocessingStats Tests ==========

  describe('getReprocessingStats', () => {
    it('should fetch reprocessing stats successfully', () => {
      service.getReprocessingStats().subscribe((stats) => {
        expect(stats).toEqual(mockReprocessingStats);
        expect(stats.totalEligible).toBe(15);
      });

      const req = httpMock.expectOne(`${apiUrl}/image-reprocessing/stats`);
      expect(req.request.method).toBe('GET');
      req.flush(mockReprocessingStats);
    });

    it('should log error when fetch stats fails', () => {
      service.getReprocessingStats().subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Fetch reprocessing stats failed',
            jasmine.objectContaining({
              error: jasmine.anything(),
            })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/image-reprocessing/stats`);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });

  // ========== getLastJob Tests ==========

  describe('getLastJob', () => {
    it('should fetch last job info successfully', () => {
      service.getLastJob().subscribe((lastJob) => {
        expect(lastJob).toEqual(mockLastJobInfo);
        expect(lastJob.lastJobId).toBe(123);
        expect(lastJob.processedCount).toBe(15);
      });

      const req = httpMock.expectOne(`${apiUrl}/image-reprocessing/last`);
      expect(req.request.method).toBe('GET');
      req.flush(mockLastJobInfo);
    });

    it('should handle null values when no job has been executed', () => {
      const emptyJobInfo: LastJobInfo = {
        lastJobId: null,
        lastJobStatus: null,
        lastJobDate: null,
        exitCode: null,
        processedCount: 0,
        errorCount: 0,
      };

      service.getLastJob().subscribe((lastJob) => {
        expect(lastJob.lastJobId).toBeNull();
        expect(lastJob.processedCount).toBe(0);
      });

      const req = httpMock.expectOne(`${apiUrl}/image-reprocessing/last`);
      req.flush(emptyJobInfo);
    });

    it('should log error when fetch last job fails', () => {
      service.getLastJob().subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Fetch last job info failed',
            jasmine.objectContaining({
              error: jasmine.anything(),
            })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/image-reprocessing/last`);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });

  // ========== runReprocessingJob Tests ==========

  describe('runReprocessingJob', () => {
    it('should run reprocessing job successfully', () => {
      service.runReprocessingJob().subscribe((result) => {
        expect(result).toEqual(mockJobRunResult);
        expect(result.jobId).toBe(124);
        expect(result.exitCode).toBe('COMPLETED');
      });

      expect(loggerService.info).toHaveBeenCalledWith(
        '[HTTP] POST /api/admin/batch/image-reprocessing/run'
      );

      const req = httpMock.expectOne(`${apiUrl}/image-reprocessing/run`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(mockJobRunResult);
    });

    it('should handle failed job execution', () => {
      const failedResult: JobRunResult = {
        jobId: 125,
        status: 'FAILED',
        startTime: '2025-01-15T11:00:00',
        exitCode: 'FAILED',
      };

      service.runReprocessingJob().subscribe((result) => {
        expect(result.status).toBe('FAILED');
        expect(result.exitCode).toBe('FAILED');
      });

      const req = httpMock.expectOne(`${apiUrl}/image-reprocessing/run`);
      req.flush(failedResult);
    });

    it('should log error when run job fails', () => {
      service.runReprocessingJob().subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Run reprocessing job failed',
            jasmine.objectContaining({
              status: 500,
            })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/image-reprocessing/run`);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });
});
