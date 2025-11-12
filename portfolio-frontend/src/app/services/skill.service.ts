import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Skill,
  CreateSkillRequest,
  UpdateSkillRequest,
  SkillCategory
} from '../models/skill.model';

@Injectable({
  providedIn: 'root'
})
export class SkillService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/skills`;

  /**
   * Get all skills ordered by display order
   * @returns Observable of all skills
   */
  getAll(): Observable<Skill[]> {
    return this.http.get<Skill[]>(this.apiUrl);
  }

  /**
   * Get skill by ID
   * @param id Skill ID
   * @returns Observable of skill details
   */
  getById(id: number): Observable<Skill> {
    return this.http.get<Skill>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get skills by category
   * @param category Skill category
   * @returns Observable of skills in the specified category
   */
  getByCategory(category: SkillCategory): Observable<Skill[]> {
    return this.http.get<Skill[]>(`${this.apiUrl}/category/${category}`);
  }

  /**
   * Create a new skill (Admin only)
   * @param request Create skill request
   * @returns Observable of created skill
   */
  create(request: CreateSkillRequest): Observable<Skill> {
    return this.http.post<Skill>(`${this.apiUrl}/admin`, request);
  }

  /**
   * Update an existing skill (Admin only)
   * @param id Skill ID
   * @param request Update skill request
   * @returns Observable of updated skill
   */
  update(id: number, request: UpdateSkillRequest): Observable<Skill> {
    return this.http.put<Skill>(`${this.apiUrl}/admin/${id}`, request);
  }

  /**
   * Delete a skill (Admin only)
   * @param id Skill ID
   * @returns Observable of void
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/${id}`);
  }
}
