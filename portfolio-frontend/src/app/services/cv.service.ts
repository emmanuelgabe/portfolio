import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { CvResponse } from '../models/cv.model';
import { LoggerService } from './logger.service';

/**
 * Service for CV operations
 */
@Injectable({
  providedIn: 'root',
})
export class CvService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/cv`;

  /**
   * Get the current CV metadata
   * @returns Observable of current CV or null if not found (204 No Content)
   */
  getCurrentCv(): Observable<CvResponse | null> {
    this.logger.debug('[HTTP_REQUEST] Fetching current CV');

    return this.http.get<CvResponse>(`${this.apiUrl}/current`).pipe(
      tap((cv) => {
        if (cv) {
          this.logger.info('[HTTP_SUCCESS] Current CV fetched', {
            id: cv.id,
            fileName: cv.originalFileName,
          });
        } else {
          this.logger.debug('[HTTP_INFO] No current CV available');
        }
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to fetch current CV', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Download the current CV
   * Opens the CV in a new tab
   */
  downloadCv(): void {
    this.logger.info('[USER_ACTION] Downloading CV');

    const downloadUrl = `${environment.apiUrl}/api/cv/download`;
    window.open(downloadUrl, '_blank');

    this.logger.info('[USER_ACTION] CV download initiated');
  }

  /**
   * Get the download URL for the current CV
   * @returns The download URL
   */
  getDownloadUrl(): string {
    return `${environment.apiUrl}/api/cv/download`;
  }

  /**
   * Upload a new CV (admin only)
   * @param file PDF file to upload
   * @returns Observable of uploaded CV response
   */
  uploadCv(file: File): Observable<CvResponse> {
    this.logger.info('[HTTP_REQUEST] Uploading CV', { fileName: file.name, size: file.size });

    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<CvResponse>(`${this.apiUrl}/upload`, formData).pipe(
      tap((cv) => {
        this.logger.info('[HTTP_SUCCESS] CV uploaded', {
          id: cv.id,
          fileName: cv.fileName,
        });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to upload CV', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get all CVs for the authenticated user (admin only)
   * @returns Observable of all CV versions
   */
  getAllCvs(): Observable<CvResponse[]> {
    this.logger.debug('[HTTP_REQUEST] Fetching all CVs');

    return this.http.get<CvResponse[]>(`${this.apiUrl}/all`).pipe(
      tap((cvs) => {
        this.logger.info('[HTTP_SUCCESS] All CVs fetched', { count: cvs.length });
      }),
      catchError((error) => {
        // 404 is expected when no CVs have been uploaded yet
        if (error.status === 404) {
          this.logger.debug('[HTTP_INFO] No CVs available (404)', {
            status: error.status,
          });
        } else {
          this.logger.error('[HTTP_ERROR] Failed to fetch all CVs', {
            status: error.status,
            message: error.message,
          });
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * Set a specific CV as current (admin only)
   * @param cvId CV ID to set as current
   * @returns Observable of updated CV
   */
  setCurrentCv(cvId: number): Observable<CvResponse> {
    this.logger.info('[HTTP_REQUEST] Setting CV as current', { cvId });

    return this.http.put<CvResponse>(`${this.apiUrl}/${cvId}/set-current`, {}).pipe(
      tap((cv) => {
        this.logger.info('[HTTP_SUCCESS] CV set as current', { id: cv.id });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to set CV as current', {
          cvId,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Delete a CV (admin only)
   * @param cvId CV ID to delete
   * @returns Observable of void
   */
  deleteCv(cvId: number): Observable<void> {
    this.logger.info('[HTTP_REQUEST] Deleting CV', { cvId });

    return this.http.delete<void>(`${this.apiUrl}/${cvId}`).pipe(
      tap(() => {
        this.logger.info('[HTTP_SUCCESS] CV deleted', { cvId });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to delete CV', {
          cvId,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }
}
