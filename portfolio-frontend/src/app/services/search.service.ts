import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  ArticleSearchResult,
  ProjectSearchResult,
  ExperienceSearchResult,
} from '../models/search.model';
import { LoggerService } from './logger.service';

/**
 * Service for full-text search functionality in admin panel.
 * Communicates with Elasticsearch-backed search API.
 */
@Injectable({
  providedIn: 'root',
})
export class SearchService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/admin/search`;

  /**
   * Search articles by query string.
   */
  searchArticles(query: string): Observable<ArticleSearchResult[]> {
    this.logger.info('[HTTP] GET /api/admin/search/articles', { query });

    return this.http
      .get<ArticleSearchResult[]>(`${this.apiUrl}/articles`, {
        params: { q: query },
      })
      .pipe(
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Search articles failed', {
            query,
            status: error.status,
            message: error.message,
          });
          return throwError(() => error);
        })
      );
  }

  /**
   * Search projects by query string.
   */
  searchProjects(query: string): Observable<ProjectSearchResult[]> {
    this.logger.info('[HTTP] GET /api/admin/search/projects', { query });

    return this.http
      .get<ProjectSearchResult[]>(`${this.apiUrl}/projects`, {
        params: { q: query },
      })
      .pipe(
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Search projects failed', {
            query,
            status: error.status,
            message: error.message,
          });
          return throwError(() => error);
        })
      );
  }

  /**
   * Search experiences by query string.
   */
  searchExperiences(query: string): Observable<ExperienceSearchResult[]> {
    this.logger.info('[HTTP] GET /api/admin/search/experiences', { query });

    return this.http
      .get<ExperienceSearchResult[]>(`${this.apiUrl}/experiences`, {
        params: { q: query },
      })
      .pipe(
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Search experiences failed', {
            query,
            status: error.status,
            message: error.message,
          });
          return throwError(() => error);
        })
      );
  }
}
