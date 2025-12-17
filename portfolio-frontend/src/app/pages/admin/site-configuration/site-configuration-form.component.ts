import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SiteConfigurationService } from '../../../services/site-configuration.service';
import { LoggerService } from '../../../services/logger.service';
import { DemoModeService } from '../../../services/demo-mode.service';
import { ToastrService } from 'ngx-toastr';
import { UpdateSiteConfigurationRequest } from '../../../models/site-configuration.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-site-configuration-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslateModule],
  templateUrl: './site-configuration-form.component.html',
  styleUrls: ['./site-configuration-form.component.css'],
})
export class SiteConfigurationFormComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly siteConfigService = inject(SiteConfigurationService);
  private readonly location = inject(Location);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  readonly demoModeService = inject(DemoModeService);
  private readonly translate = inject(TranslateService);
  private readonly destroy$ = new Subject<void>();

  configForm!: FormGroup;
  loading = false;
  submitting = false;
  uploadingImage = false;

  // Profile image
  profileImageUrl: string | undefined;
  selectedFile: File | undefined;
  imagePreview: string | undefined;

  ngOnInit(): void {
    this.initForm();
    this.loadConfiguration();
  }

  initForm(): void {
    this.configForm = this.fb.group({
      // Identity
      fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],

      // Hero Section
      heroTitle: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(200)]],
      heroDescription: ['', [Validators.required]],

      // SEO
      siteTitle: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      seoDescription: ['', [Validators.required, Validators.maxLength(300)]],

      // Social Links
      githubUrl: ['', [Validators.required, Validators.pattern(/^https?:\/\/.*/)]],
      linkedinUrl: ['', [Validators.required, Validators.pattern(/^https?:\/\/.*/)]],
    });
  }

  loadConfiguration(): void {
    this.loading = true;
    this.logger.info('[SITE_CONFIG_FORM] Loading site configuration');

    this.siteConfigService
      .getSiteConfiguration()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (config) => {
          this.logger.info('[SITE_CONFIG_FORM] Configuration loaded', {
            fullName: config.fullName,
          });

          this.configForm.patchValue({
            fullName: config.fullName,
            email: config.email,
            heroTitle: config.heroTitle,
            heroDescription: config.heroDescription,
            siteTitle: config.siteTitle,
            seoDescription: config.seoDescription,
            githubUrl: config.githubUrl,
            linkedinUrl: config.linkedinUrl,
          });

          this.profileImageUrl = config.profileImageUrl
            ? `${environment.apiUrl}${config.profileImageUrl}`
            : undefined;

          this.loading = false;
        },
        error: (error) => {
          this.logger.error('[SITE_CONFIG_FORM] Failed to load configuration', { error });
          this.toastr.error(this.translate.instant('admin.siteConfig.loadError'));
          this.loading = false;
        },
      });
  }

  onSubmit(): void {
    if (this.demoModeService.isDemo()) {
      return;
    }

    if (this.configForm.invalid) {
      this.configForm.markAllAsTouched();
      this.toastr.warning(this.translate.instant('admin.common.fillRequired'));
      return;
    }

    this.updateConfiguration();
  }

  updateConfiguration(): void {
    this.submitting = true;
    const formValue = this.configForm.value;

    const request: UpdateSiteConfigurationRequest = {
      fullName: formValue.fullName,
      email: formValue.email,
      heroTitle: formValue.heroTitle,
      heroDescription: formValue.heroDescription,
      siteTitle: formValue.siteTitle,
      seoDescription: formValue.seoDescription,
      githubUrl: formValue.githubUrl,
      linkedinUrl: formValue.linkedinUrl,
    };

    this.logger.info('[SITE_CONFIG_FORM] Updating configuration', { fullName: request.fullName });

    this.siteConfigService
      .updateSiteConfiguration(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (config) => {
          this.logger.info('[SITE_CONFIG_FORM] Configuration updated', {
            fullName: config.fullName,
          });
          this.toastr.success(this.translate.instant('admin.siteConfig.updateSuccess'));
          this.submitting = false;
        },
        error: (error) => {
          this.logger.error('[SITE_CONFIG_FORM] Failed to update configuration', { error });
          this.toastr.error(this.translate.instant('admin.siteConfig.updateError'));
          this.submitting = false;
        },
      });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.selectedFile = input.files[0];

      // Create preview
      const reader = new FileReader();
      reader.onload = () => {
        this.imagePreview = reader.result as string;
      };
      reader.readAsDataURL(this.selectedFile);
    }
  }

  uploadProfileImage(): void {
    if (this.demoModeService.isDemo() || !this.selectedFile) {
      return;
    }

    this.uploadingImage = true;
    this.logger.info('[SITE_CONFIG_FORM] Uploading profile image', {
      fileName: this.selectedFile.name,
    });

    this.siteConfigService
      .uploadProfileImage(this.selectedFile)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (config) => {
          this.logger.info('[SITE_CONFIG_FORM] Profile image uploaded', {
            url: config.profileImageUrl,
          });
          this.profileImageUrl = config.profileImageUrl
            ? `${environment.apiUrl}${config.profileImageUrl}`
            : undefined;
          this.selectedFile = undefined;
          this.imagePreview = undefined;
          this.toastr.success(this.translate.instant('admin.siteConfig.imageUploadSuccess'));
          this.uploadingImage = false;
        },
        error: (error) => {
          this.logger.error('[SITE_CONFIG_FORM] Failed to upload profile image', { error });
          this.toastr.error(this.translate.instant('admin.siteConfig.imageUploadError'));
          this.uploadingImage = false;
        },
      });
  }

  deleteProfileImage(): void {
    if (this.demoModeService.isDemo()) {
      return;
    }

    this.uploadingImage = true;
    this.logger.info('[SITE_CONFIG_FORM] Deleting profile image');

    this.siteConfigService
      .deleteProfileImage()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.logger.info('[SITE_CONFIG_FORM] Profile image deleted');
          this.profileImageUrl = undefined;
          this.toastr.success(this.translate.instant('admin.siteConfig.imageDeleteSuccess'));
          this.uploadingImage = false;
        },
        error: (error) => {
          this.logger.error('[SITE_CONFIG_FORM] Failed to delete profile image', { error });
          this.toastr.error(this.translate.instant('admin.siteConfig.imageDeleteError'));
          this.uploadingImage = false;
        },
      });
  }

  cancelImageSelection(): void {
    this.selectedFile = undefined;
    this.imagePreview = undefined;
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.configForm.get(fieldName);
    return !!(field?.invalid && (field?.touched || field?.dirty));
  }

  hasError(fieldName: string, errorType: string): boolean {
    const field = this.configForm.get(fieldName);
    return !!(field?.hasError(errorType) && (field?.touched || field?.dirty));
  }

  goBack(): void {
    this.location.back();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
