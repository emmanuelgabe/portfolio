import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  ArticleResponse,
  CreateArticleRequest,
  UpdateArticleRequest,
  Page,
} from '../models/article.model';
import { LoggerService } from './logger.service';

/**
 * Service for managing blog articles
 * Handles HTTP requests for article CRUD operations
 */
@Injectable({
  providedIn: 'root',
})
export class ArticleService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/articles`;

  /**
   * Get all published articles (public)
   */
  getAll(): Observable<ArticleResponse[]> {
    return this.http.get<ArticleResponse[]>(this.apiUrl).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch articles failed', { error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get published articles with pagination (public)
   */
  getAllPaginated(page: number = 0, size: number = 10): Observable<Page<ArticleResponse>> {
    return this.http
      .get<Page<ArticleResponse>>(`${this.apiUrl}/paginated`, {
        params: { page: page.toString(), size: size.toString() },
      })
      .pipe(
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Fetch paginated articles failed', { page, size, error });
          return throwError(() => error);
        })
      );
  }

  /**
   * Get article by slug (public)
   */
  getBySlug(slug: string): Observable<ArticleResponse> {
    return this.http.get<ArticleResponse>(`${this.apiUrl}/${slug}`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch article by slug failed', { slug, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get all articles including drafts (admin only)
   */
  getAllAdmin(): Observable<ArticleResponse[]> {
    return this.http.get<ArticleResponse[]>(`${environment.apiUrl}/api/admin/articles`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch all articles (admin) failed', { error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get article by ID (admin only)
   */
  getById(id: number): Observable<ArticleResponse> {
    return this.http.get<ArticleResponse>(`${environment.apiUrl}/api/admin/articles/${id}`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch article by ID failed', { id, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Create new article (admin only)
   */
  create(request: CreateArticleRequest): Observable<ArticleResponse> {
    this.logger.info('[HTTP_REQUEST] POST /api/admin/articles', { title: request.title });

    return this.http
      .post<ArticleResponse>(`${environment.apiUrl}/api/admin/articles`, request)
      .pipe(
        tap((article) => {
          this.logger.info('[HTTP_SUCCESS] Article created', {
            id: article.id,
            title: article.title,
          });
        }),
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Create article failed', { error });
          return throwError(() => error);
        })
      );
  }

  /**
   * Update existing article (admin only)
   */
  update(id: number, request: UpdateArticleRequest): Observable<ArticleResponse> {
    this.logger.info('[HTTP_REQUEST] PUT /api/admin/articles/:id', { id });

    return this.http
      .put<ArticleResponse>(`${environment.apiUrl}/api/admin/articles/${id}`, request)
      .pipe(
        tap((article) => {
          this.logger.info('[HTTP_SUCCESS] Article updated', {
            id: article.id,
            title: article.title,
          });
        }),
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Update article failed', { id, error });
          return throwError(() => error);
        })
      );
  }

  /**
   * Delete article (admin only)
   */
  delete(id: number): Observable<void> {
    this.logger.info('[HTTP_REQUEST] DELETE /api/admin/articles/:id', { id });

    return this.http.delete<void>(`${environment.apiUrl}/api/admin/articles/${id}`).pipe(
      tap(() => {
        this.logger.info('[HTTP_SUCCESS] Article deleted', { id });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Delete article failed', { id, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Publish article (admin only)
   */
  publish(id: number): Observable<ArticleResponse> {
    this.logger.info('[HTTP_REQUEST] PUT /api/admin/articles/:id/publish', { id });

    return this.http
      .put<ArticleResponse>(`${environment.apiUrl}/api/admin/articles/${id}/publish`, {})
      .pipe(
        tap((article) => {
          this.logger.info('[HTTP_SUCCESS] Article published', {
            id: article.id,
            title: article.title,
          });
        }),
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Publish article failed', { id, error });
          return throwError(() => error);
        })
      );
  }

  /**
   * Unpublish article (admin only)
   */
  unpublish(id: number): Observable<ArticleResponse> {
    this.logger.info('[HTTP_REQUEST] PUT /api/admin/articles/:id/unpublish', { id });

    return this.http
      .put<ArticleResponse>(`${environment.apiUrl}/api/admin/articles/${id}/unpublish`, {})
      .pipe(
        tap((article) => {
          this.logger.info('[HTTP_SUCCESS] Article unpublished', {
            id: article.id,
            title: article.title,
          });
        }),
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Unpublish article failed', { id, error });
          return throwError(() => error);
        })
      );
  }

  /**
   * Reorder articles (admin only)
   * @param orderedIds Ordered list of article IDs
   * @returns Observable of void
   */
  reorder(orderedIds: number[]): Observable<void> {
    this.logger.info('[HTTP_REQUEST] PUT /api/admin/articles/reorder', {
      count: orderedIds.length,
    });

    return this.http
      .put<void>(`${environment.apiUrl}/api/admin/articles/reorder`, { orderedIds })
      .pipe(
        tap(() => {
          this.logger.info('[HTTP_SUCCESS] Articles reordered');
        }),
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Reorder articles failed', { error });
          return throwError(() => error);
        })
      );
  }
}
