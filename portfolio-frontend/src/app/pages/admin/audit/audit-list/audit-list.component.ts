import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SkeletonTableRowComponent } from '../../../../components/shared/skeleton';
import { AuditService } from '../../../../services/audit.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { AuditLogResponse, AuditLogFilter, Page } from '../../../../models/audit.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-admin-audit-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, SkeletonTableRowComponent],
  templateUrl: './audit-list.component.html',
  styleUrls: ['./audit-list.component.scss'],
})
export class AdminAuditListComponent implements OnInit {
  private readonly auditService = inject(AuditService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);
  readonly demoModeService = inject(DemoModeService);

  readonly Math = Math;

  auditLogs: AuditLogResponse[] = [];
  loading = false;
  error: string | undefined;

  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  // Filter options
  actions: string[] = [];
  entityTypes: string[] = [];

  // Active filters
  filter: AuditLogFilter = {};

  ngOnInit(): void {
    if (!this.demoModeService.isDemoRoute()) {
      this.loadFilterOptions();
      this.loadAuditLogs();
    }
  }

  loadFilterOptions(): void {
    this.auditService.getActions().subscribe({
      next: (actions) => (this.actions = actions),
      error: (error) => this.logger.error('[HTTP_ERROR] Failed to load actions', { error }),
    });

    this.auditService.getEntityTypes().subscribe({
      next: (types) => (this.entityTypes = types),
      error: (error) => this.logger.error('[HTTP_ERROR] Failed to load entity types', { error }),
    });
  }

  loadAuditLogs(): void {
    this.loading = true;
    this.error = undefined;

    this.logger.info('[HTTP_REQUEST] Loading audit logs', {
      page: this.currentPage,
      size: this.pageSize,
      filter: this.filter,
    });

    this.auditService.getAuditLogs(this.filter, this.currentPage, this.pageSize).subscribe({
      next: (page: Page<AuditLogResponse>) => {
        this.auditLogs = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
        this.logger.info('[HTTP_SUCCESS] Audit logs loaded', { count: page.content.length });
      },
      error: (error) => {
        this.error = this.translate.instant('admin.audit.loadError');
        this.loading = false;
        this.logger.error('[HTTP_ERROR] Failed to load audit logs', { error });
        this.toastr.error(this.translate.instant('admin.audit.loadError'));
      },
    });
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadAuditLogs();
  }

  clearFilters(): void {
    this.filter = {};
    this.currentPage = 0;
    this.loadAuditLogs();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadAuditLogs();
    }
  }

  exportCsv(): void {
    this.auditService.exportCsv(this.filter);
    this.toastr.info(this.translate.instant('admin.audit.exporting'));
  }

  exportJson(): void {
    this.auditService.exportJson(this.filter);
    this.toastr.info(this.translate.instant('admin.audit.exporting'));
  }

  getActionBadgeClass(action: string): string {
    switch (action) {
      case 'CREATE':
        return 'bg-success';
      case 'UPDATE':
        return 'bg-primary';
      case 'DELETE':
        return 'bg-danger';
      case 'PUBLISH':
        return 'bg-info';
      case 'UNPUBLISH':
        return 'bg-warning text-dark';
      case 'SET_CURRENT':
        return 'bg-secondary';
      case 'LOGIN':
        return 'bg-success';
      case 'LOGOUT':
        return 'bg-secondary';
      case 'PASSWORD_CHANGE':
        return 'bg-warning text-dark';
      default:
        return 'bg-secondary';
    }
  }

  getStatusBadgeClass(success: boolean): string {
    return success ? 'bg-success' : 'bg-danger';
  }

  formatDate(dateString: string): string {
    if (!dateString) {
      return '-';
    }
    return new Date(dateString).toLocaleString('fr-FR', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(0, this.currentPage - Math.floor(maxVisible / 2));
    const end = Math.min(this.totalPages, start + maxVisible);

    if (end - start < maxVisible) {
      start = Math.max(0, end - maxVisible);
    }

    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    return pages;
  }
}
