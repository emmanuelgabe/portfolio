import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  Skill,
  CreateSkillRequest,
  UpdateSkillRequest,
  SkillCategory,
} from '../models/skill.model';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class SkillService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/skills`;

  /**
   * Get all skills ordered by display order
   * @returns Observable of all skills
   */
  getAll(): Observable<Skill[]> {
    this.logger.debug('[HTTP_REQUEST] Fetching all skills');

    return this.http.get<Skill[]>(this.apiUrl).pipe(
      tap((skills) => {
        this.logger.info('[HTTP_SUCCESS] Skills fetched', { count: skills.length });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to fetch skills', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get skill by ID
   * @param id Skill ID
   * @returns Observable of skill details
   */
  getById(id: number): Observable<Skill> {
    this.logger.debug('[HTTP_REQUEST] Fetching skill', { id });

    return this.http.get<Skill>(`${this.apiUrl}/${id}`).pipe(
      tap((skill) => {
        this.logger.debug('[HTTP_SUCCESS] Skill fetched', { id: skill.id, name: skill.name });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to fetch skill', {
          id,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Get skills by category
   * @param category Skill category
   * @returns Observable of skills in the specified category
   */
  getByCategory(category: SkillCategory): Observable<Skill[]> {
    this.logger.debug('[HTTP_REQUEST] Fetching skills by category', { category });

    return this.http.get<Skill[]>(`${this.apiUrl}/category/${category}`).pipe(
      tap((skills) => {
        this.logger.info('[HTTP_SUCCESS] Skills by category fetched', {
          category,
          count: skills.length,
        });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to fetch skills by category', {
          category,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Create a new skill (Admin only)
   * @param request Create skill request
   * @returns Observable of created skill
   */
  create(request: CreateSkillRequest): Observable<Skill> {
    this.logger.info('[HTTP_REQUEST] Creating skill', { name: request.name });

    return this.http.post<Skill>(`${this.apiUrl}/admin`, request).pipe(
      tap((skill) => {
        this.logger.info('[HTTP_SUCCESS] Skill created', {
          id: skill.id,
          name: skill.name,
        });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to create skill', {
          name: request.name,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Update an existing skill (Admin only)
   * @param id Skill ID
   * @param request Update skill request
   * @returns Observable of updated skill
   */
  update(id: number, request: UpdateSkillRequest): Observable<Skill> {
    this.logger.info('[HTTP_REQUEST] Updating skill', { id });

    return this.http.put<Skill>(`${this.apiUrl}/admin/${id}`, request).pipe(
      tap((skill) => {
        this.logger.info('[HTTP_SUCCESS] Skill updated', {
          id: skill.id,
          name: skill.name,
        });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to update skill', {
          id,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Delete a skill (Admin only)
   * @param id Skill ID
   * @returns Observable of void
   */
  delete(id: number): Observable<void> {
    this.logger.info('[HTTP_REQUEST] Deleting skill', { id });

    return this.http.delete<void>(`${this.apiUrl}/admin/${id}`).pipe(
      tap(() => {
        this.logger.info('[HTTP_SUCCESS] Skill deleted', { id });
      }),
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Failed to delete skill', {
          id,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }
}
