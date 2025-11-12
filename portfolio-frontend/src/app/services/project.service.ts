import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ProjectResponse,
  CreateProjectRequest,
  UpdateProjectRequest
} from '../models';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/projects`;

  /**
   * Get all projects
   * @returns Observable of all projects
   */
  getAll(): Observable<ProjectResponse[]> {
    return this.http.get<ProjectResponse[]>(this.apiUrl);
  }

  /**
   * Get project by ID
   * @param id Project ID
   * @returns Observable of project details
   */
  getById(id: number): Observable<ProjectResponse> {
    return this.http.get<ProjectResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Create a new project
   * @param request Create project request
   * @returns Observable of created project
   */
  create(request: CreateProjectRequest): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(this.apiUrl, request);
  }

  /**
   * Update an existing project
   * @param id Project ID
   * @param request Update project request
   * @returns Observable of updated project
   */
  update(id: number, request: UpdateProjectRequest): Observable<ProjectResponse> {
    return this.http.put<ProjectResponse>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Delete a project
   * @param id Project ID
   * @returns Observable of void
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get all featured projects
   * @returns Observable of featured projects
   */
  getFeatured(): Observable<ProjectResponse[]> {
    return this.http.get<ProjectResponse[]>(`${this.apiUrl}/featured`);
  }

  /**
   * Search projects by title
   * @param title Title to search for
   * @returns Observable of matching projects
   */
  searchByTitle(title: string): Observable<ProjectResponse[]> {
    const params = new HttpParams().set('title', title);
    return this.http.get<ProjectResponse[]>(`${this.apiUrl}/search/title`, { params });
  }

  /**
   * Search projects by technology
   * @param technology Technology to search for
   * @returns Observable of matching projects
   */
  searchByTechnology(technology: string): Observable<ProjectResponse[]> {
    const params = new HttpParams().set('technology', technology);
    return this.http.get<ProjectResponse[]>(`${this.apiUrl}/search/technology`, { params });
  }
}
