import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ArticleImageResponse } from '../models/article.model';
import { LoggerService } from './logger.service';

/**
 * Service for managing article images
 * Handles HTTP requests for uploading and deleting article images
 */
@Injectable({
  providedIn: 'root',
})
export class ArticleImageService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/admin/articles`;

  /**
   * Upload image for article (admin only)
   */
  uploadImage(articleId: number, file: File): Observable<ArticleImageResponse> {
    this.logger.info('[HTTP_REQUEST] POST /api/admin/articles/:id/images', {
      articleId,
      fileName: file.name,
      fileSize: file.size,
    });

    const formData = new FormData();
    formData.append('file', file);

    return this.http
      .post<ArticleImageResponse>(`${this.apiUrl}/${articleId}/images`, formData)
      .pipe(
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Upload article image failed', {
            articleId,
            fileName: file.name,
            error,
          });
          return throwError(() => error);
        })
      );
  }

  /**
   * Delete image from article (admin only)
   */
  deleteImage(articleId: number, imageId: number): Observable<void> {
    this.logger.info('[HTTP_REQUEST] DELETE /api/admin/articles/:articleId/images/:imageId', {
      articleId,
      imageId,
    });

    return this.http.delete<void>(`${this.apiUrl}/${articleId}/images/${imageId}`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Delete article image failed', {
          articleId,
          imageId,
          error,
        });
        return throwError(() => error);
      })
    );
  }
}
