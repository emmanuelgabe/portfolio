import { Component, inject, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { SafeHtml } from '@angular/platform-browser';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { ExperienceService } from '../../../../services/experience.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { MarkdownService } from '../../../../services/markdown.service';
import { ExperienceType } from '../../../../models';
import { MonthYearPickerComponent } from '../../../../components/shared/month-year-picker/month-year-picker.component';

@Component({
  selector: 'app-experience-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    TranslateModule,
    MonthYearPickerComponent,
  ],
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
  private readonly toastr = inject(ToastrService);
  private readonly markdownService = inject(MarkdownService);
  readonly demoModeService = inject(DemoModeService);
  private readonly destroy$ = new Subject<void>();

  experienceForm!: FormGroup;
  isEditMode = false;
  experienceId?: number;
  loading = false;
  submitting = false;
  showPreview = false;
  descriptionPreview: SafeHtml = '';

  readonly experienceTypes = Object.values(ExperienceType);

  @ViewChild('descriptionTextarea') descriptionTextarea!: ElementRef<HTMLTextAreaElement>;

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

    // Use getRawValue() to include disabled controls
    const formValue = this.experienceForm.getRawValue();

    // Convert empty endDate to null for backend compatibility
    if (!formValue.endDate) {
      formValue.endDate = null;
    }

    // Validate that endDate is after startDate if endDate is provided
    if (formValue.endDate && formValue.startDate) {
      const startDate = new Date(formValue.startDate);
      const endDate = new Date(formValue.endDate);
      if (endDate < startDate) {
        this.toastr.error(
          this.translate.instant('admin.experiences.endDateBeforeStartDate'),
          this.translate.instant('admin.common.validationError')
        );
        return;
      }
    }

    this.submitting = true;

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

  togglePreview(): void {
    this.showPreview = !this.showPreview;
    if (this.showPreview) {
      this.updatePreview();
    }
  }

  updatePreview(): void {
    const description = this.experienceForm.get('description')?.value || '';
    this.descriptionPreview = this.markdownService.toSafeHtml(description);
  }

  insertBold(): void {
    this.wrapSelection('**', '**');
  }

  insertItalic(): void {
    this.wrapSelection('*', '*');
  }

  insertBulletList(): void {
    this.insertAtLineStart('- ');
  }

  insertNumberedList(): void {
    this.insertAtLineStart('1. ');
  }

  insertLink(): void {
    const textarea = this.descriptionTextarea?.nativeElement;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;
    const selectedText = text.substring(start, end) || 'texte';

    const newText = text.substring(0, start) + `[${selectedText}](url)` + text.substring(end);
    this.updateDescription(newText);

    setTimeout(() => {
      const linkStart = start + selectedText.length + 3;
      textarea.focus();
      textarea.setSelectionRange(linkStart, linkStart + 3);
    });
  }

  private wrapSelection(before: string, after: string): void {
    const textarea = this.descriptionTextarea?.nativeElement;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;
    const selectedText = text.substring(start, end);

    const newText = text.substring(0, start) + before + selectedText + after + text.substring(end);
    this.updateDescription(newText);

    setTimeout(() => {
      textarea.focus();
      if (selectedText) {
        textarea.setSelectionRange(start + before.length, end + before.length);
      } else {
        textarea.setSelectionRange(start + before.length, start + before.length);
      }
    });
  }

  private insertAtLineStart(prefix: string): void {
    const textarea = this.descriptionTextarea?.nativeElement;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const text = textarea.value;

    const lineStart = text.lastIndexOf('\n', start - 1) + 1;
    const newText = text.substring(0, lineStart) + prefix + text.substring(lineStart);
    this.updateDescription(newText);

    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(start + prefix.length, start + prefix.length);
    });
  }

  private updateDescription(value: string): void {
    this.experienceForm.patchValue({ description: value });
  }

  goBack(): void {
    this.location.back();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
