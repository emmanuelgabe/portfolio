import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { CvService } from '../../../services/cv.service';
import { DemoModeService } from '../../../services/demo-mode.service';
import { CvResponse } from '../../../models/cv.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-admin-cv',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './admin-cv.component.html',
  styleUrls: ['./admin-cv.component.css'],
})
export class AdminCvComponent implements OnInit, OnDestroy {
  private cvService = inject(CvService);
  readonly demoModeService = inject(DemoModeService);
  private readonly translate = inject(TranslateService);
  private readonly destroy$ = new Subject<void>();

  cvs: CvResponse[] = [];
  isLoading = false;
  error?: string;
  selectedFile?: File;
  uploadProgress = 0;
  isUploading = false;

  ngOnInit(): void {
    this.loadCvs();
  }

  loadCvs(): void {
    this.isLoading = true;
    this.error = undefined;

    this.cvService
      .getAllCvs()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (cvs) => {
          this.cvs = cvs;
          this.isLoading = false;
        },
        error: (err) => {
          // If no CVs found (404), it's not an error, just empty list
          if (err.status === 404) {
            this.cvs = [];
          } else {
            this.error = this.translate.instant('admin.cv.loadError');
          }
          this.isLoading = false;
        },
      });
  }

  onFileSelected(event: Event): void {
    if (this.demoModeService.isDemo()) {
      return;
    }
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
    }
  }

  uploadCv(): void {
    if (this.demoModeService.isDemo()) {
      return;
    }
    if (!this.selectedFile) return;

    this.isUploading = true;
    this.error = undefined;

    this.cvService
      .uploadCv(this.selectedFile)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (_cv) => {
          this.isUploading = false;
          this.selectedFile = undefined;
          this.loadCvs();
          // Reset input
          const input = document.querySelector('input[type="file"]') as HTMLInputElement;
          if (input) input.value = '';
        },
        error: (_err) => {
          this.error = this.translate.instant('admin.cv.uploadError');
          this.isUploading = false;
        },
      });
  }

  setCurrentCv(cvId: number): void {
    if (this.demoModeService.isDemo()) {
      return;
    }
    this.cvService
      .setCurrentCv(cvId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.loadCvs(),
        error: (_err) => (this.error = this.translate.instant('admin.cv.setCurrentError')),
      });
  }

  deleteCv(cvId: number): void {
    if (this.demoModeService.isDemo()) {
      return;
    }
    if (!confirm(this.translate.instant('admin.cv.deleteConfirm'))) return;

    this.cvService
      .deleteCv(cvId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.loadCvs(),
        error: (_err) => (this.error = this.translate.instant('admin.cv.deleteError')),
      });
  }

  downloadCv(): void {
    this.cvService.downloadCv();
  }

  formatFileSize(bytes: number): string {
    return (bytes / 1024 / 1024).toFixed(2) + ' MB';
  }

  getFullFileUrl(fileUrl: string): string {
    return `${environment.apiUrl}${fileUrl}`;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
