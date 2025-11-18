import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { ProjectService } from '../../../../services/project.service';
import { FileUploadService } from '../../../../services/file-upload.service';
import { LoggerService } from '../../../../services/logger.service';
import { ToastrService } from 'ngx-toastr';
import { CreateProjectRequest, UpdateProjectRequest } from '../../../../models/project.model';

@Component({
  selector: 'app-project-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './project-form.component.html',
  styleUrls: ['./project-form.component.scss'],
})
export class ProjectFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly projectService = inject(ProjectService);
  private readonly fileUploadService = inject(FileUploadService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);

  projectForm!: FormGroup;
  isEditMode = false;
  projectId: number | null = null;
  loading = false;
  submitting = false;
  uploadingImage = false;
  imagePreview: string | null = null;
  selectedFile: File | null = null;

  ngOnInit(): void {
    this.initForm();
    this.checkMode();
  }

  initForm(): void {
    this.projectForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      techStack: ['', Validators.required],
      githubUrl: [''],
      demoUrl: [''],
      imageUrl: [''],
      featured: [false],
    });
  }

  checkMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.projectId = +id;
      this.loadProject(this.projectId);
    }
  }

  loadProject(id: number): void {
    this.loading = true;
    this.logger.info('[HTTP_REQUEST] Loading project', { id });

    this.projectService.getById(id).subscribe({
      next: (project) => {
        this.logger.info('[HTTP_SUCCESS] Project loaded', { id, title: project.title });

        this.projectForm.patchValue({
          title: project.title,
          description: project.description,
          techStack: project.techStack,
          githubUrl: project.githubUrl || '',
          demoUrl: project.demoUrl || '',
          imageUrl: project.imageUrl || '',
          featured: project.featured,
        });

        if (project.imageUrl) {
          this.imagePreview = this.getFullImageUrl(project.imageUrl);
        }

        this.loading = false;
      },
      error: (error) => {
        this.logger.error('[HTTP_ERROR] Failed to load project', { id, error });
        this.toastr.error('Erreur lors du chargement du projet');
        this.loading = false;
        this.router.navigate(['/admin/projects']);
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];

      // Preview
      const reader = new FileReader();
      reader.onload = (e) => {
        this.imagePreview = e.target?.result as string;
      };
      reader.readAsDataURL(this.selectedFile);

      // Upload immediately
      this.uploadImage();
    }
  }

  uploadImage(): void {
    if (!this.selectedFile) return;

    this.uploadingImage = true;
    this.logger.info('[HTTP_REQUEST] Uploading image', { fileName: this.selectedFile.name });

    this.fileUploadService.uploadImage(this.selectedFile).subscribe({
      next: (response) => {
        this.logger.info('[HTTP_SUCCESS] Image uploaded', { fileUrl: response.fileUrl });
        this.projectForm.patchValue({ imageUrl: response.fileUrl });
        this.uploadingImage = false;
        this.toastr.success('Image uploadée avec succès');
      },
      error: (error) => {
        this.logger.error('[HTTP_ERROR] Image upload failed', { error });
        this.uploadingImage = false;
        this.toastr.error("Erreur lors de l'upload de l'image");
        this.imagePreview = null;
        this.selectedFile = null;
      },
    });
  }

  removeImage(): void {
    this.imagePreview = null;
    this.selectedFile = null;
    this.projectForm.patchValue({ imageUrl: '' });
  }

  onSubmit(): void {
    if (this.projectForm.invalid) {
      this.markFormGroupTouched(this.projectForm);
      this.toastr.warning('Veuillez remplir tous les champs requis');
      return;
    }

    if (this.uploadingImage) {
      this.toastr.warning("Veuillez attendre la fin de l'upload de l'image");
      return;
    }

    if (this.isEditMode) {
      this.updateProject();
    } else {
      this.createProject();
    }
  }

  createProject(): void {
    this.submitting = true;
    const formValue = this.projectForm.value;

    const request: CreateProjectRequest = {
      title: formValue.title,
      description: formValue.description,
      techStack: formValue.techStack,
      githubUrl: formValue.githubUrl || undefined,
      demoUrl: formValue.demoUrl || undefined,
      imageUrl: formValue.imageUrl || undefined,
      featured: formValue.featured,
    };

    this.logger.info('[HTTP_REQUEST] Creating project', { title: request.title });

    this.projectService.create(request).subscribe({
      next: (project) => {
        this.logger.info('[HTTP_SUCCESS] Project created', { id: project.id });
        this.toastr.success(`Projet "${project.title}" créé avec succès`);
        this.submitting = false;
        this.router.navigate(['/admin/projects']);
      },
      error: (error) => {
        this.logger.error('[HTTP_ERROR] Failed to create project', { error });
        this.toastr.error('Erreur lors de la création du projet');
        this.submitting = false;
      },
    });
  }

  updateProject(): void {
    if (!this.projectId) return;

    this.submitting = true;
    const formValue = this.projectForm.value;

    const request: UpdateProjectRequest = {
      title: formValue.title,
      description: formValue.description,
      techStack: formValue.techStack,
      githubUrl: formValue.githubUrl || undefined,
      demoUrl: formValue.demoUrl || undefined,
      imageUrl: formValue.imageUrl || undefined,
      featured: formValue.featured,
    };

    this.logger.info('[HTTP_REQUEST] Updating project', { id: this.projectId });

    this.projectService.update(this.projectId, request).subscribe({
      next: (project) => {
        this.logger.info('[HTTP_SUCCESS] Project updated', { id: project.id });
        this.toastr.success(`Projet "${project.title}" mis à jour avec succès`);
        this.submitting = false;
        this.router.navigate(['/admin/projects']);
      },
      error: (error) => {
        this.logger.error('[HTTP_ERROR] Failed to update project', { error });
        this.toastr.error('Erreur lors de la mise à jour du projet');
        this.submitting = false;
      },
    });
  }

  getFullImageUrl(imageUrl: string): string {
    if (!imageUrl) return '';
    return imageUrl.startsWith('http') ? imageUrl : `http://localhost:8080${imageUrl}`;
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.projectForm.get(fieldName);
    return !!(field?.invalid && (field?.touched || field?.dirty));
  }

  hasError(fieldName: string, errorType: string): boolean {
    const field = this.projectForm.get(fieldName);
    return !!(field?.hasError(errorType) && (field?.touched || field?.dirty));
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.get(key);
      control?.markAsTouched();
      control?.markAsDirty();
    });
  }
}
