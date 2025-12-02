import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SkillService } from '../../../../services/skill.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { ToastrService } from 'ngx-toastr';
import { IconPickerComponent } from '../../../../components/shared/icon-picker/icon-picker.component';
import {
  CreateSkillRequest,
  UpdateSkillRequest,
  SkillCategory,
  IconType,
} from '../../../../models/skill.model';

@Component({
  selector: 'app-skill-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, IconPickerComponent],
  templateUrl: './skill-form.component.html',
  styleUrls: ['./skill-form.component.scss'],
})
export class SkillFormComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly skillService = inject(SkillService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  readonly demoModeService = inject(DemoModeService);
  private readonly destroy$ = new Subject<void>();

  skillForm!: FormGroup;
  isEditMode = false;
  skillId?: number;
  loading = false;
  submitting = false;
  uploadingIcon = false;
  svgPreview?: string;

  readonly IconType = IconType;

  categories = [
    { value: SkillCategory.FRONTEND, label: 'Frontend' },
    { value: SkillCategory.BACKEND, label: 'Backend' },
    { value: SkillCategory.DATABASE, label: 'Database' },
    { value: SkillCategory.DEVOPS, label: 'DevOps' },
    { value: SkillCategory.TOOLS, label: 'Tools' },
  ];

  iconTypes = [
    { value: IconType.FONT_AWESOME, label: 'Font Awesome' },
    { value: IconType.CUSTOM_SVG, label: 'SVG personnalise' },
  ];

  ngOnInit(): void {
    this.initForm();
    this.checkMode();
    this.subscribeToDemoMode();
  }

  initForm(): void {
    this.skillForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      iconType: [IconType.FONT_AWESOME, Validators.required],
      icon: ['fa-solid fa-code'],
      customIconUrl: [''],
      color: ['#007bff', Validators.required],
      category: [SkillCategory.FRONTEND, Validators.required],
      displayOrder: [0, [Validators.required, Validators.min(0)]],
    });
  }

  onIconTypeChange(): void {
    const iconType = this.skillForm.get('iconType')?.value;
    if (iconType === IconType.FONT_AWESOME) {
      // Clear custom icon when switching to Font Awesome
      this.skillForm.patchValue({ customIconUrl: '' });
      this.svgPreview = undefined;
    } else {
      // Clear FA icon when switching to custom
      this.skillForm.patchValue({ icon: '' });
    }
  }

  onIconSelected(iconClass: string): void {
    this.skillForm.patchValue({ icon: iconClass });
  }

  onSvgFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];

    // Validate file type
    if (!file.name.toLowerCase().endsWith('.svg')) {
      this.toastr.error('Seuls les fichiers SVG sont acceptes');
      return;
    }

    // Validate file size (100KB max)
    if (file.size > 102400) {
      this.toastr.error('Le fichier ne doit pas depasser 100KB');
      return;
    }

    // Preview SVG
    const reader = new FileReader();
    reader.onload = (e) => {
      this.svgPreview = e.target?.result as string;
    };
    reader.readAsDataURL(file);

    // If in edit mode, upload immediately
    if (this.isEditMode && this.skillId) {
      this.uploadSvgIcon(file);
    } else {
      // Store file for upload after skill creation
      this.pendingSvgFile = file;
    }
  }

  private pendingSvgFile?: File;

  private uploadSvgIcon(file: File): void {
    if (!this.skillId) return;

    this.uploadingIcon = true;
    this.logger.info('[SKILL_FORM] Uploading SVG icon', { skillId: this.skillId });

    this.skillService
      .uploadIcon(this.skillId, file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (skill) => {
          this.logger.info('[SKILL_FORM] SVG icon uploaded', { skillId: this.skillId });
          this.skillForm.patchValue({
            iconType: IconType.CUSTOM_SVG,
            customIconUrl: skill.customIconUrl,
          });
          this.toastr.success('Icone uploadee avec succes');
          this.uploadingIcon = false;
        },
        error: (error) => {
          this.logger.error('[SKILL_FORM] Failed to upload SVG icon', { error });
          this.toastr.error("Erreur lors de l'upload de l'icone");
          this.uploadingIcon = false;
        },
      });
  }

  subscribeToDemoMode(): void {
    this.updateFormDisabledState();
  }

  updateFormDisabledState(): void {
    const isDemo = this.demoModeService.isDemo();

    if (isDemo) {
      this.skillForm.get('category')?.disable();
      this.skillForm.get('iconType')?.disable();
      this.skillForm.get('icon')?.disable();
      this.skillForm.get('color')?.disable();
    } else {
      this.skillForm.get('category')?.enable();
      this.skillForm.get('iconType')?.enable();
      this.skillForm.get('icon')?.enable();
      this.skillForm.get('color')?.enable();
    }
  }

  checkMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.skillId = +id;
      this.loadSkill(this.skillId);
    }
  }

  loadSkill(id: number): void {
    this.loading = true;
    this.logger.info('[SKILL_FORM] Loading skill', { id });

    this.skillService
      .getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (skill) => {
          this.logger.info('[SKILL_FORM] Skill loaded', { id, name: skill.name });

          this.skillForm.patchValue({
            name: skill.name,
            iconType: skill.iconType,
            icon: skill.icon || 'fa-solid fa-code',
            customIconUrl: skill.customIconUrl || '',
            color: skill.color,
            category: skill.category,
            displayOrder: skill.displayOrder,
          });

          // Set SVG preview if custom icon exists
          if (skill.iconType === IconType.CUSTOM_SVG && skill.customIconUrl) {
            this.svgPreview = skill.customIconUrl;
          }

          this.loading = false;
        },
        error: (error) => {
          this.logger.error('[SKILL_FORM] Failed to load skill', { id, error });
          this.toastr.error('Erreur lors du chargement de la competence');
          this.loading = false;
          this.router.navigate(['/admin/skills']);
        },
      });
  }

  onSubmit(): void {
    if (this.demoModeService.isDemo()) {
      return;
    }

    if (this.skillForm.invalid) {
      this.markFormGroupTouched(this.skillForm);
      this.toastr.warning('Veuillez remplir tous les champs requis');
      return;
    }

    if (this.isEditMode) {
      this.updateSkill();
    } else {
      this.createSkill();
    }
  }

  createSkill(): void {
    this.submitting = true;
    const formValue = this.skillForm.value;

    const request: CreateSkillRequest = {
      name: formValue.name,
      iconType: formValue.iconType,
      icon: formValue.iconType === IconType.FONT_AWESOME ? formValue.icon : undefined,
      customIconUrl: formValue.customIconUrl || undefined,
      color: formValue.color,
      category: formValue.category,
      displayOrder: formValue.displayOrder,
    };

    this.logger.info('[SKILL_FORM] Creating skill', { name: request.name });

    this.skillService
      .create(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (skill) => {
          this.logger.info('[SKILL_FORM] Skill created', { id: skill.id });

          // If there's a pending SVG file, upload it
          if (this.pendingSvgFile && formValue.iconType === IconType.CUSTOM_SVG) {
            this.skillId = skill.id;
            this.uploadSvgIcon(this.pendingSvgFile);
            this.pendingSvgFile = undefined;
          }

          this.toastr.success(`Competence "${skill.name}" creee avec succes`);
          this.submitting = false;
          this.router.navigate(['/admin/skills']);
        },
        error: (error) => {
          this.logger.error('[SKILL_FORM] Failed to create skill', { error });
          this.toastr.error('Erreur lors de la creation de la competence');
          this.submitting = false;
        },
      });
  }

  updateSkill(): void {
    if (!this.skillId) return;

    this.submitting = true;
    const formValue = this.skillForm.value;

    const request: UpdateSkillRequest = {
      name: formValue.name,
      iconType: formValue.iconType,
      icon: formValue.iconType === IconType.FONT_AWESOME ? formValue.icon : undefined,
      customIconUrl: formValue.customIconUrl || undefined,
      color: formValue.color,
      category: formValue.category,
      displayOrder: formValue.displayOrder,
    };

    this.logger.info('[SKILL_FORM] Updating skill', { id: this.skillId });

    this.skillService
      .update(this.skillId, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (skill) => {
          this.logger.info('[SKILL_FORM] Skill updated', { id: skill.id });
          this.toastr.success(`Competence "${skill.name}" mise a jour avec succes`);
          this.submitting = false;
          this.router.navigate(['/admin/skills']);
        },
        error: (error) => {
          this.logger.error('[SKILL_FORM] Failed to update skill', { error });
          this.toastr.error('Erreur lors de la mise a jour de la competence');
          this.submitting = false;
        },
      });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.skillForm.get(fieldName);
    return !!(field?.invalid && (field?.touched || field?.dirty));
  }

  hasError(fieldName: string, errorType: string): boolean {
    const field = this.skillForm.get(fieldName);
    return !!(field?.hasError(errorType) && (field?.touched || field?.dirty));
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.get(key);
      control?.markAsTouched();
      control?.markAsDirty();
    });
  }

  getCategoryLabel(categoryValue: string): string {
    const category = this.categories.find((c) => c.value === categoryValue);
    return category ? category.label : categoryValue;
  }

  goBack(): void {
    this.location.back();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
