import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { SkillService } from '../../../../services/skill.service';
import { LoggerService } from '../../../../services/logger.service';
import { ToastrService } from 'ngx-toastr';
import {
  CreateSkillRequest,
  UpdateSkillRequest,
  SkillCategory,
} from '../../../../models/skill.model';

@Component({
  selector: 'app-skill-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './skill-form.component.html',
  styleUrls: ['./skill-form.component.scss'],
})
export class SkillFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly skillService = inject(SkillService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);

  skillForm!: FormGroup;
  isEditMode = false;
  skillId: number | null = null;
  loading = false;
  submitting = false;

  categories = [
    { value: SkillCategory.FRONTEND, label: 'Frontend' },
    { value: SkillCategory.BACKEND, label: 'Backend' },
    { value: SkillCategory.DATABASE, label: 'Database' },
    { value: SkillCategory.DEVOPS, label: 'DevOps' },
    { value: SkillCategory.TOOLS, label: 'Tools' },
  ];

  commonIcons = [
    'bi-code-slash',
    'bi-gear',
    'bi-database',
    'bi-cloud',
    'bi-server',
    'bi-terminal',
    'bi-braces',
    'bi-cpu',
    'bi-diagram-3',
    'bi-filetype-html',
    'bi-filetype-css',
    'bi-filetype-js',
    'bi-filetype-java',
    'bi-filetype-py',
    'bi-git',
  ];

  ngOnInit(): void {
    this.initForm();
    this.checkMode();
  }

  initForm(): void {
    this.skillForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      icon: ['bi-code-slash', Validators.required],
      color: ['#007bff', Validators.required],
      category: [SkillCategory.FRONTEND, Validators.required],
      displayOrder: [0, [Validators.required, Validators.min(0)]],
    });
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

    this.skillService.getById(id).subscribe({
      next: (skill) => {
        this.logger.info('[SKILL_FORM] Skill loaded', { id, name: skill.name });

        this.skillForm.patchValue({
          name: skill.name,
          icon: skill.icon,
          color: skill.color,
          category: skill.category,
          displayOrder: skill.displayOrder,
        });

        this.loading = false;
      },
      error: (error) => {
        this.logger.error('[SKILL_FORM] Failed to load skill', { id, error });
        this.toastr.error('Erreur lors du chargement de la compétence');
        this.loading = false;
        this.router.navigate(['/admin/skills']);
      },
    });
  }

  onSubmit(): void {
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
      icon: formValue.icon,
      color: formValue.color,
      category: formValue.category,
      displayOrder: formValue.displayOrder,
    };

    this.logger.info('[SKILL_FORM] Creating skill', { name: request.name });

    this.skillService.create(request).subscribe({
      next: (skill) => {
        this.logger.info('[SKILL_FORM] Skill created', { id: skill.id });
        this.toastr.success(`Compétence "${skill.name}" créée avec succès`);
        this.submitting = false;
        this.router.navigate(['/admin/skills']);
      },
      error: (error) => {
        this.logger.error('[SKILL_FORM] Failed to create skill', { error });
        this.toastr.error('Erreur lors de la création de la compétence');
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
      icon: formValue.icon,
      color: formValue.color,
      category: formValue.category,
      displayOrder: formValue.displayOrder,
    };

    this.logger.info('[SKILL_FORM] Updating skill', { id: this.skillId });

    this.skillService.update(this.skillId, request).subscribe({
      next: (skill) => {
        this.logger.info('[SKILL_FORM] Skill updated', { id: skill.id });
        this.toastr.success(`Compétence "${skill.name}" mise à jour avec succès`);
        this.submitting = false;
        this.router.navigate(['/admin/skills']);
      },
      error: (error) => {
        this.logger.error('[SKILL_FORM] Failed to update skill', { error });
        this.toastr.error('Erreur lors de la mise à jour de la compétence');
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
}
