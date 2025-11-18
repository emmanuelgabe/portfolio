import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { LoggerService } from './logger.service';
import { environment } from '../../environments/environment';

export interface FileUploadResponse {
  fileName: string;
  fileUrl: string;
  fileType: string;
  fileSize: number;
}

@Injectable({
  providedIn: 'root',
})
export class FileUploadService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/admin/upload`;

  uploadImage(file: File): Observable<FileUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);

    this.logger.info('[HTTP_REQUEST] Uploading image', {
      fileName: file.name,
      fileSize: file.size,
      fileType: file.type,
    });

    return this.http.post<FileUploadResponse>(`${this.apiUrl}/image`, formData).pipe(
      tap((response) => {
        this.logger.info('[HTTP_SUCCESS] Image uploaded successfully', {
          fileName: response.fileName,
          fileUrl: response.fileUrl,
        });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to upload image', {
          fileName: file.name,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }
}
