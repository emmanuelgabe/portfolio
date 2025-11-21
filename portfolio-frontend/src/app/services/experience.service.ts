import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  ExperienceResponse,
  CreateExperienceRequest,
  UpdateExperienceRequest,
  ExperienceType,
} from '../models';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class ExperienceService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/experiences`;
  private readonly adminApiUrl = `${environment.apiUrl}/api/admin/experiences`;

  /**
   * Get all experiences ordered by start date descending
   * @returns Observable of all experiences
   */
  getAll(): Observable<ExperienceResponse[]> {
    return this.http.get<ExperienceResponse[]>(this.apiUrl).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to fetch experiences', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get experience by ID
   * @param id Experience ID
   * @returns Observable of experience details
   */
  getById(id: number): Observable<ExperienceResponse> {
    return this.http.get<ExperienceResponse>(`${this.apiUrl}/${id}`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to fetch experience', {
          id,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Create a new experience (Admin only)
   * @param request Create experience request
   * @returns Observable of created experience
   */
  create(request: CreateExperienceRequest): Observable<ExperienceResponse> {
    this.logger.info('[HTTP] POST /api/admin/experiences', {
      company: request.company,
      role: request.role,
    });

    return this.http.post<ExperienceResponse>(this.adminApiUrl, request).pipe(
      tap((experience) => {
        this.logger.info('[HTTP_SUCCESS] Experience created', {
          id: experience.id,
          company: experience.company,
        });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to create experience', {
          company: request.company,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Update an existing experience (Admin only)
   * @param id Experience ID
   * @param request Update experience request
   * @returns Observable of updated experience
   */
  update(id: number, request: UpdateExperienceRequest): Observable<ExperienceResponse> {
    this.logger.info('[HTTP] PUT /api/admin/experiences', { id });

    return this.http.put<ExperienceResponse>(`${this.adminApiUrl}/${id}`, request).pipe(
      tap((experience) => {
        this.logger.info('[HTTP_SUCCESS] Experience updated', {
          id: experience.id,
          company: experience.company,
        });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to update experience', {
          id,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Delete an experience (Admin only)
   * @param id Experience ID
   * @returns Observable of void
   */
  delete(id: number): Observable<void> {
    this.logger.info('[HTTP] DELETE /api/admin/experiences', { id });

    return this.http.delete<void>(`${this.adminApiUrl}/${id}`).pipe(
      tap(() => {
        this.logger.info('[HTTP_SUCCESS] Experience deleted', { id });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to delete experience', {
          id,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get experiences filtered by type
   * @param type Experience type
   * @returns Observable of filtered experiences
   */
  getByType(type: ExperienceType): Observable<ExperienceResponse[]> {
    return this.http.get<ExperienceResponse[]>(`${this.apiUrl}/type/${type}`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to fetch experiences by type', {
          type,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get ongoing experiences (where endDate is null)
   * @returns Observable of ongoing experiences
   */
  getOngoing(): Observable<ExperienceResponse[]> {
    return this.http.get<ExperienceResponse[]>(`${this.apiUrl}/ongoing`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to fetch ongoing experiences', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get top N most recent experiences
   * Used for displaying a summary on the home page
   * @param limit Maximum number of experiences to return (default: 3)
   * @returns Observable of recent experiences
   */
  getRecent(limit: number = 3): Observable<ExperienceResponse[]> {
    return this.http
      .get<ExperienceResponse[]>(`${this.apiUrl}/recent`, {
        params: { limit: limit.toString() },
      })
      .pipe(
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Failed to fetch recent experiences', {
            limit,
            status: error.status,
            message: error.message,
          });
          return throwError(() => error);
        })
      );
  }
}
