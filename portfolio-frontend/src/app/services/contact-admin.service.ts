import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  ContactStatsResponse,
  ContactHistoryResponse,
  ContactCategory,
} from '../models/contact-moderation.model';
import { Page } from '../models/audit.model';
import { LoggerService } from './logger.service';

/**
 * Service for contact moderation administration (admin only).
 * Handles HTTP requests for contact stats, history, and categories.
 */
@Injectable({
  providedIn: 'root',
})
export class ContactAdminService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/admin/contacts`;

  /**
   * Get aggregated contact moderation statistics.
   */
  getStats(days: number = 30): Observable<ContactStatsResponse> {
    const params = new HttpParams().set('days', days.toString());

    return this.http.get<ContactStatsResponse>(`${this.apiUrl}/stats`, { params }).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch contact stats failed', { days, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get paginated contact history with optional category filter.
   */
  getHistory(
    category: ContactCategory | undefined,
    page: number = 0,
    size: number = 20
  ): Observable<Page<ContactHistoryResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');

    if (category) {
      params = params.set('category', category);
    }

    return this.http.get<Page<ContactHistoryResponse>>(`${this.apiUrl}/history`, { params }).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch contact history failed', {
          category,
          page,
          size,
          error,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get available contact categories for filtering.
   */
  getCategories(): Observable<ContactCategory[]> {
    return this.http.get<ContactCategory[]>(`${this.apiUrl}/categories`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch contact categories failed', { error });
        return throwError(() => error);
      })
    );
  }
}
