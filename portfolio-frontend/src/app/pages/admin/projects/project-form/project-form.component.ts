import {
  Component,
  inject,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  FormsModule,
} from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subject, Subscription, forkJoin } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { ProjectService } from '../../../../services/project.service';
import { ProjectImageService } from '../../../../services/project-image.service';
import { TagService } from '../../../../services/tag.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { ToastrService } from 'ngx-toastr';
import { CreateProjectRequest, UpdateProjectRequest } from '../../../../models/project.model';
import { ProjectImageResponse } from '../../../../models/project-image.model';
import { TagResponse } from '../../../../models/tag.model';
import { AsyncImageDirective } from '../../../../directives/async-image.directive';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-project-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
    TranslateModule,
    AsyncImageDirective,
  ],
  templateUrl: './project-form.component.html',
  styleUrls: ['./project-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectFormComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly projectService = inject(ProjectService);
  private readonly projectImageService = inject(ProjectImageService);
  private readonly tagService = inject(TagService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);
  readonly demoModeService = inject(DemoModeService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroy$ = new Subject<void>();

  private readonly MAX_IMAGES = 10;

  projectForm!: FormGroup;
  isEditMode = false;
  projectId?: number;
  loading = false;
  submitting = false;
  availableTags: TagResponse[] = [];
  selectedTags: number[] = [];

  // Multi-image management
  projectImages: ProjectImageResponse[] = [];
  pendingFiles: { file: File; altText: string; preview?: string }[] = [];
  uploadingImageCount = 0;
  private pollingSubscriptions = new Map<number, Subscription>();

  ngOnInit(): void {
    this.initForm();
    this.loadTags();
    this.checkMode();
    this.subscribeToDemoMode();
  }

  initForm(): void {
    this.projectForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      techStack: [''],
      githubUrl: [''],
      demoUrl: [''],
      featured: [false],
      hasDetails: [true],
    });

    // Update techStack validation based on hasDetails
    this.updateTechStackValidation();
  }

  /**
   * Update techStack validation based on hasDetails value
   */
  private updateTechStackValidation(): void {
    const hasDetails = this.projectForm.get('hasDetails')?.value;
    const techStackControl = this.projectForm.get('techStack');

    if (hasDetails) {
      techStackControl?.setValidators([Validators.required]);
    } else {
      techStackControl?.clearValidators();
    }
    techStackControl?.updateValueAndValidity();
  }

  /**
   * Handle hasDetails checkbox change
   */
  onHasDetailsChange(): void {
    this.updateTechStackValidation();
  }

  subscribeToDemoMode(): void {
    this.updateFormDisabledState();
  }

  updateFormDisabledState(): void {
    const isDemo = this.demoModeService.isDemo();

    if (isDemo) {
      this.projectForm.get('featured')?.disable();
    } else {
      this.projectForm.get('featured')?.enable();
    }
  }

  checkMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.projectId = +id;
      this.loadProject(this.projectId);
    }
  }

  loadTags(): void {
    this.tagService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (tags) => {
          this.availableTags = tags;
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to load tags', { error });
          this.toastr.error(this.translate.instant('admin.projects.tagsLoadError'));
        },
      });
  }

  toggleTag(tagId: number): void {
    const index = this.selectedTags.indexOf(tagId);
    if (index > -1) {
      this.selectedTags.splice(index, 1);
    } else {
      this.selectedTags.push(tagId);
    }
  }

  isTagSelected(tagId: number): boolean {
    return this.selectedTags.includes(tagId);
  }

  loadProject(id: number): void {
    this.loading = true;
    this.logger.info('[HTTP_REQUEST] Loading project', { id });

    this.projectService
      .getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (project) => {
          this.logger.info('[HTTP_SUCCESS] Project loaded', { id, title: project.title });

          this.projectForm.patchValue({
            title: project.title,
            description: project.description,
            techStack: project.techStack,
            githubUrl: project.githubUrl || '',
            demoUrl: project.demoUrl || '',
            featured: project.featured,
            hasDetails: project.hasDetails,
          });

          // Update validation after loading hasDetails value
          this.updateTechStackValidation();

          if (project.tags && project.tags.length > 0) {
            this.selectedTags = project.tags.map((tag) => tag.id);
          }

          // Load project images for multi-image support
          if (project.images && project.images.length > 0) {
            this.projectImages = [...project.images].sort(
              (a, b) => a.displayOrder - b.displayOrder
            );
          }

          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to load project', { id, error });
          this.toastr.error(this.translate.instant('admin.projects.loadSingleError'));
          this.loading = false;
          this.cdr.markForCheck();
          this.router.navigate(['/admin/projects']);
        },
      });
  }

  onSubmit(): void {
    if (this.demoModeService.isDemo()) {
      return;
    }

    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      this.toastr.warning(this.translate.instant('admin.projects.fillRequired'));
      return;
    }

    if (this.uploadingImageCount > 0) {
      this.toastr.warning(this.translate.instant('admin.projects.waitForUpload'));
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
      techStack: formValue.hasDetails ? formValue.techStack : '',
      githubUrl: formValue.githubUrl?.trim() || undefined,
      demoUrl: formValue.demoUrl?.trim() || undefined,
      featured: formValue.featured,
      hasDetails: formValue.hasDetails,
      tagIds: this.selectedTags.length > 0 ? this.selectedTags : undefined,
    };

    this.logger.info('[HTTP_REQUEST] Creating project', { title: request.title });

    this.projectService
      .create(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (project) => {
          this.logger.info('[HTTP_SUCCESS] Project created', { id: project.id });
          this.projectId = project.id;

          // Upload images if any were selected
          if (this.pendingFiles.length > 0) {
            this.uploadImagesAfterCreate(project.title);
          } else {
            this.toastr.success(this.translate.instant('admin.projects.createSuccess'));
            this.submitting = false;
            this.cdr.markForCheck();
            this.router.navigate(['/admin/projects']);
          }
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to create project', { error });
          this.toastr.error(this.translate.instant('admin.projects.createError'));
          this.submitting = false;
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Upload pending images after project creation
   */
  private uploadImagesAfterCreate(_projectTitle: string): void {
    if (!this.projectId || this.pendingFiles.length === 0) {
      this.submitting = false;
      this.cdr.markForCheck();
      this.router.navigate(['/admin/projects']);
      return;
    }

    this.uploadingImageCount = this.pendingFiles.length;

    const uploads = this.pendingFiles.map((pending) =>
      this.projectImageService.uploadImage(
        this.projectId!,
        pending.file,
        pending.altText || undefined
      )
    );

    forkJoin(uploads)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.uploadingImageCount = 0;
          this.submitting = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.toastr.success(this.translate.instant('admin.projects.createSuccess'));
          this.pendingFiles = [];
          this.router.navigate(['/admin/projects']);
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to upload images after create', { error });
          this.toastr.warning(this.translate.instant('admin.projects.imagesUploadError'));
          this.router.navigate(['/admin/projects']);
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
      techStack: formValue.hasDetails ? formValue.techStack : '',
      githubUrl: formValue.githubUrl?.trim() || undefined,
      demoUrl: formValue.demoUrl?.trim() || undefined,
      featured: formValue.featured,
      hasDetails: formValue.hasDetails,
      tagIds: this.selectedTags.length > 0 ? this.selectedTags : undefined,
    };

    this.logger.info('[HTTP_REQUEST] Updating project', { id: this.projectId });

    this.projectService
      .update(this.projectId, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (project) => {
          this.logger.info('[HTTP_SUCCESS] Project updated', { id: project.id });
          this.toastr.success(this.translate.instant('admin.projects.updateSuccess'));
          this.submitting = false;
          this.cdr.markForCheck();
          this.router.navigate(['/admin/projects']);
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to update project', { error });
          this.toastr.error(this.translate.instant('admin.projects.updateError'));
          this.submitting = false;
          this.cdr.markForCheck();
        },
      });
  }

  getFullImageUrl(imageUrl: string): string {
    if (!imageUrl) return '';
    return imageUrl.startsWith('http') ? imageUrl : `${environment.apiUrl}${imageUrl}`;
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.projectForm.get(fieldName);
    return !!(field?.invalid && (field?.touched || field?.dirty));
  }

  hasError(fieldName: string, errorType: string): boolean {
    const field = this.projectForm.get(fieldName);
    return !!(field?.hasError(errorType) && (field?.touched || field?.dirty));
  }

  goBack(): void {
    this.location.back();
  }

  // ========== Multi-image Management ==========

  /**
   * Check if more images can be added
   */
  get canAddMoreImages(): boolean {
    return this.projectImages.length + this.pendingFiles.length < this.MAX_IMAGES;
  }

  /**
   * Get remaining image slots
   */
  get remainingSlots(): number {
    return this.MAX_IMAGES - this.projectImages.length - this.pendingFiles.length;
  }

  /**
   * Handle multiple files selection
   */
  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const files = Array.from(input.files).slice(0, this.remainingSlots);

    for (const file of files) {
      const pendingFile: { file: File; altText: string; preview?: string } = { file, altText: '' };
      this.pendingFiles.push(pendingFile);

      // Generate preview
      const reader = new FileReader();
      reader.onload = (e) => {
        pendingFile.preview = e.target?.result as string;
        this.cdr.markForCheck();
      };
      reader.readAsDataURL(file);
    }

    if (input.files.length > this.remainingSlots) {
      this.toastr.warning(
        this.translate.instant('admin.projects.maxImagesWarning', {
          max: this.MAX_IMAGES,
          ignored: input.files.length - this.remainingSlots,
        })
      );
    }

    input.value = '';
    this.cdr.markForCheck();
  }

  /**
   * Remove a pending file before upload
   */
  removePendingFile(index: number): void {
    this.pendingFiles.splice(index, 1);
  }

  /**
   * Upload all pending images
   */
  uploadPendingImages(): void {
    if (this.demoModeService.isDemo()) return;
    if (!this.projectId || this.pendingFiles.length === 0) return;

    this.uploadingImageCount = this.pendingFiles.length;

    const uploads = this.pendingFiles.map((pending) =>
      this.projectImageService.uploadImage(
        this.projectId!,
        pending.file,
        pending.altText || undefined
      )
    );

    forkJoin(uploads)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.uploadingImageCount = 0;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (responses) => {
          this.processUploadedImages(responses);
          this.pendingFiles = [];
          this.toastr.success(this.translate.instant('admin.projects.imagesUploadSuccess'));
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to upload images', { error });
          this.toastr.error(this.translate.instant('admin.projects.imagesUploadError'));
        },
      });
  }

  /**
   * Delete an existing project image
   */
  deleteProjectImage(imageId: number): void {
    if (this.demoModeService.isDemo()) return;
    if (!this.projectId) return;

    this.projectImageService
      .deleteImage(this.projectId, imageId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.projectImages = this.projectImages.filter((img) => img.id !== imageId);
          this.toastr.success(this.translate.instant('admin.projects.imageDeleteSuccess'));
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to delete image', { error });
          this.toastr.error(this.translate.instant('admin.projects.imageDeleteError'));
        },
      });
  }

  /**
   * Set an image as primary/thumbnail
   */
  setImageAsPrimary(imageId: number): void {
    if (this.demoModeService.isDemo()) return;
    if (!this.projectId) return;

    this.projectImageService
      .setPrimaryImage(this.projectId, imageId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.projectImages = this.projectImages.map((img) => ({
            ...img,
            primary: img.id === imageId,
          }));
          this.toastr.success(this.translate.instant('admin.projects.mainImageSuccess'));
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to set primary image', { error });
          this.toastr.error(this.translate.instant('admin.projects.mainImageError'));
        },
      });
  }

  /**
   * Move an image up in the display order
   */
  moveImageUp(index: number): void {
    if (this.demoModeService.isDemo()) return;
    if (index <= 0) return;

    [this.projectImages[index - 1], this.projectImages[index]] = [
      this.projectImages[index],
      this.projectImages[index - 1],
    ];
    this.saveImageOrder();
  }

  /**
   * Move an image down in the display order
   */
  moveImageDown(index: number): void {
    if (this.demoModeService.isDemo()) return;
    if (index >= this.projectImages.length - 1) return;

    [this.projectImages[index], this.projectImages[index + 1]] = [
      this.projectImages[index + 1],
      this.projectImages[index],
    ];
    this.saveImageOrder();
  }

  /**
   * Save the current image order to the backend
   */
  private saveImageOrder(): void {
    if (!this.projectId) return;

    const imageIds = this.projectImages.map((img) => img.id);
    this.projectImageService
      .reorderImages(this.projectId, { imageIds })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to reorder images', { error });
          this.toastr.error(this.translate.instant('admin.projects.reorderError'));
          this.loadProjectImages();
        },
      });
  }

  /**
   * Reload project images from the server
   */
  private loadProjectImages(): void {
    if (!this.projectId) return;

    this.projectImageService
      .getImages(this.projectId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (images) => {
          this.projectImages = [...images].sort((a, b) => a.displayOrder - b.displayOrder);
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to load project images', { error });
        },
      });
  }

  /**
   * Start polling for a processing image until it becomes READY or FAILED.
   */
  private startPollingForImage(imageId: number): void {
    if (!this.projectId || this.pollingSubscriptions.has(imageId)) {
      return;
    }

    this.logger.info('[POLL] Starting status polling', { projectId: this.projectId, imageId });

    const subscription = this.projectImageService
      .pollImageStatus(this.projectId, imageId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (status) => {
          this.logger.info('[POLL] Status resolved', { imageId, status });

          // Update the image in the list
          const imageIndex = this.projectImages.findIndex((img) => img.id === imageId);
          if (imageIndex !== -1) {
            this.projectImages[imageIndex] = {
              ...this.projectImages[imageIndex],
              status,
            };
            this.cdr.markForCheck();
          }

          // Clean up subscription
          this.pollingSubscriptions.delete(imageId);

          if (status === 'FAILED') {
            this.toastr.error(this.translate.instant('admin.projects.imageProcessingFailed'));
          }
        },
        error: (error) => {
          this.logger.error('[POLL_ERROR] Status polling failed', { imageId, error });
          this.pollingSubscriptions.delete(imageId);
        },
      });

    this.pollingSubscriptions.set(imageId, subscription);
  }

  /**
   * Process uploaded images and start polling for processing ones.
   */
  private processUploadedImages(responses: ProjectImageResponse[]): void {
    for (const image of responses) {
      this.projectImages.push(image);

      if (image.status === 'PROCESSING') {
        this.startPollingForImage(image.id);
      }
    }
    this.projectImages.sort((a, b) => a.displayOrder - b.displayOrder);
    this.cdr.markForCheck();
  }

  ngOnDestroy(): void {
    // Clean up all polling subscriptions
    this.pollingSubscriptions.forEach((sub) => sub.unsubscribe());
    this.pollingSubscriptions.clear();

    this.destroy$.next();
    this.destroy$.complete();
  }
}
