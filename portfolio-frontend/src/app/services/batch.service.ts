import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';

/**
 * Statistics about images eligible for reprocessing
 */
export interface ImageReprocessingStats {
  projectImagesEligible: number;
  articleImagesEligible: number;
  totalEligible: number;
  timestamp: string;
}

/**
 * Information about the last executed batch job
 */
export interface LastJobInfo {
  lastJobId: number | null;
  lastJobStatus: string | null;
  lastJobDate: string | null;
  exitCode: string | null;
  processedCount: number;
  errorCount: number;
}

/**
 * Result of running a batch job
 */
export interface JobRunResult {
  jobId: number;
  status: string;
  startTime: string;
  exitCode: string;
}

/**
 * Service for managing batch jobs (admin only)
 * Handles image reprocessing batch operations
 */
@Injectable({
  providedIn: 'root',
})
export class BatchService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/admin/batch`;

  /**
   * Get statistics about images eligible for reprocessing
   */
  getReprocessingStats(): Observable<ImageReprocessingStats> {
    return this.http.get<ImageReprocessingStats>(`${this.apiUrl}/image-reprocessing/stats`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch reprocessing stats failed', { error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get information about the last executed image reprocessing job
   */
  getLastJob(): Observable<LastJobInfo> {
    return this.http.get<LastJobInfo>(`${this.apiUrl}/image-reprocessing/last`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch last job info failed', { error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Run the image reprocessing batch job
   */
  runReprocessingJob(): Observable<JobRunResult> {
    this.logger.info('[HTTP] POST /api/admin/batch/image-reprocessing/run');

    return this.http.post<JobRunResult>(`${this.apiUrl}/image-reprocessing/run`, {}).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Run reprocessing job failed', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }
}
