import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ProjectService } from '../../../../services/project.service';
import { ModalService } from '../../../../services/modal.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { ProjectResponse } from '../../../../models/project.model';
import { ToastrService } from 'ngx-toastr';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.scss'],
})
export class ProjectListComponent implements OnInit, OnDestroy {
  private readonly projectService = inject(ProjectService);
  private readonly modalService = inject(ModalService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  readonly demoModeService = inject(DemoModeService);
  private readonly destroy$ = new Subject<void>();

  projects: ProjectResponse[] = [];
  loading = false;
  error?: string;

  // Default placeholder image as SVG data URL
  readonly defaultImage =
    'data:image/svg+xml;base64,' +
    btoa(`
    <svg xmlns="http://www.w3.org/2000/svg" width="150" height="100" viewBox="0 0 150 100">
      <rect width="150" height="100" fill="#e9ecef"/>
      <text x="50%" y="50%" text-anchor="middle" dy=".3em" fill="#6c757d" font-family="Arial, sans-serif" font-size="14">No Image</text>
    </svg>
  `);

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading = true;
    this.error = undefined;

    this.logger.info('[HTTP_REQUEST] Loading projects list');

    this.projectService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (projects) => {
          this.projects = projects;
          this.loading = false;
          this.logger.info('[HTTP_SUCCESS] Projects loaded', { count: projects.length });
        },
        error: (error) => {
          this.error = 'Failed to load projects';
          this.loading = false;
          this.logger.error('[HTTP_ERROR] Failed to load projects', { error });
          this.toastr.error('Erreur lors du chargement des projets');
        },
      });
  }

  deleteProject(project: ProjectResponse): void {
    this.logger.info('[USER_ACTION] Delete requested', { id: project.id, title: project.title });

    this.modalService.confirmDelete(project.title, this.demoModeService.isDemo()).subscribe({
      next: (confirmed) => {
        if (confirmed) {
          if (this.demoModeService.isDemo()) {
            this.toastr.info('Action non disponible en mode démonstration');
            this.logger.info('[USER_ACTION] Delete blocked - demo mode', { id: project.id });
            return;
          }
          this.performDelete(project);
        } else {
          this.logger.info('[USER_ACTION] Delete cancelled', { id: project.id });
        }
      },
      error: () => {
        this.logger.info('[USER_ACTION] Delete modal dismissed', { id: project.id });
      },
    });
  }

  private performDelete(project: ProjectResponse): void {
    this.logger.info('[HTTP_REQUEST] Deleting project', { id: project.id });

    this.projectService
      .delete(project.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.projects = this.projects.filter((p) => p.id !== project.id);
          this.logger.info('[HTTP_SUCCESS] Project deleted', { id: project.id });
          this.toastr.success(`Projet "${project.title}" supprime`, 'Suppression reussie');
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to delete project', { id: project.id, error });
          this.toastr.error('Erreur lors de la suppression', 'Erreur');
        },
      });
  }

  getImageUrl(imageUrl?: string): string {
    if (!imageUrl) {
      return this.defaultImage;
    }
    return imageUrl.startsWith('http') ? imageUrl : `${environment.apiUrl}${imageUrl}`;
  }

  getTechArray(techStack: string): string[] {
    return techStack
      .split(',')
      .map((tech) => tech.trim())
      .filter((tech) => tech.length > 0);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
