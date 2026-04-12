import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SkeletonTableRowComponent } from '../../../../components/shared/skeleton';
import { DemoDisabledDirective } from '../../../../directives/demo-disabled.directive';
import { AuditService } from '../../../../services/audit.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { AuditLogResponse, AuditLogFilter, Page } from '../../../../models/audit.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-admin-audit-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TranslateModule,
    SkeletonTableRowComponent,
    DemoDisabledDirective,
  ],
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

  // Demo mode: unfiltered reference data for client-side filtering
  private allDemoLogs: AuditLogResponse[] = [];

  ngOnInit(): void {
    if (this.demoModeService.isDemoRoute()) {
      this.loadDemoData();
      return;
    }
    this.loadFilterOptions();
    this.loadAuditLogs();
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
    if (this.demoModeService.isDemoRoute()) {
      this.applyDemoFilters();
      return;
    }
    this.loadAuditLogs();
  }

  clearFilters(): void {
    this.filter = {};
    this.currentPage = 0;
    if (this.demoModeService.isDemoRoute()) {
      this.applyDemoFilters();
      return;
    }
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

  // ========== Demo mode ==========

  private loadDemoData(): void {
    const now = Date.now();
    const day = 86_400_000;
    const hour = 3_600_000;

    this.actions = [
      'CREATE',
      'UPDATE',
      'DELETE',
      'PUBLISH',
      'UNPUBLISH',
      'FEATURE',
      'SET_CURRENT',
      'LOGIN',
      'LOGOUT',
      'LOGIN_FAILED',
      'PASSWORD_CHANGE',
      'APPROVE',
      'EXECUTE',
    ];

    this.entityTypes = [
      'PROJECT',
      'ARTICLE',
      'SKILL',
      'EXPERIENCE',
      'TAG',
      'CV',
      'SITE_CONFIGURATION',
      'USER',
      'HEALTH_PROPOSED_ACTION',
    ];

    this.allDemoLogs = [
      {
        id: 1042,
        action: 'LOGIN',
        entityType: 'USER',
        entityName: 'admin',
        username: 'admin',
        success: true,
        createdAt: new Date(now - 2 * hour).toISOString(),
      },
      {
        id: 1041,
        action: 'APPROVE',
        entityType: 'HEALTH_PROPOSED_ACTION',
        entityId: 7002,
        entityName: 'FLUSH_REDIS_CACHE',
        username: 'admin',
        success: true,
        requestMethod: 'POST',
        requestUri: '/api/admin/health/actions/7002/approve',
        createdAt: new Date(now - 5 * hour).toISOString(),
      },
      {
        id: 1040,
        action: 'EXECUTE',
        entityType: 'HEALTH_PROPOSED_ACTION',
        entityId: 7002,
        entityName: 'FLUSH_REDIS_CACHE',
        username: 'system',
        success: true,
        requestMethod: 'POST',
        requestUri: '/api/admin/health/actions/7002/execute',
        createdAt: new Date(now - 5 * hour - 3000).toISOString(),
      },
      {
        id: 1039,
        action: 'UPDATE',
        entityType: 'SITE_CONFIGURATION',
        entityId: 1,
        entityName: 'Configuration globale',
        username: 'admin',
        success: true,
        changedFields: ['maintenanceMode', 'contactEmail'],
        requestMethod: 'PUT',
        requestUri: '/api/admin/configuration/1',
        createdAt: new Date(now - 1 * day - 3 * hour).toISOString(),
      },
      {
        id: 1038,
        action: 'PUBLISH',
        entityType: 'ARTICLE',
        entityId: 12,
        entityName: 'Monitoring avec Prometheus et Grafana',
        username: 'admin',
        success: true,
        requestMethod: 'PUT',
        requestUri: '/api/admin/articles/12/publish',
        createdAt: new Date(now - 1 * day - 6 * hour).toISOString(),
      },
      {
        id: 1037,
        action: 'CREATE',
        entityType: 'ARTICLE',
        entityId: 12,
        entityName: 'Monitoring avec Prometheus et Grafana',
        username: 'admin',
        success: true,
        requestMethod: 'POST',
        requestUri: '/api/admin/articles',
        createdAt: new Date(now - 1 * day - 7 * hour).toISOString(),
      },
      {
        id: 1036,
        action: 'FEATURE',
        entityType: 'PROJECT',
        entityId: 3,
        entityName: 'API Gateway Microservices',
        username: 'admin',
        success: true,
        requestMethod: 'PUT',
        requestUri: '/api/admin/projects/3/feature',
        createdAt: new Date(now - 2 * day - 2 * hour).toISOString(),
      },
      {
        id: 1035,
        action: 'UPDATE',
        entityType: 'PROJECT',
        entityId: 3,
        entityName: 'API Gateway Microservices',
        username: 'admin',
        success: true,
        changedFields: ['description', 'technologies'],
        requestMethod: 'PUT',
        requestUri: '/api/admin/projects/3',
        createdAt: new Date(now - 2 * day - 3 * hour).toISOString(),
      },
      {
        id: 1034,
        action: 'DELETE',
        entityType: 'TAG',
        entityId: 28,
        entityName: 'deprecated-lib',
        username: 'admin',
        success: true,
        requestMethod: 'DELETE',
        requestUri: '/api/admin/tags/28',
        createdAt: new Date(now - 3 * day - 1 * hour).toISOString(),
      },
      {
        id: 1033,
        action: 'CREATE',
        entityType: 'SKILL',
        entityId: 15,
        entityName: 'Kubernetes',
        username: 'admin',
        success: true,
        requestMethod: 'POST',
        requestUri: '/api/admin/skills',
        createdAt: new Date(now - 3 * day - 4 * hour).toISOString(),
      },
      {
        id: 1032,
        action: 'LOGIN_FAILED',
        entityType: 'USER',
        entityName: 'admin',
        username: 'admin',
        success: false,
        errorMessage: 'Invalid credentials',
        ipAddress: '91.178.42.103',
        createdAt: new Date(now - 4 * day - 12 * hour).toISOString(),
      },
      {
        id: 1031,
        action: 'SET_CURRENT',
        entityType: 'CV',
        entityId: 5,
        entityName: 'CV-2026-fullstack.pdf',
        username: 'admin',
        success: true,
        requestMethod: 'PUT',
        requestUri: '/api/admin/cvs/5/set-current',
        createdAt: new Date(now - 5 * day - 2 * hour).toISOString(),
      },
      {
        id: 1030,
        action: 'PASSWORD_CHANGE',
        entityType: 'USER',
        entityId: 1,
        entityName: 'admin',
        username: 'admin',
        success: true,
        requestMethod: 'PUT',
        requestUri: '/api/admin/users/1/password',
        createdAt: new Date(now - 6 * day - 8 * hour).toISOString(),
      },
      {
        id: 1029,
        action: 'UPDATE',
        entityType: 'EXPERIENCE',
        entityId: 4,
        entityName: 'Lead Developer - TechCorp',
        username: 'admin',
        success: true,
        changedFields: ['endDate', 'description'],
        requestMethod: 'PUT',
        requestUri: '/api/admin/experiences/4',
        createdAt: new Date(now - 7 * day - 5 * hour).toISOString(),
      },
      {
        id: 1028,
        action: 'UNPUBLISH',
        entityType: 'ARTICLE',
        entityId: 8,
        entityName: 'Introduction a Docker Compose',
        username: 'admin',
        success: true,
        requestMethod: 'PUT',
        requestUri: '/api/admin/articles/8/unpublish',
        createdAt: new Date(now - 8 * day - 3 * hour).toISOString(),
      },
      {
        id: 1027,
        action: 'DELETE',
        entityType: 'PROJECT',
        entityId: 7,
        entityName: 'Legacy CRM Module',
        username: 'admin',
        success: false,
        errorMessage: 'Cannot delete: entity has active references',
        requestMethod: 'DELETE',
        requestUri: '/api/admin/projects/7',
        createdAt: new Date(now - 9 * day - 6 * hour).toISOString(),
      },
    ];

    this.auditLogs = [...this.allDemoLogs];
    this.totalElements = this.allDemoLogs.length;
    this.totalPages = 1;
  }

  private applyDemoFilters(): void {
    let filtered = [...this.allDemoLogs];

    if (this.filter.action) {
      filtered = filtered.filter((log) => log.action === this.filter.action);
    }
    if (this.filter.entityType) {
      filtered = filtered.filter((log) => log.entityType === this.filter.entityType);
    }
    if (this.filter.success !== undefined) {
      filtered = filtered.filter((log) => log.success === this.filter.success);
    }
    if (this.filter.startDate) {
      const start = new Date(this.filter.startDate).getTime();
      filtered = filtered.filter((log) => new Date(log.createdAt).getTime() >= start);
    }
    if (this.filter.endDate) {
      const end = new Date(this.filter.endDate).getTime() + 86_400_000;
      filtered = filtered.filter((log) => new Date(log.createdAt).getTime() < end);
    }

    this.auditLogs = filtered;
    this.totalElements = filtered.length;
    this.totalPages = 1;
  }
}
