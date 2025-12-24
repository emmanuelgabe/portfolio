import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SkeletonProjectDetailComponent } from '../shared/skeleton';
import { ProjectService } from '../../services/project.service';
import { ProjectResponse } from '../../models';
import { ProjectImageResponse } from '../../models/project-image.model';
import { LoggerService } from '../../services/logger.service';
import { ToastrService } from 'ngx-toastr';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule, SkeletonProjectDetailComponent],
  templateUrl: './project-detail.component.html',
  styleUrls: ['./project-detail.component.css'],
})
export class ProjectDetailComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);

  project: ProjectResponse | undefined;
  isLoading = true;
  error: string | undefined;
  notFound = false;
  currentSlideIndex = 0;

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
   * Navigate to previous slide
   */
  prevSlide(): void {
    if (this.sortedImages.length === 0) return;
    this.currentSlideIndex =
      this.currentSlideIndex === 0 ? this.sortedImages.length - 1 : this.currentSlideIndex - 1;
  }

  /**
   * Navigate to next slide
   */
  nextSlide(): void {
    if (this.sortedImages.length === 0) return;
    this.currentSlideIndex =
      this.currentSlideIndex === this.sortedImages.length - 1 ? 0 : this.currentSlideIndex + 1;
  }

  /**
   * Go to specific slide
   */
  goToSlide(index: number): void {
    if (index >= 0 && index < this.sortedImages.length) {
      this.currentSlideIndex = index;
    }
  }

  /**
   * Load project details from API
   */
  loadProject(id: number): void {
    this.isLoading = true;
    this.error = undefined;
    this.notFound = false;
    this.currentSlideIndex = 0;

    this.projectService.getById(id).subscribe({
      next: (project) => {
        // Redirect to home if project has no details page
        if (!project.hasDetails) {
          this.logger.info('[NAVIGATION] Project has no details page, redirecting', {
            projectId: id,
          });
          this.router.navigate(['/']);
          return;
        }
        this.project = project;
        this.isLoading = false;
      },
      error: (err) => {
        this.logger.error('[HTTP_ERROR] Failed to load project', {
          projectId: id,
          error: err.message || err,
        });
        if (err.status === 404) {
          this.notFound = true;
        } else {
          this.error = this.translate.instant('errors.loadingFailed');
        }
        this.isLoading = false;
      },
    });
  }

  /**
   * Navigate back to projects list
   */
  goBack(): void {
    this.router.navigate(['/'], { fragment: 'projects' });
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
        this.toastr.success(this.translate.instant('projects.linkCopied'));
      })
      .catch((err) => {
        this.logger.error('[CLIPBOARD_ERROR] Failed to copy link', { error: err.message || err });
        this.toastr.error(this.translate.instant('projects.linkCopyError'));
      });
  }

  /**
   * Get tech stack as array
   */
  get techStackArray(): string[] {
    return this.project?.techStack.split(',').map((tech) => tech.trim()) || [];
  }

  /**
   * Check if project has multiple images (for carousel)
   */
  get hasMultipleImages(): boolean {
    return !!(this.project?.images && this.project.images.length > 0);
  }

  /**
   * Get sorted images for carousel display
   */
  get sortedImages(): ProjectImageResponse[] {
    if (!this.project?.images) return [];
    return [...this.project.images].sort((a, b) => a.displayOrder - b.displayOrder);
  }

  /**
   * Check if project has a single image (legacy fallback)
   */
  get hasLegacyImage(): boolean {
    return !this.hasMultipleImages && !!this.project?.imageUrl;
  }

  /**
   * Get legacy image source with fallback
   */
  get legacyImageSrc(): string {
    return this.getFullImageUrl(this.project?.imageUrl || '');
  }

  /**
   * Get full URL for an image
   */
  getFullImageUrl(imageUrl: string): string {
    if (!imageUrl) return 'assets/images/project-placeholder.svg';
    if (imageUrl.startsWith('http')) return imageUrl;
    return `${environment.apiUrl}${imageUrl}`;
  }

  /**
   * Check if project has any links (GitHub or Demo)
   */
  get hasLinks(): boolean {
    return !!(
      (this.project?.githubUrl && this.project.githubUrl.trim() !== '') ||
      (this.project?.demoUrl && this.project.demoUrl.trim() !== '')
    );
  }

  /**
   * Check if URL is an internal route (starts with /)
   */
  isInternalRoute(url: string): boolean {
    return url.startsWith('/');
  }
}
