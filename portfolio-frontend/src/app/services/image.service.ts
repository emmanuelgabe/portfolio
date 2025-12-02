import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpEvent } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ImageUploadResponse } from '../models/image-upload-response.model';
import { LoggerService } from './logger.service';

/**
 * Service for project image upload and management
 * Handles optimized images and thumbnail generation
 */
@Injectable({
  providedIn: 'root',
})
export class ImageService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/admin/projects`;

  /**
   * Upload project image with progress tracking
   */
  uploadProjectImage(
    projectId: number,
    file: File,
    reportProgress: true
  ): Observable<HttpEvent<ImageUploadResponse>>;

  /**
   * Upload project image without progress tracking
   */
  uploadProjectImage(
    projectId: number,
    file: File,
    reportProgress?: false
  ): Observable<ImageUploadResponse>;

  /**
   * Upload project image
   * Generates optimized image (1200px max width, WebP) and thumbnail (300x300px)
   *
   * @param projectId Project ID
   * @param file Image file to upload
   * @param reportProgress Whether to report upload progress
   * @returns Observable of ImageUploadResponse or HttpEvent for progress tracking
   */
  uploadProjectImage(
    projectId: number,
    file: File,
    reportProgress = false
  ): Observable<ImageUploadResponse> | Observable<HttpEvent<ImageUploadResponse>> {
    this.logger.info('[HTTP] POST /api/admin/projects/{id}/image', {
      projectId,
      fileName: file.name,
    });

    const formData = new FormData();
    formData.append('file', file);

    if (reportProgress) {
      return this.http.post<ImageUploadResponse>(`${this.apiUrl}/${projectId}/image`, formData, {
        reportProgress: true,
        observe: 'events',
      });
    } else {
      return this.http
        .post<ImageUploadResponse>(`${this.apiUrl}/${projectId}/image`, formData)
        .pipe(
          catchError((error) => {
            this.logger.error('[HTTP] Upload project image failed', {
              projectId,
              fileName: file.name,
              status: error.status,
              message: error.message,
            });
            return throwError(() => error);
          })
        );
    }
  }

  /**
   * Delete project image
   * Removes both optimized image and thumbnail
   *
   * @param projectId Project ID
   * @returns Observable of void
   */
  deleteProjectImage(projectId: number): Observable<void> {
    this.logger.info('[HTTP] DELETE /api/admin/projects/{id}/image', { projectId });

    return this.http.delete<void>(`${this.apiUrl}/${projectId}/image`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP] Delete project image failed', {
          projectId,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }
}
