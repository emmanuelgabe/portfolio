import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ExperienceService } from '../../../../services/experience.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { ExperienceType } from '../../../../models';

@Component({
  selector: 'app-experience-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslateModule],
  templateUrl: './experience-form.component.html',
  styleUrl: './experience-form.component.css',
})
export class ExperienceFormComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly experienceService = inject(ExperienceService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly logger = inject(LoggerService);
  private readonly translate = inject(TranslateService);
  readonly demoModeService = inject(DemoModeService);
  private readonly destroy$ = new Subject<void>();

  experienceForm!: FormGroup;
  isEditMode = false;
  experienceId?: number;
  loading = false;
  submitting = false;

  readonly experienceTypes = Object.values(ExperienceType);

  ngOnInit(): void {
    this.initForm();
    this.checkMode();
    this.subscribeToDemoMode();
  }

  initForm(): void {
    this.experienceForm = this.fb.group({
      company: ['', [Validators.required, Validators.minLength(2)]],
      role: ['', [Validators.required, Validators.minLength(2)]],
      startDate: ['', Validators.required],
      endDate: [''],
      description: ['', [Validators.required, Validators.minLength(10)]],
      type: [ExperienceType.WORK, Validators.required],
    });
  }

  subscribeToDemoMode(): void {
    this.updateFormDisabledState();
  }

  updateFormDisabledState(): void {
    const isDemo = this.demoModeService.isDemo();

    if (isDemo) {
      this.experienceForm.get('startDate')?.disable();
      this.experienceForm.get('endDate')?.disable();
      this.experienceForm.get('type')?.disable();
    } else {
      this.experienceForm.get('startDate')?.enable();
      this.experienceForm.get('endDate')?.enable();
      this.experienceForm.get('type')?.enable();
    }
  }

  checkMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.experienceId = +id;
      this.loadExperience(this.experienceId);
    }
  }

  loadExperience(id: number): void {
    this.loading = true;
    this.experienceService
      .getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (experience) => {
          this.experienceForm.patchValue({
            company: experience.company,
            role: experience.role,
            startDate: experience.startDate,
            endDate: experience.endDate || '',
            description: experience.description,
            type: experience.type,
          });
          this.loading = false;
        },
        error: (err) => {
          this.logger.error('[HTTP_ERROR] Failed to load experience', {
            id,
            error: err.message || err,
          });
          this.router.navigate(['/admin/experiences']);
        },
      });
  }

  onSubmit(): void {
    if (this.demoModeService.isDemo()) {
      return;
    }

    if (this.experienceForm.invalid) {
      this.experienceForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const formValue = this.experienceForm.value;

    if (this.isEditMode && this.experienceId) {
      this.experienceService
        .update(this.experienceId, formValue)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.router.navigate(['/admin/experiences']);
          },
          error: (err) => {
            this.logger.error('[HTTP_ERROR] Failed to update experience', {
              id: this.experienceId,
              error: err.message || err,
            });
            this.submitting = false;
          },
        });
    } else {
      this.experienceService
        .create(formValue)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.router.navigate(['/admin/experiences']);
          },
          error: (err) => {
            this.logger.error('[HTTP_ERROR] Failed to create experience', {
              company: formValue.company,
              error: err.message || err,
            });
            this.submitting = false;
          },
        });
    }
  }

  getTypeLabel(type: ExperienceType): string {
    const labels: Record<ExperienceType, string> = {
      [ExperienceType.WORK]: this.translate.instant('admin.experiences.work'),
      [ExperienceType.EDUCATION]: this.translate.instant('admin.experiences.education'),
      [ExperienceType.CERTIFICATION]: this.translate.instant('admin.experiences.certification'),
      [ExperienceType.VOLUNTEERING]: this.translate.instant('admin.experiences.volunteering'),
    };
    return labels[type];
  }

  goBack(): void {
    this.location.back();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
