import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { ProjectCardComponent } from '../project-card/project-card.component';
import { SkeletonProjectCardComponent } from '../shared/skeleton';
import { ProjectService } from '../../services/project.service';
import { ProjectResponse } from '../../models';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [CommonModule, ProjectCardComponent, SkeletonProjectCardComponent],
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.css'],
})
export class ProjectListComponent implements OnInit, OnDestroy {
  private readonly projectService = inject(ProjectService);
  private readonly logger = inject(LoggerService);
  private readonly destroy$ = new Subject<void>();

  projects: ProjectResponse[] = [];
  isLoading = true;
  error: string | undefined;

  ngOnInit(): void {
    this.loadProjects();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load all projects from the API
   */
  loadProjects(): void {
    this.isLoading = true;
    this.error = undefined;

    this.projectService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (projects) => {
          this.projects = projects;
          this.isLoading = false;
        },
        error: (err) => {
          this.logger.error('[HTTP_ERROR] Failed to load projects', { error: err.message || err });
          this.error = 'Failed to load projects. Please try again later.';
          this.isLoading = false;
        },
      });
  }

  /**
   * Retry loading projects
   */
  retry(): void {
    this.loadProjects();
  }

  /**
   * Check if there are no projects to display
   */
  get hasNoProjects(): boolean {
    return !this.isLoading && !this.error && this.projects.length === 0;
  }
}
