import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { CreateTagRequest, TagResponse, UpdateTagRequest } from '../models/tag.model';
import { LoggerService } from './logger.service';

/**
 * Service for managing tags
 * Handles HTTP requests for tag operations
 */
@Injectable({
  providedIn: 'root',
})
export class TagService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/tags`;
  private readonly adminApiUrl = `${environment.apiUrl}/api/admin/tags`;

  /**
   * Get all tags
   */
  getAll(): Observable<TagResponse[]> {
    return this.http.get<TagResponse[]>(this.apiUrl).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch tags failed', { error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get tag by ID
   */
  getById(id: number): Observable<TagResponse> {
    return this.http.get<TagResponse>(`${this.apiUrl}/${id}`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch tag by ID failed', { id, status: error.status });
        return throwError(() => error);
      })
    );
  }

  /**
   * Create a new tag
   */
  create(request: CreateTagRequest): Observable<TagResponse> {
    this.logger.info('[HTTP_REQUEST] POST /api/admin/tags', {
      name: request.name,
      color: request.color,
    });

    return this.http.post<TagResponse>(this.adminApiUrl, request).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Create tag failed', {
          name: request.name,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Update an existing tag
   */
  update(id: number, request: UpdateTagRequest): Observable<TagResponse> {
    this.logger.info('[HTTP_REQUEST] PUT /api/admin/tags', {
      id,
      name: request.name,
      color: request.color,
    });

    return this.http.put<TagResponse>(`${this.adminApiUrl}/${id}`, request).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Update tag failed', {
          id,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Delete a tag
   */
  delete(id: number): Observable<void> {
    this.logger.info('[HTTP_REQUEST] DELETE /api/admin/tags', { id });

    return this.http.delete<void>(`${this.adminApiUrl}/${id}`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Delete tag failed', { id, status: error.status });
        return throwError(() => error);
      })
    );
  }
}
