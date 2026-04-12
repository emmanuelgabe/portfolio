import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ToastrService } from 'ngx-toastr';
import { Subject, forkJoin, interval, takeUntil } from 'rxjs';
import { DemoModeService } from '../../../services/demo-mode.service';
import { HealthMonitorService } from '../../../services/health-monitor.service';
import { LoggerService } from '../../../services/logger.service';
import {
  ActionLevel,
  ActionStatus,
  HealthReport,
  ProposedAction,
  ReportSeverity,
} from '../../../models/health-monitor.model';
import {
  ActionApprovalModalComponent,
  ActionDecisionMode,
} from './action-approval-modal/action-approval-modal.component';

/**
 * Admin dashboard for the AI Health Monitor agent. Streams diagnostic
 * reports and proposed actions in real time via SSE, and lets the admin
 * approve or reject mutations through a confirmation modal. Demo mode
 * renders realistic mock data so non-authenticated visitors can explore
 * the showcase without hitting the backend.
 */
@Component({
  selector: 'app-health-monitor',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslateModule],
  templateUrl: './health-monitor.component.html',
  styleUrls: ['./health-monitor.component.scss'],
})
export class HealthMonitorComponent implements OnInit, OnDestroy {
  private readonly healthService = inject(HealthMonitorService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);
  private readonly modalService = inject(NgbModal);
  readonly demoModeService = inject(DemoModeService);

  private readonly destroy$ = new Subject<void>();

  reports: HealthReport[] = [];
  pendingActions: ProposedAction[] = [];
  history: ProposedAction[] = [];

  loading = false;
  historyLoading = false;
  analyzing = false;
  connected = false;
  expandedReportIds = new Set<number>();

  // History filters (client-side)
  statusFilter: ActionStatus | undefined;
  levelFilter: ActionLevel | undefined;
  readonly statusOptions: ActionStatus[] = [
    'PENDING',
    'APPROVED',
    'REJECTED',
    'EXECUTED',
    'FAILED',
    'EXPIRED',
  ];
  readonly levelOptions: ActionLevel[] = ['GREEN', 'ORANGE', 'RED'];

  // Now tick -- drives the "expires in X min" countdowns
  now = Date.now();

  ngOnInit(): void {
    if (this.demoModeService.isDemoRoute()) {
      this.loadDemoData();
      this.startClockTick();
      return;
    }

    this.loadInitialData();
    this.subscribeToStreams();
    this.healthService.connect();
    this.startClockTick();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.healthService.disconnect();
  }

  // ========== Data loading ==========

  private loadInitialData(): void {
    this.loading = true;
    this.historyLoading = true;

    forkJoin({
      reports: this.healthService.loadRecentReports(20),
      pending: this.healthService.loadPendingActions(),
      history: this.healthService.loadActionHistory(100),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ reports, pending, history }) => {
          this.healthService.setReports(reports);
          this.healthService.setPendingActions(pending);
          this.history = history;
          this.loading = false;
          this.historyLoading = false;
        },
        error: (error) => {
          this.loading = false;
          this.historyLoading = false;
          this.logger.error('[HEALTH_MONITOR] Initial load failed', {
            status: error.status,
            message: error.message,
          });
          this.toastr.error(this.translate.instant('admin.healthMonitor.toasts.loadError'));
        },
      });
  }

  private subscribeToStreams(): void {
    this.healthService.reports$.pipe(takeUntil(this.destroy$)).subscribe((reports) => {
      this.reports = reports;
    });

    this.healthService.pendingActions$.pipe(takeUntil(this.destroy$)).subscribe((pending) => {
      const previousIds = new Set(this.pendingActions.map((a) => a.id));
      this.pendingActions = pending;
      const hasNew = pending.some((a) => !previousIds.has(a.id));
      if (hasNew && this.pendingActions.length > 0) {
        this.toastr.info(this.translate.instant('admin.healthMonitor.toasts.newPendingAction'));
      }
    });

    this.healthService.connected$.pipe(takeUntil(this.destroy$)).subscribe((connected) => {
      this.connected = connected;
    });
  }

  private startClockTick(): void {
    // 30s is enough for "expires in X min" precision while keeping the UI
    // quiet. Runs in both admin and demo mode.
    interval(30_000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => (this.now = Date.now()));
  }

  // ========== Analyze now ==========

  analyzeNow(): void {
    if (this.analyzing) {
      return;
    }
    this.analyzing = true;

    if (this.demoModeService.isDemoRoute()) {
      this.simulateDemoAnalysis();
      return;
    }

    this.healthService
      .triggerAnalysis()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (report) => {
          this.analyzing = false;
          if (!report) {
            this.toastr.info(this.translate.instant('admin.healthMonitor.toasts.analyzeNoAnomaly'));
            return;
          }
          this.healthService.prependReport(report);
          this.toastr.success(this.translate.instant('admin.healthMonitor.toasts.analyzeSuccess'));
        },
        error: () => {
          this.analyzing = false;
          this.toastr.error(this.translate.instant('admin.healthMonitor.toasts.analyzeFailed'));
        },
      });
  }

  // ========== Approve / reject ==========

  openApproveModal(action: ProposedAction): void {
    this.openDecisionModal(action, 'approve');
  }

  openRejectModal(action: ProposedAction): void {
    this.openDecisionModal(action, 'reject');
  }

  private openDecisionModal(action: ProposedAction, mode: ActionDecisionMode): void {
    const modalRef = this.modalService.open(ActionApprovalModalComponent, {
      centered: true,
      backdrop: 'static',
      size: 'lg',
    });
    const instance = modalRef.componentInstance as ActionApprovalModalComponent;
    instance.action = action;
    instance.mode = mode;

    modalRef.result
      .then((result: { reason: string | undefined } | undefined) => {
        if (!result) {
          return;
        }
        if (this.demoModeService.isDemoRoute()) {
          this.applyDemoDecision(action, mode, result.reason);
          return;
        }
        this.submitDecision(action, mode, result.reason);
      })
      .catch(() => {
        // Modal dismissed
      });
  }

  private submitDecision(
    action: ProposedAction,
    mode: ActionDecisionMode,
    reason: string | undefined
  ): void {
    const request$ =
      mode === 'approve'
        ? this.healthService.approveAction(action.id, { reason })
        : this.healthService.rejectAction(action.id, { reason });

    request$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (updated) => {
        this.healthService.upsertAction(updated);
        this.history = [updated, ...this.history.filter((a) => a.id !== updated.id)];
        const successKey =
          mode === 'approve'
            ? 'admin.healthMonitor.toasts.approveSuccess'
            : 'admin.healthMonitor.toasts.rejectSuccess';
        this.toastr.success(this.translate.instant(successKey));
      },
      error: () => {
        this.toastr.error(this.translate.instant('admin.healthMonitor.toasts.decisionFailed'));
      },
    });
  }

  // ========== History filters ==========

  get filteredHistory(): ProposedAction[] {
    return this.history.filter((a) => {
      if (this.statusFilter && a.status !== this.statusFilter) {
        return false;
      }
      if (this.levelFilter && a.level !== this.levelFilter) {
        return false;
      }
      return true;
    });
  }

  clearHistoryFilters(): void {
    this.statusFilter = undefined;
    this.levelFilter = undefined;
  }

  // ========== Expand / collapse reports ==========

  toggleReport(id: number): void {
    if (this.expandedReportIds.has(id)) {
      this.expandedReportIds.delete(id);
    } else {
      this.expandedReportIds.add(id);
    }
  }

  isExpanded(id: number): boolean {
    return this.expandedReportIds.has(id);
  }

  // ========== Stats ==========

  get currentSeverity(): ReportSeverity {
    const mostRecent = this.reports[0];
    return mostRecent?.severity ?? 'UNKNOWN';
  }

  get pendingCount(): number {
    return this.pendingActions.length;
  }

  get recentAnomaliesCount(): number {
    return this.reports.slice(0, 5).reduce((sum, r) => sum + (r.anomalies?.length ?? 0), 0);
  }

  get lastCycleAt(): string | undefined {
    return this.reports[0]?.createdAt;
  }

  // ========== Badges / styling ==========

  severityBadgeClass(severity: ReportSeverity): string {
    switch (severity) {
      case 'OK':
        return 'bg-success';
      case 'WARNING':
        return 'bg-warning text-dark';
      case 'CRITICAL':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  }

  severityBorderClass(severity: ReportSeverity): string {
    switch (severity) {
      case 'OK':
        return 'border-success';
      case 'WARNING':
        return 'border-warning';
      case 'CRITICAL':
        return 'border-danger';
      default:
        return 'border-secondary';
    }
  }

  levelBadgeClass(level: ActionLevel): string {
    switch (level) {
      case 'RED':
        return 'bg-danger';
      case 'ORANGE':
        return 'bg-warning text-dark';
      case 'GREEN':
        return 'bg-success';
      default:
        return 'bg-secondary';
    }
  }

  levelBorderClass(level: ActionLevel): string {
    switch (level) {
      case 'RED':
        return 'border-danger border-3';
      case 'ORANGE':
        return 'border-warning border-2';
      default:
        return 'border-secondary';
    }
  }

  statusBadgeClass(status: ActionStatus): string {
    switch (status) {
      case 'EXECUTED':
        return 'bg-success';
      case 'APPROVED':
        return 'bg-info';
      case 'PENDING':
        return 'bg-warning text-dark';
      case 'REJECTED':
        return 'bg-secondary';
      case 'FAILED':
        return 'bg-danger';
      case 'EXPIRED':
        return 'bg-dark';
      default:
        return 'bg-secondary';
    }
  }

  cycleIcon(cycleType: string): string {
    switch (cycleType) {
      case 'ON_DEMAND':
        return 'bi-lightning-charge';
      case 'PRECHECK_ONLY':
        return 'bi-shield-check';
      default:
        return 'bi-clock-history';
    }
  }

  // ========== Expiration countdown ==========

  /**
   * Minutes remaining until the action expires, or 0 when already expired.
   * Uses the ticking {@code now} value to stay live without zone hacks.
   */
  minutesUntilExpiry(expiresAt: string): number {
    const target = new Date(expiresAt).getTime();
    if (Number.isNaN(target)) {
      return 0;
    }
    const diffMs = target - this.now;
    return Math.max(0, Math.floor(diffMs / 60_000));
  }

  isExpired(expiresAt: string): boolean {
    const target = new Date(expiresAt).getTime();
    return Number.isNaN(target) ? false : target <= this.now;
  }

  // ========== Formatting ==========

  formatDate(dateString: string | undefined): string {
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

  formatParams(params: Record<string, unknown>): string {
    const entries = Object.entries(params ?? {});
    if (entries.length === 0) {
      return '';
    }
    return entries
      .map(([k, v]) => `${k}=${typeof v === 'string' ? v : JSON.stringify(v)}`)
      .join(', ');
  }

  // ========== Demo mode ==========

  private loadDemoData(): void {
    const now = Date.now();

    const reports: HealthReport[] = [
      {
        id: 4001,
        cycleType: 'SCHEDULED',
        severity: 'OK',
        summary: 'Tous les services en bon état. Aucune anomalie critique détectée.',
        anomalies: [],
        toolCalls: [
          'checkServiceHealth',
          'getServiceMetrics',
          'checkCircuitBreakers',
          'checkDeadLetterQueue',
        ],
        durationMs: 1820,
        createdAt: new Date(now - 2 * 60_000).toISOString(),
      },
      {
        id: 4000,
        cycleType: 'SCHEDULED',
        severity: 'WARNING',
        summary:
          'Consommation memoire elevee sur le backend (86%). Redis cache sature a 92%, impact probable sur le temps de reponse.',
        anomalies: [
          'portfolio-backend: RAM usage 86% (threshold 80%)',
          'Redis cache hit ratio down to 41%, memory pressure detected',
        ],
        toolCalls: [
          'checkServiceHealth',
          'getServiceMetrics',
          'getServiceLogs',
          'checkCircuitBreakers',
        ],
        durationMs: 2940,
        createdAt: new Date(now - 7 * 60_000).toISOString(),
      },
      {
        id: 3999,
        cycleType: 'ON_DEMAND',
        severity: 'CRITICAL',
        summary:
          'Pic memoire sur portfolio-backend a 97%. Aucun leak identifie, redemarrage controle recommande avant OOMKiller.',
        anomalies: [
          'portfolio-backend: RAM usage 97% (threshold critical at 95%)',
          'GC pause time increased by 340% in the last 10 minutes',
        ],
        toolCalls: ['checkServiceHealth', 'getServiceMetrics', 'getServiceLogs'],
        durationMs: 3420,
        createdAt: new Date(now - 14 * 60_000).toISOString(),
      },
      {
        id: 3998,
        cycleType: 'PRECHECK_ONLY',
        severity: 'OK',
        summary: "Pre-verification propre, pas d'appel LLM.",
        anomalies: [],
        toolCalls: [],
        durationMs: 42,
        createdAt: new Date(now - 20 * 60_000).toISOString(),
      },
    ];

    const pending: ProposedAction[] = [
      {
        id: 7002,
        reportId: 4000,
        level: 'ORANGE',
        actionType: 'FLUSH_REDIS_CACHE',
        params: { pattern: 'session:*' },
        justification:
          'Le cache Redis affiche un hit ratio de 41% et sature en memoire. Un flush cible sur les cles de session devrait liberer 60% de la pression memoire sans impacter les donnees metier.',
        status: 'PENDING',
        approvedBy: undefined,
        approvedAt: undefined,
        executedAt: undefined,
        rejectionReason: undefined,
        resultMessage: undefined,
        expiresAt: new Date(now + 47 * 60_000).toISOString(),
        createdAt: new Date(now - 7 * 60_000).toISOString(),
      },
      {
        id: 7001,
        reportId: 3999,
        level: 'RED',
        actionType: 'RESTART_SERVICE',
        params: { serviceName: 'portfolio-backend' },
        justification:
          'Memoire a 97%, GC pause x3.4, aucun leak identifie dans les heap dumps recents. Un redemarrage controle est prefere a une mort par OOMKiller qui laisserait des requetes en cours interrompues.',
        status: 'PENDING',
        approvedBy: undefined,
        approvedAt: undefined,
        executedAt: undefined,
        rejectionReason: undefined,
        resultMessage: undefined,
        expiresAt: new Date(now + 23 * 60_000).toISOString(),
        createdAt: new Date(now - 14 * 60_000).toISOString(),
      },
    ];

    const history: ProposedAction[] = [
      {
        id: 6999,
        reportId: 3990,
        level: 'ORANGE',
        actionType: 'FLUSH_REDIS_CACHE',
        params: { pattern: '*' },
        justification: 'Cache sature apres deploiement, flush global recommande.',
        status: 'EXECUTED',
        approvedBy: 'admin',
        approvedAt: new Date(now - 45 * 60_000).toISOString(),
        executedAt: new Date(now - 45 * 60_000).toISOString(),
        rejectionReason: undefined,
        resultMessage: 'Cache flushed successfully, 1842 keys removed',
        expiresAt: new Date(now - 5 * 60_000).toISOString(),
        createdAt: new Date(now - 50 * 60_000).toISOString(),
      },
      {
        id: 6998,
        reportId: 3989,
        level: 'RED',
        actionType: 'RESTART_SERVICE',
        params: { serviceName: 'rabbitmq' },
        justification: 'DLQ inondee, suspicion de consommateur bloque.',
        status: 'REJECTED',
        approvedBy: 'admin',
        approvedAt: new Date(now - 90 * 60_000).toISOString(),
        executedAt: undefined,
        rejectionReason: 'Impact trop large, preferer une relance ciblee des messages.',
        resultMessage: undefined,
        expiresAt: new Date(now - 30 * 60_000).toISOString(),
        createdAt: new Date(now - 95 * 60_000).toISOString(),
      },
      {
        id: 6997,
        reportId: 3989,
        level: 'ORANGE',
        actionType: 'RETRY_DLQ_MESSAGES',
        params: { count: 50 },
        justification: 'Rejouer les messages DLQ apres correction du consommateur.',
        status: 'EXECUTED',
        approvedBy: 'admin',
        approvedAt: new Date(now - 85 * 60_000).toISOString(),
        executedAt: new Date(now - 85 * 60_000).toISOString(),
        rejectionReason: undefined,
        resultMessage: '50 messages replayed, 48 successful, 2 moved to dead queue',
        expiresAt: new Date(now - 25 * 60_000).toISOString(),
        createdAt: new Date(now - 90 * 60_000).toISOString(),
      },
      {
        id: 6996,
        reportId: 3980,
        level: 'ORANGE',
        actionType: 'REINDEX_ELASTICSEARCH',
        params: {},
        justification: "Desynchronisation detectee entre la base et l'index de recherche.",
        status: 'EXECUTED',
        approvedBy: 'admin',
        approvedAt: new Date(now - 4 * 60 * 60_000).toISOString(),
        executedAt: new Date(now - 4 * 60 * 60_000).toISOString(),
        rejectionReason: undefined,
        resultMessage: 'Reindexation completed in 42s, 1204 documents',
        expiresAt: new Date(now - 3.5 * 60 * 60_000).toISOString(),
        createdAt: new Date(now - 4.1 * 60 * 60_000).toISOString(),
      },
      {
        id: 6995,
        reportId: 3960,
        level: 'ORANGE',
        actionType: 'TRIGGER_IMAGE_REPROCESSING',
        params: {},
        justification: 'Images eligibles pour recompression apres mise a jour des parametres.',
        status: 'EXPIRED',
        approvedBy: undefined,
        approvedAt: undefined,
        executedAt: undefined,
        rejectionReason: undefined,
        resultMessage: undefined,
        expiresAt: new Date(now - 12 * 60 * 60_000).toISOString(),
        createdAt: new Date(now - 13 * 60 * 60_000).toISOString(),
      },
    ];

    this.reports = reports;
    this.pendingActions = pending;
    this.history = history;
    this.connected = true;
  }

  private simulateDemoAnalysis(): void {
    setTimeout(() => {
      const now = Date.now();
      const simulated: HealthReport = {
        id: Math.floor(Math.random() * 100000) + 5000,
        cycleType: 'ON_DEMAND',
        severity: 'OK',
        summary:
          'Analyse simulee : aucun probleme critique detecte. Tous les services repondent sous 200ms.',
        anomalies: [],
        toolCalls: [
          'checkServiceHealth',
          'getServiceMetrics',
          'checkCircuitBreakers',
          'checkDeadLetterQueue',
          'getAuditStats',
        ],
        durationMs: 1450 + Math.floor(Math.random() * 800),
        createdAt: new Date(now).toISOString(),
      };
      this.reports = [simulated, ...this.reports];
      this.analyzing = false;
      this.toastr.info(this.translate.instant('admin.healthMonitor.demo.analyzeSimulated'));
    }, 1500);
  }

  private applyDemoDecision(
    action: ProposedAction,
    mode: ActionDecisionMode,
    reason: string | undefined
  ): void {
    this.toastr.info(this.translate.instant('admin.healthMonitor.demo.actionBlocked'));
    // Local-only update so the demo still reacts visually to the click.
    const now = new Date().toISOString();
    const updated: ProposedAction = {
      ...action,
      status: mode === 'approve' ? 'EXECUTED' : 'REJECTED',
      approvedBy: 'demo',
      approvedAt: now,
      executedAt: mode === 'approve' ? now : undefined,
      rejectionReason: mode === 'reject' ? reason : undefined,
      resultMessage: mode === 'approve' ? 'Demo mode: action not actually executed' : undefined,
    };
    this.pendingActions = this.pendingActions.filter((a) => a.id !== action.id);
    this.history = [updated, ...this.history.filter((a) => a.id !== action.id)];
  }
}
