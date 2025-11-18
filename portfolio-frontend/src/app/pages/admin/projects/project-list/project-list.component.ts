import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ProjectService } from '../../../../services/project.service';
import { ModalService } from '../../../../services/modal.service';
import { LoggerService } from '../../../../services/logger.service';
import { ProjectResponse } from '../../../../models/project.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.scss'],
})
export class ProjectListComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly modalService = inject(ModalService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);

  projects: ProjectResponse[] = [];
  loading = false;
  error: string | null = null;

  // Default placeholder image as SVG data URL
  readonly defaultImage = 'data:image/svg+xml;base64,' + btoa(`
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
    this.error = null;

    this.logger.info('[HTTP_REQUEST] Loading projects list');

    this.projectService.getAll().subscribe({
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

    this.modalService.confirmDelete(project.title).subscribe({
      next: (confirmed) => {
        if (confirmed) {
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

    this.projectService.delete(project.id).subscribe({
      next: () => {
        this.projects = this.projects.filter((p) => p.id !== project.id);
        this.logger.info('[HTTP_SUCCESS] Project deleted', { id: project.id });
        this.toastr.success(`Projet "${project.title}" supprimé`, 'Suppression réussie');
      },
      error: (error) => {
        this.logger.error('[HTTP_ERROR] Failed to delete project', { id: project.id, error });
        this.toastr.error('Erreur lors de la suppression', 'Erreur');
      },
    });
  }

  getImageUrl(imageUrl?: string | null): string {
    if (!imageUrl) {
      return this.defaultImage;
    }
    // If imageUrl starts with http, return as is, otherwise prepend API URL
    return imageUrl.startsWith('http') ? imageUrl : `http://localhost:8080${imageUrl}`;
  }

  getTechArray(techStack: string): string[] {
    return techStack
      .split(',')
      .map((tech) => tech.trim())
      .filter((tech) => tech.length > 0);
  }
}
