import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TagService } from '../../../../services/tag.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';

@Component({
  selector: 'app-tag-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './tag-form.component.html',
  styleUrl: './tag-form.component.scss',
})
export class TagFormComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly tagService = inject(TagService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly logger = inject(LoggerService);
  readonly demoModeService = inject(DemoModeService);
  private readonly destroy$ = new Subject<void>();

  tagForm!: FormGroup;
  isEditMode = false;
  tagId?: number;
  loading = false;
  submitting = false;

  ngOnInit(): void {
    this.initForm();
    this.checkMode();
    this.subscribeToDemoMode();
  }

  initForm(): void {
    this.tagForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      color: [
        '#007bff',
        [Validators.required, Validators.pattern(/^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/)],
      ],
    });
  }

  subscribeToDemoMode(): void {
    this.updateFormDisabledState();
  }

  updateFormDisabledState(): void {
    const isDemo = this.demoModeService.isDemo();

    if (isDemo) {
      this.tagForm.get('color')?.disable();
    } else {
      this.tagForm.get('color')?.enable();
    }
  }

  checkMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.tagId = +id;
      this.loadTag(this.tagId);
    }
  }

  loadTag(id: number): void {
    this.loading = true;
    this.tagService
      .getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (tag) => {
          this.tagForm.patchValue({
            name: tag.name,
            color: tag.color,
          });
          this.loading = false;
        },
        error: (err) => {
          this.logger.error('[HTTP_ERROR] Failed to load tag', {
            id,
            error: err.message || err,
          });
          this.router.navigate(['/admin/tags']);
        },
      });
  }

  onSubmit(): void {
    if (this.demoModeService.isDemo()) {
      return;
    }

    if (this.tagForm.invalid) {
      this.tagForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const formValue = this.tagForm.value;

    if (this.isEditMode && this.tagId) {
      this.tagService
        .update(this.tagId, formValue)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.router.navigate(['/admin/tags']);
          },
          error: (err) => {
            this.logger.error('[HTTP_ERROR] Failed to update tag', {
              id: this.tagId,
              error: err.message || err,
            });
            this.submitting = false;
          },
        });
    } else {
      this.tagService
        .create(formValue)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.router.navigate(['/admin/tags']);
          },
          error: (err) => {
            this.logger.error('[HTTP_ERROR] Failed to create tag', {
              error: err.message || err,
            });
            this.submitting = false;
          },
        });
    }
  }

  goBack(): void {
    this.location.back();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
