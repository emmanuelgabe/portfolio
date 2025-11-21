import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ExperienceService } from '../../../../services/experience.service';
import { ModalService } from '../../../../services/modal.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { ExperienceResponse, ExperienceType } from '../../../../models';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-experience-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './experience-list.component.html',
  styleUrl: './experience-list.component.css',
})
export class ExperienceListComponent implements OnInit, OnDestroy {
  private readonly experienceService = inject(ExperienceService);
  private readonly modalService = inject(ModalService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  readonly demoModeService = inject(DemoModeService);
  private readonly destroy$ = new Subject<void>();

  experiences: ExperienceResponse[] = [];
  loading = true;
  error?: string;

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
          this.error = 'Failed to load experiences';
          this.loading = false;
          this.logger.error('[HTTP_ERROR] Failed to load experiences', {
            error: err.message || err,
          });
        },
      });
  }

  getTypeLabel(type: ExperienceType): string {
    const labels: Record<ExperienceType, string> = {
      [ExperienceType.WORK]: 'Travail',
      [ExperienceType.EDUCATION]: 'Formation',
      [ExperienceType.CERTIFICATION]: 'Certification',
      [ExperienceType.VOLUNTEERING]: 'Bénévolat',
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
        title: 'Confirmer la suppression',
        message: `Voulez-vous vraiment supprimer l'expérience "${experience.role}" chez ${experience.company} ?`,
        confirmText: 'Supprimer',
        cancelText: 'Annuler',
        confirmButtonClass: 'btn-danger',
        disableConfirm: this.demoModeService.isDemo(),
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          if (this.demoModeService.isDemo()) {
            this.toastr.info('Action non disponible en mode démonstration');
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

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
