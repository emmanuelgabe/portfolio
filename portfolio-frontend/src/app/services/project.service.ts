import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ProjectResponse, CreateProjectRequest, UpdateProjectRequest } from '../models';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/projects`;

  /**
   * Get all projects
   * @returns Observable of all projects
   */
  getAll(): Observable<ProjectResponse[]> {
    this.logger.debug('[HTTP_REQUEST] Fetching all projects');

    return this.http.get<ProjectResponse[]>(this.apiUrl).pipe(
      tap((projects) => {
        this.logger.info('[HTTP_SUCCESS] Projects fetched', { count: projects.length });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to fetch projects', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get project by ID
   * @param id Project ID
   * @returns Observable of project details
   */
  getById(id: number): Observable<ProjectResponse> {
    this.logger.debug('[HTTP_REQUEST] Fetching project', { id });

    return this.http.get<ProjectResponse>(`${this.apiUrl}/${id}`).pipe(
      tap((project) => {
        this.logger.debug('[HTTP_SUCCESS] Project fetched', {
          id: project.id,
          title: project.title,
        });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to fetch project', {
          id,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Create a new project
   * @param request Create project request
   * @returns Observable of created project
   */
  create(request: CreateProjectRequest): Observable<ProjectResponse> {
    this.logger.info('[HTTP_REQUEST] Creating project', { title: request.title });

    return this.http.post<ProjectResponse>(this.apiUrl, request).pipe(
      tap((project) => {
        this.logger.info('[HTTP_SUCCESS] Project created', {
          id: project.id,
          title: project.title,
        });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to create project', {
          title: request.title,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Update an existing project
   * @param id Project ID
   * @param request Update project request
   * @returns Observable of updated project
   */
  update(id: number, request: UpdateProjectRequest): Observable<ProjectResponse> {
    this.logger.info('[HTTP_REQUEST] Updating project', { id });

    return this.http.put<ProjectResponse>(`${this.apiUrl}/${id}`, request).pipe(
      tap((project) => {
        this.logger.info('[HTTP_SUCCESS] Project updated', {
          id: project.id,
          title: project.title,
        });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to update project', {
          id,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Delete a project
   * @param id Project ID
   * @returns Observable of void
   */
  delete(id: number): Observable<void> {
    this.logger.info('[HTTP_REQUEST] Deleting project', { id });

    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => {
        this.logger.info('[HTTP_SUCCESS] Project deleted', { id });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to delete project', {
          id,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get all featured projects
   * @returns Observable of featured projects
   */
  getFeatured(): Observable<ProjectResponse[]> {
    this.logger.debug('[HTTP_REQUEST] Fetching featured projects');

    return this.http.get<ProjectResponse[]>(`${this.apiUrl}/featured`).pipe(
      tap((projects) => {
        this.logger.info('[HTTP_SUCCESS] Featured projects fetched', { count: projects.length });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to fetch featured projects', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Search projects by title
   * @param title Title to search for
   * @returns Observable of matching projects
   */
  searchByTitle(title: string): Observable<ProjectResponse[]> {
    this.logger.debug('[HTTP_REQUEST] Searching projects by title', { title });

    const params = new HttpParams().set('title', title);
    return this.http.get<ProjectResponse[]>(`${this.apiUrl}/search/title`, { params }).pipe(
      tap((projects) => {
        this.logger.info('[HTTP_SUCCESS] Projects search completed', {
          title,
          count: projects.length,
        });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to search projects by title', {
          title,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Search projects by technology
   * @param technology Technology to search for
   * @returns Observable of matching projects
   */
  searchByTechnology(technology: string): Observable<ProjectResponse[]> {
    this.logger.debug('[HTTP_REQUEST] Searching projects by technology', { technology });

    const params = new HttpParams().set('technology', technology);
    return this.http.get<ProjectResponse[]>(`${this.apiUrl}/search/technology`, { params }).pipe(
      tap((projects) => {
        this.logger.info('[HTTP_SUCCESS] Technology search completed', {
          technology,
          count: projects.length,
        });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to search projects by technology', {
          technology,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }
}
