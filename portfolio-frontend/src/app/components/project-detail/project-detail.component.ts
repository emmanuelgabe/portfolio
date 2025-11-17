import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { ProjectResponse } from '../../models';
import { LoggerService } from '../../services/logger.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './project-detail.component.html',
  styleUrls: ['./project-detail.component.css'],
})
export class ProjectDetailComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);

  project: ProjectResponse | null = null;
  isLoading = true;
  error: string | null = null;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadProject(+id);
    } else {
      this.error = 'Invalid project ID';
      this.isLoading = false;
    }
  }

  /**
   * Load project details from API
   */
  loadProject(id: number): void {
    this.isLoading = true;
    this.error = null;

    this.projectService.getById(id).subscribe({
      next: (project) => {
        this.project = project;
        this.isLoading = false;
      },
      error: (err) => {
        this.logger.error('[HTTP_ERROR] Failed to load project', {
          projectId: id,
          error: err.message || err,
        });
        this.error = 'Project not found or failed to load. Please try again.';
        this.isLoading = false;
      },
    });
  }

  /**
   * Navigate back to projects list
   */
  goBack(): void {
    this.router.navigate(['/projects']);
  }

  /**
   * Retry loading the project
   */
  retry(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadProject(+id);
    }
  }

  /**
   * Share on Twitter
   */
  shareOnTwitter(): void {
    if (!this.project) return;
    const text = encodeURIComponent(`Check out ${this.project.title}!`);
    const url = encodeURIComponent(window.location.href);
    window.open(`https://twitter.com/intent/tweet?text=${text}&url=${url}`, '_blank');
  }

  /**
   * Share on LinkedIn
   */
  shareOnLinkedIn(): void {
    if (!this.project) return;
    const url = encodeURIComponent(window.location.href);
    window.open(`https://www.linkedin.com/sharing/share-offsite/?url=${url}`, '_blank');
  }

  /**
   * Copy link to clipboard
   */
  copyLink(): void {
    const url = window.location.href;
    navigator.clipboard
      .writeText(url)
      .then(() => {
        this.toastr.success('Link copied to clipboard!', 'Success');
      })
      .catch((err) => {
        this.logger.error('[CLIPBOARD_ERROR] Failed to copy link', { error: err.message || err });
        this.toastr.error('Failed to copy link to clipboard', 'Error');
      });
  }

  /**
   * Get tech stack as array
   */
  get techStackArray(): string[] {
    return this.project?.techStack.split(',').map((tech) => tech.trim()) || [];
  }

  /**
   * Check if project has an image
   */
  get hasImage(): boolean {
    return !!this.project?.imageUrl;
  }

  /**
   * Get image source with fallback
   */
  get imageSrc(): string {
    return this.project?.imageUrl || 'assets/images/project-placeholder.png';
  }
}
