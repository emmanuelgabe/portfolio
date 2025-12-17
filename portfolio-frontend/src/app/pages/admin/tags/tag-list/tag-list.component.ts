import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SkeletonTableRowComponent } from '../../../../components/shared/skeleton';
import { TagService } from '../../../../services/tag.service';
import { ModalService } from '../../../../services/modal.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { TagResponse } from '../../../../models/tag.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-tag-list',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule, SkeletonTableRowComponent],
  templateUrl: './tag-list.component.html',
  styleUrl: './tag-list.component.scss',
})
export class TagListComponent implements OnInit, OnDestroy {
  private readonly tagService = inject(TagService);
  private readonly modalService = inject(ModalService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);
  readonly demoModeService = inject(DemoModeService);
  private readonly destroy$ = new Subject<void>();

  tags: TagResponse[] = [];
  loading = true;
  error?: string;

  ngOnInit(): void {
    this.loadTags();
  }

  private loadTags(): void {
    this.loading = true;
    this.error = undefined;

    this.tagService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (tags) => {
          this.tags = tags;
          this.loading = false;
        },
        error: (err) => {
          this.error = this.translate.instant('admin.tags.loadError');
          this.loading = false;
          this.logger.error('[HTTP_ERROR] Failed to load tags', {
            error: err.message || err,
          });
        },
      });
  }

  confirmDelete(tag: TagResponse): void {
    this.modalService
      .confirm({
        title: this.translate.instant('admin.common.confirmDelete'),
        message: this.translate.instant('admin.tags.deleteConfirm', { name: tag.name }),
        confirmText: this.translate.instant('admin.common.delete'),
        cancelText: this.translate.instant('admin.common.cancel'),
        confirmButtonClass: 'btn-danger',
        disableConfirm: this.demoModeService.isDemo(),
      })
      .subscribe((confirmed) => {
        if (confirmed) {
          if (this.demoModeService.isDemo()) {
            this.toastr.info(this.translate.instant('admin.common.demoModeDisabled'));
            return;
          }
          this.deleteTag(tag.id);
        }
      });
  }

  private deleteTag(id: number): void {
    this.tagService
      .delete(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.tags = this.tags.filter((tag) => tag.id !== id);
        },
        error: (err) => {
          this.logger.error('[HTTP_ERROR] Failed to delete tag', {
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
