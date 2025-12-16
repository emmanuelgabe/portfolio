import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { interval, Observable, throwError } from 'rxjs';
import { catchError, filter, map, switchMap, take, takeWhile } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  ImageStatus,
  ProjectImageResponse,
  ReorderProjectImagesRequest,
  UpdateProjectImageRequest,
} from '../models/project-image.model';
import { LoggerService } from './logger.service';

/**
 * Service for managing project images.
 */
@Injectable({
  providedIn: 'root',
})
export class ProjectImageService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/admin/projects`;

  /**
   * Upload a new image to a project.
   */
  uploadImage(
    projectId: number,
    file: File,
    altText?: string,
    caption?: string
  ): Observable<ProjectImageResponse> {
    this.logger.info('[HTTP] POST /api/admin/projects/:id/images', {
      projectId,
      fileName: file.name,
    });

    const formData = new FormData();
    formData.append('file', file);
    if (altText) {
      formData.append('altText', altText);
    }
    if (caption) {
      formData.append('caption', caption);
    }

    return this.http
      .post<ProjectImageResponse>(`${this.apiUrl}/${projectId}/images`, formData)
      .pipe(
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Upload project image failed', {
            projectId,
            error: error.message,
          });
          return throwError(() => error);
        })
      );
  }

  /**
   * Get all images for a project.
   */
  getImages(projectId: number): Observable<ProjectImageResponse[]> {
    return this.http.get<ProjectImageResponse[]>(`${this.apiUrl}/${projectId}/images`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Get project images failed', {
          projectId,
          error: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Delete an image from a project.
   */
  deleteImage(projectId: number, imageId: number): Observable<void> {
    this.logger.info('[HTTP] DELETE /api/admin/projects/:projectId/images/:imageId', {
      projectId,
      imageId,
    });

    return this.http.delete<void>(`${this.apiUrl}/${projectId}/images/${imageId}`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Delete project image failed', {
          projectId,
          imageId,
          error: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Update image metadata (alt text, caption).
   */
  updateImage(
    projectId: number,
    imageId: number,
    request: UpdateProjectImageRequest
  ): Observable<ProjectImageResponse> {
    this.logger.info('[HTTP] PUT /api/admin/projects/:projectId/images/:imageId', {
      projectId,
      imageId,
    });

    return this.http
      .put<ProjectImageResponse>(`${this.apiUrl}/${projectId}/images/${imageId}`, request)
      .pipe(
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Update project image failed', {
            projectId,
            imageId,
            error: error.message,
          });
          return throwError(() => error);
        })
      );
  }

  /**
   * Set an image as the primary/thumbnail image for the project.
   */
  setPrimaryImage(projectId: number, imageId: number): Observable<void> {
    this.logger.info('[HTTP] PUT /api/admin/projects/:projectId/images/:imageId/primary', {
      projectId,
      imageId,
    });

    return this.http.put<void>(`${this.apiUrl}/${projectId}/images/${imageId}/primary`, {}).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Set primary image failed', {
          projectId,
          imageId,
          error: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Reorder project images.
   */
  reorderImages(projectId: number, request: ReorderProjectImagesRequest): Observable<void> {
    this.logger.info('[HTTP] PUT /api/admin/projects/:projectId/images/reorder', {
      projectId,
      count: request.imageIds.length,
    });

    return this.http.put<void>(`${this.apiUrl}/${projectId}/images/reorder`, request).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Reorder images failed', {
          projectId,
          error: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get the processing status of an image.
   */
  getImageStatus(projectId: number, imageId: number): Observable<ImageStatus> {
    return this.http.get<ImageStatus>(`${this.apiUrl}/${projectId}/images/${imageId}/status`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Get image status failed', {
          projectId,
          imageId,
          error: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Poll for image status until READY or FAILED.
   * @param projectId Project ID
   * @param imageId Image ID
   * @param intervalMs Polling interval in milliseconds (default: 1000)
   * @param maxAttempts Maximum polling attempts (default: 30)
   */
  pollImageStatus(
    projectId: number,
    imageId: number,
    intervalMs = 1000,
    maxAttempts = 30
  ): Observable<ImageStatus> {
    this.logger.info('[POLL] Starting image status polling', { projectId, imageId });

    return interval(intervalMs).pipe(
      take(maxAttempts),
      switchMap(() => this.getImageStatus(projectId, imageId)),
      takeWhile((status) => status === 'PROCESSING', true),
      filter((status) => status !== 'PROCESSING'),
      map((status) => {
        this.logger.info('[POLL] Image status resolved', { projectId, imageId, status });
        return status;
      })
    );
  }
}
