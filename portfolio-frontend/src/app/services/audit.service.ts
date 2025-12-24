import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AuditLogResponse, AuditLogFilter, Page } from '../models/audit.model';
import { LoggerService } from './logger.service';

/**
 * Service for managing audit logs (admin only)
 * Handles HTTP requests for audit log queries and exports
 */
@Injectable({
  providedIn: 'root',
})
export class AuditService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/admin/audit`;

  /**
   * Get paginated audit logs with optional filters
   */
  getAuditLogs(
    filter: AuditLogFilter = {},
    page: number = 0,
    size: number = 20
  ): Observable<Page<AuditLogResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');

    if (filter.action) {
      params = params.set('action', filter.action);
    }
    if (filter.entityType) {
      params = params.set('entityType', filter.entityType);
    }
    if (filter.entityId !== undefined) {
      params = params.set('entityId', filter.entityId.toString());
    }
    if (filter.username) {
      params = params.set('username', filter.username);
    }
    if (filter.success !== undefined) {
      params = params.set('success', filter.success.toString());
    }
    if (filter.startDate) {
      params = params.set('startDate', filter.startDate);
    }
    if (filter.endDate) {
      params = params.set('endDate', filter.endDate);
    }

    return this.http.get<Page<AuditLogResponse>>(this.apiUrl, { params }).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch audit logs failed', { filter, page, size, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get audit history for a specific entity
   */
  getEntityHistory(entityType: string, entityId: number): Observable<AuditLogResponse[]> {
    return this.http
      .get<AuditLogResponse[]>(`${this.apiUrl}/entity/${entityType}/${entityId}`)
      .pipe(
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Fetch entity history failed', {
            entityType,
            entityId,
            error,
          });
          return throwError(() => error);
        })
      );
  }

  /**
   * Get available audit actions
   */
  getActions(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/actions`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch audit actions failed', { error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get available entity types
   */
  getEntityTypes(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/entity-types`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch entity types failed', { error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Export audit logs to CSV and trigger download
   */
  exportCsv(filter: AuditLogFilter = {}): void {
    this.logger.info('[HTTP_REQUEST] GET /api/admin/audit/export/csv');

    let params = new HttpParams();
    if (filter.action) {
      params = params.set('action', filter.action);
    }
    if (filter.entityType) {
      params = params.set('entityType', filter.entityType);
    }
    if (filter.startDate) {
      params = params.set('startDate', filter.startDate);
    }
    if (filter.endDate) {
      params = params.set('endDate', filter.endDate);
    }

    this.http
      .get(`${this.apiUrl}/export/csv`, {
        params,
        responseType: 'blob',
      })
      .subscribe({
        next: (blob) => {
          this.downloadFile(blob, 'audit-logs.csv', 'text/csv');
          this.logger.info('[HTTP_SUCCESS] Audit CSV exported');
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Export CSV failed', { error });
        },
      });
  }

  /**
   * Export audit logs to JSON and trigger download
   */
  exportJson(filter: AuditLogFilter = {}): void {
    this.logger.info('[HTTP_REQUEST] GET /api/admin/audit/export/json');

    let params = new HttpParams();
    if (filter.action) {
      params = params.set('action', filter.action);
    }
    if (filter.entityType) {
      params = params.set('entityType', filter.entityType);
    }
    if (filter.startDate) {
      params = params.set('startDate', filter.startDate);
    }
    if (filter.endDate) {
      params = params.set('endDate', filter.endDate);
    }

    this.http
      .get(`${this.apiUrl}/export/json`, {
        params,
        responseType: 'blob',
      })
      .subscribe({
        next: (blob) => {
          this.downloadFile(blob, 'audit-logs.json', 'application/json');
          this.logger.info('[HTTP_SUCCESS] Audit JSON exported');
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Export JSON failed', { error });
        },
      });
  }

  /**
   * Trigger file download in browser
   */
  private downloadFile(blob: Blob, filename: string, mimeType: string): void {
    const url = window.URL.createObjectURL(new Blob([blob], { type: mimeType }));
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    window.URL.revokeObjectURL(url);
  }
}
