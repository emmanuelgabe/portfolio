import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SkeletonTableRowComponent } from '../../../../components/shared/skeleton';
import { SearchInputComponent } from '../../../../components/shared/search-input/search-input.component';
import { ProjectService } from '../../../../services/project.service';
import { SearchService } from '../../../../services/search.service';
import { ModalService } from '../../../../services/modal.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { ProjectResponse } from '../../../../models/project.model';
import { ProjectSearchResult } from '../../../../models/search.model';
import { ToastrService } from 'ngx-toastr';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    TranslateModule,
    SkeletonTableRowComponent,
    SearchInputComponent,
  ],
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.scss'],
})
export class ProjectListComponent implements OnInit, OnDestroy {
  private readonly projectService = inject(ProjectService);
  private readonly searchService = inject(SearchService);
  private readonly modalService = inject(ModalService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);
  readonly demoModeService = inject(DemoModeService);
  private readonly destroy$ = new Subject<void>();

  projects: ProjectResponse[] = [];
  loading = false;
  error?: string;

  // Search state
  searchQuery = '';
  searchResults: ProjectSearchResult[] = [];
  isSearching = false;
  isSearchActive = false;

  // Reorder state
  reordering = false;

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
          this.projects = projects.sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
          this.loading = false;
          this.logger.info('[HTTP_SUCCESS] Projects loaded', { count: projects.length });
        },
        error: (error) => {
          this.error = this.translate.instant('admin.projects.loadError');
          this.loading = false;
          this.logger.error('[HTTP_ERROR] Failed to load projects', { error });
          this.toastr.error(this.translate.instant('admin.projects.loadError'));
        },
      });
  }

  moveUp(index: number): void {
    if (index <= 0 || this.reordering || this.demoModeService.isDemo() || this.isSearchActive) {
      if (this.demoModeService.isDemo()) {
        this.toastr.info(this.translate.instant('admin.common.demoModeDisabled'));
      }
      return;
    }

    this.swapAndSave(index, index - 1);
  }

  moveDown(index: number): void {
    if (
      index >= this.projects.length - 1 ||
      this.reordering ||
      this.demoModeService.isDemo() ||
      this.isSearchActive
    ) {
      if (this.demoModeService.isDemo()) {
        this.toastr.info(this.translate.instant('admin.common.demoModeDisabled'));
      }
      return;
    }

    this.swapAndSave(index, index + 1);
  }

  private swapAndSave(fromIndex: number, toIndex: number): void {
    this.reordering = true;

    // Swap items in the array
    const temp = this.projects[fromIndex];
    this.projects[fromIndex] = this.projects[toIndex];
    this.projects[toIndex] = temp;

    // Update display order values
    this.projects.forEach((project, i) => (project.displayOrder = i));

    // Get ordered IDs
    const orderedIds = this.projects.map((p) => p.id);

    this.projectService
      .reorder(orderedIds)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.reordering = false;
          this.logger.info('[ADMIN_PROJECTS] Projects reordered');
        },
        error: (error) => {
          this.reordering = false;
          this.logger.error('[ADMIN_PROJECTS] Failed to reorder projects', { error });
          this.toastr.error(this.translate.instant('admin.common.reorderError'));
          this.loadProjects(); // Reload to restore original order
        },
      });
  }

  deleteProject(project: ProjectResponse): void {
    this.logger.info('[USER_ACTION] Delete requested', { id: project.id, title: project.title });

    this.modalService.confirmDelete(project.title, this.demoModeService.isDemo()).subscribe({
      next: (confirmed) => {
        if (confirmed) {
          if (this.demoModeService.isDemo()) {
            this.toastr.info(this.translate.instant('admin.common.demoModeDisabled'));
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
          this.toastr.success(this.translate.instant('admin.projects.deleteSuccess'));
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to delete project', { id: project.id, error });
          this.toastr.error(this.translate.instant('admin.projects.deleteError'));
        },
      });
  }

  getImageUrl(imageUrl?: string): string {
    if (!imageUrl) {
      return this.defaultImage;
    }
    return imageUrl.startsWith('http') ? imageUrl : `${environment.apiUrl}${imageUrl}`;
  }

  /**
   * Check if the primary image of a project is ready to display.
   * Returns true if no images exist or if the primary image has status READY.
   */
  isPrimaryImageReady(project: ProjectResponse): boolean {
    if (!project.images || project.images.length === 0) {
      return true; // No images, show default
    }
    const primaryImage = project.images.find((img) => img.primary);
    if (!primaryImage) {
      return true; // No primary image, show default
    }
    return primaryImage.status === 'READY';
  }

  /**
   * Check if the primary image is currently processing.
   */
  isPrimaryImageProcessing(project: ProjectResponse): boolean {
    if (!project.images || project.images.length === 0) {
      return false;
    }
    const primaryImage = project.images.find((img) => img.primary);
    return primaryImage?.status === 'PROCESSING';
  }

  getTechArray(techStack: string): string[] {
    return techStack
      .split(',')
      .map((tech) => tech.trim())
      .filter((tech) => tech.length > 0);
  }

  get filteredProjects(): ProjectResponse[] {
    if (this.isSearchActive) {
      if (this.searchResults.length === 0) {
        return [];
      }
      const searchIds = new Set(this.searchResults.map((r) => r.id));
      return this.projects.filter((p) => searchIds.has(p.id));
    }
    return this.projects;
  }

  onSearch(query: string): void {
    this.searchQuery = query;
    this.isSearching = true;
    this.logger.info('[SEARCH] Searching projects', { query });

    this.searchService
      .searchProjects(query)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (results) => {
          this.searchResults = results;
          this.isSearchActive = true;
          this.isSearching = false;
          this.logger.info('[SEARCH] Search completed', { count: results.length });
        },
        error: (error) => {
          this.isSearching = false;
          this.logger.error('[SEARCH] Search failed', { error });
          this.toastr.error(this.translate.instant('admin.common.searchError'));
        },
      });
  }

  onSearchClear(): void {
    this.searchQuery = '';
    this.searchResults = [];
    this.isSearchActive = false;
    this.logger.info('[SEARCH] Search cleared');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
