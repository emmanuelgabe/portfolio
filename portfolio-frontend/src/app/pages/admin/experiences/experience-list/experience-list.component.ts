import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SkeletonTableRowComponent } from '../../../../components/shared/skeleton';
import { SearchInputComponent } from '../../../../components/shared/search-input/search-input.component';
import { ExperienceService } from '../../../../services/experience.service';
import { SearchService } from '../../../../services/search.service';
import { ModalService } from '../../../../services/modal.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { ExperienceResponse, ExperienceType } from '../../../../models';
import { ExperienceSearchResult } from '../../../../models/search.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-experience-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    TranslateModule,
    SkeletonTableRowComponent,
    SearchInputComponent,
  ],
  templateUrl: './experience-list.component.html',
  styleUrl: './experience-list.component.css',
})
export class ExperienceListComponent implements OnInit, OnDestroy {
  private readonly experienceService = inject(ExperienceService);
  private readonly searchService = inject(SearchService);
  private readonly modalService = inject(ModalService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);
  readonly demoModeService = inject(DemoModeService);
  private readonly destroy$ = new Subject<void>();

  experiences: ExperienceResponse[] = [];
  loading = true;
  error?: string;

  // Search state
  searchQuery = '';
  searchResults: ExperienceSearchResult[] = [];
  isSearching = false;
  isSearchActive = false;

  readonly ExperienceType = ExperienceType;

  ngOnInit(): void {
    this.loadExperiences();
  }

  private loadExperiences(): void {
    this.loading = true;
    this.error = undefined;

    this.experienceService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (experiences) => {
          this.experiences = experiences;
          this.loading = false;
        },
        error: (err) => {
          this.error = this.translate.instant('admin.experiences.loadError');
          this.loading = false;
          this.logger.error('[HTTP_ERROR] Failed to load experiences', {
            error: err.message || err,
          });
        },
      });
  }

  getTypeLabel(type: ExperienceType): string {
    const labels: Record<ExperienceType, string> = {
      [ExperienceType.WORK]: this.translate.instant('admin.experiences.work'),
      [ExperienceType.EDUCATION]: this.translate.instant('admin.experiences.education'),
      [ExperienceType.CERTIFICATION]: this.translate.instant('admin.experiences.certification'),
      [ExperienceType.VOLUNTEERING]: this.translate.instant('admin.experiences.volunteering'),
    };
    return labels[type] || type;
  }

  getTypeBadgeClass(type: ExperienceType): string {
    const classes: Record<ExperienceType, string> = {
      [ExperienceType.WORK]: 'bg-primary',
      [ExperienceType.EDUCATION]: 'bg-success',
      [ExperienceType.CERTIFICATION]: 'bg-warning',
      [ExperienceType.VOLUNTEERING]: 'bg-info',
    };
    return classes[type] || 'bg-secondary';
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      month: 'short',
      year: 'numeric',
    });
  }

  confirmDelete(experience: ExperienceResponse): void {
    this.modalService
      .confirm({
        title: this.translate.instant('admin.common.confirmDelete'),
        message: this.translate.instant('admin.experiences.deleteConfirm', {
          role: experience.role,
          company: experience.company,
        }),
        confirmText: this.translate.instant('admin.common.delete'),
        cancelText: this.translate.instant('admin.common.cancel'),
        confirmButtonClass: 'btn-danger',
        disableConfirm: this.demoModeService.isDemo(),
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          if (this.demoModeService.isDemo()) {
            this.toastr.info(this.translate.instant('admin.common.demoModeDisabled'));
            return;
          }
          this.deleteExperience(experience.id);
        }
      });
  }

  private deleteExperience(id: number): void {
    this.experienceService
      .delete(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.experiences = this.experiences.filter((exp) => exp.id !== id);
        },
        error: (err) => {
          this.logger.error('[HTTP_ERROR] Failed to delete experience', {
            id,
            error: err.message || err,
          });
        },
      });
  }

  get filteredExperiences(): ExperienceResponse[] {
    if (this.isSearchActive) {
      if (this.searchResults.length === 0) {
        return [];
      }
      const searchIds = new Set(this.searchResults.map((r) => r.id));
      return this.experiences.filter((e) => searchIds.has(e.id));
    }
    return this.experiences;
  }

  onSearch(query: string): void {
    this.searchQuery = query;
    this.isSearching = true;
    this.logger.info('[SEARCH] Searching experiences', { query });

    this.searchService
      .searchExperiences(query)
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
