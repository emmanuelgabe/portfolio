import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  SiteConfigurationResponse,
  UpdateSiteConfigurationRequest,
} from '../models/site-configuration.model';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class SiteConfigurationService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/configuration`;
  private readonly adminApiUrl = `${environment.apiUrl}/api/admin/configuration`;

  /**
   * Get site configuration (public endpoint)
   * @returns Observable of site configuration
   */
  getSiteConfiguration(): Observable<SiteConfigurationResponse> {
    this.logger.debug('[HTTP] Fetching site configuration');

    return this.http.get<SiteConfigurationResponse>(this.apiUrl).pipe(
      tap((config) => {
        this.logger.debug('[HTTP] Site configuration fetched', { fullName: config.fullName });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to fetch site configuration', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Update site configuration (Admin only)
   * @param request Update site configuration request
   * @returns Observable of updated site configuration
   */
  updateSiteConfiguration(
    request: UpdateSiteConfigurationRequest
  ): Observable<SiteConfigurationResponse> {
    this.logger.info('[HTTP] PUT /api/admin/configuration', { fullName: request.fullName });

    return this.http.put<SiteConfigurationResponse>(this.adminApiUrl, request).pipe(
      tap((config) => {
        this.logger.info('[HTTP] Site configuration updated', { fullName: config.fullName });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to update site configuration', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Upload profile image (Admin only)
   * @param file Image file to upload
   * @returns Observable of updated site configuration
   */
  uploadProfileImage(file: File): Observable<SiteConfigurationResponse> {
    this.logger.info('[HTTP] POST /api/admin/configuration/profile-image', {
      fileName: file.name,
      size: file.size,
    });

    const formData = new FormData();
    formData.append('file', file);

    return this.http
      .post<SiteConfigurationResponse>(`${this.adminApiUrl}/profile-image`, formData)
      .pipe(
        tap((config) => {
          this.logger.info('[HTTP] Profile image uploaded', {
            profileImageUrl: config.profileImageUrl,
          });
        }),
        catchError((error) => {
          this.logger.error('[HTTP_ERROR] Failed to upload profile image', {
            status: error.status,
            message: error.message,
          });
          return throwError(() => error);
        })
      );
  }

  /**
   * Delete profile image (Admin only)
   * @returns Observable of updated site configuration
   */
  deleteProfileImage(): Observable<SiteConfigurationResponse> {
    this.logger.info('[HTTP] DELETE /api/admin/configuration/profile-image');

    return this.http.delete<SiteConfigurationResponse>(`${this.adminApiUrl}/profile-image`).pipe(
      tap(() => {
        this.logger.info('[HTTP] Profile image deleted');
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to delete profile image', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }
}
