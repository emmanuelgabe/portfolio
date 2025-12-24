import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, of, Subject } from 'rxjs';
import { catchError, takeUntil } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ProjectService } from '../../../services/project.service';
import { SkillService } from '../../../services/skill.service';
import { CvService } from '../../../services/cv.service';
import { TagService } from '../../../services/tag.service';
import { ExperienceService } from '../../../services/experience.service';
import { ArticleService } from '../../../services/article.service';
import { LoggerService } from '../../../services/logger.service';
import { DemoModeService } from '../../../services/demo-mode.service';
import { BatchService, ImageReprocessingStats, LastJobInfo } from '../../../services/batch.service';
import { ActiveUsersService, DailyVisitorData } from '../../../services/active-users.service';
import {
  CircuitBreakerService,
  CircuitBreakerStatus,
} from '../../../services/circuit-breaker.service';
import { VisitorsChartComponent } from '../../../components/shared/visitors-chart/visitors-chart.component';
import { environment } from '../../../../environments/environment';

interface DashboardStat {
  titleKey: string;
  count: number;
  icon: string;
  color: string;
  link: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, VisitorsChartComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly projectService = inject(ProjectService);
  private readonly skillService = inject(SkillService);
  private readonly cvService = inject(CvService);
  private readonly tagService = inject(TagService);
  private readonly experienceService = inject(ExperienceService);
  private readonly articleService = inject(ArticleService);
  private readonly logger = inject(LoggerService);
  private readonly translate = inject(TranslateService);
  readonly demoModeService = inject(DemoModeService);
  private readonly batchService = inject(BatchService);
  private readonly activeUsersService = inject(ActiveUsersService);
  private readonly circuitBreakerService = inject(CircuitBreakerService);
  private readonly destroy$ = new Subject<void>();

  stats: DashboardStat[] = [];
  loading = false;

  // Batch reprocessing state
  reprocessingStats: ImageReprocessingStats | null = null;
  lastJobInfo: LastJobInfo | null = null;
  batchLoading = false;
  isJobRunning = false;
  showConfirmModal = false;
  jobResult: { success: boolean; message: string } | null = null;

  // Active users state
  activeUsersCount = 0;
  lastMonthVisitorsCount = 0;
  activeUsersConnected = false;

  // Circuit breaker state
  circuitBreakers: CircuitBreakerStatus[] = [];
  circuitBreakerLoading = false;

  // Visitors chart state
  dailyVisitorData: DailyVisitorData[] = [];
  showVisitorsChart = false;

  // Visitor tracking feature flag
  readonly visitorTrackingEnabled = environment.visitorTracking.enabled;

  ngOnInit(): void {
    const prefix = this.demoModeService.isDemo() ? '/admindemo' : '/admin';

    this.stats = [
      {
        titleKey: 'admin.dashboard.projectsCount',
        count: 0,
        icon: 'bi-folder',
        color: 'primary',
        link: `${prefix}/projects`,
      },
      {
        titleKey: 'admin.dashboard.skillsCount',
        count: 0,
        icon: 'bi-tools',
        color: 'success',
        link: `${prefix}/skills`,
      },
      {
        titleKey: 'admin.dashboard.tagsCount',
        count: 0,
        icon: 'bi-tags',
        color: 'warning',
        link: `${prefix}/tags`,
      },
      {
        titleKey: 'admin.dashboard.experiencesCount',
        count: 0,
        icon: 'bi-clock-history',
        color: 'secondary',
        link: `${prefix}/experiences`,
      },
      {
        titleKey: 'admin.dashboard.articlesCount',
        count: 0,
        icon: 'bi-file-earmark-text',
        color: 'danger',
        link: `${prefix}/articles`,
      },
      {
        titleKey: 'admin.dashboard.cvsCount',
        count: 0,
        icon: 'bi-file-earmark-pdf',
        color: 'info',
        link: `${prefix}/cv`,
      },
    ];

    this.loadStats();
    this.loadBatchStats();
    this.loadCircuitBreakerStatus();

    // Only connect to visitor tracking if enabled and not in demo mode (GDPR opt-in)
    if (!this.demoModeService.isDemo() && this.visitorTrackingEnabled) {
      this.connectToActiveUsers();
      this.loadDailyVisitorData();
    }
  }

  private connectToActiveUsers(): void {
    // Subscribe to active users count updates
    this.activeUsersService.activeUsers$.pipe(takeUntil(this.destroy$)).subscribe((count) => {
      this.activeUsersCount = count;
    });

    // Subscribe to last month visitors count
    this.activeUsersService.lastMonthCount$.pipe(takeUntil(this.destroy$)).subscribe((count) => {
      this.lastMonthVisitorsCount = count;
    });

    // Subscribe to connection status
    this.activeUsersService.connected$.pipe(takeUntil(this.destroy$)).subscribe((connected) => {
      this.activeUsersConnected = connected;
    });

    // Fetch visitor stats (last month count)
    this.activeUsersService.fetchVisitorStats();

    // Connect to SSE stream
    this.activeUsersService.connect();
  }

  loadStats(): void {
    this.loading = true;
    this.logger.info('[HTTP_REQUEST] Loading dashboard stats');

    const isDemo = this.demoModeService.isDemo();

    forkJoin({
      projects: this.projectService.getAll(),
      skills: this.skillService.getAll(),
      tags: this.tagService.getAll(),
      experiences: this.experienceService.getAll(),
      articles: isDemo ? this.articleService.getAll() : this.articleService.getAllAdmin(),
      cvs: isDemo ? of([]) : this.cvService.getAllCvs(),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ projects, skills, tags, experiences, articles, cvs }) => {
          this.stats[0].count = projects.length;
          this.stats[1].count = skills.length;
          this.stats[2].count = tags.length;
          this.stats[3].count = experiences.length;
          this.stats[4].count = articles.length;
          this.stats[5].count = cvs.length;
          this.loading = false;
          this.logger.info('[HTTP_SUCCESS] Dashboard stats loaded', {
            projectCount: projects.length,
            skillCount: skills.length,
            tagCount: tags.length,
            experienceCount: experiences.length,
            articleCount: articles.length,
            cvCount: cvs.length,
          });
        },
        error: (error) => {
          this.stats[0].count = 0;
          this.stats[1].count = 0;
          this.stats[2].count = 0;
          this.stats[3].count = 0;
          this.stats[4].count = 0;
          this.stats[5].count = 0;
          this.loading = false;
          this.logger.error('[HTTP_ERROR] Failed to load dashboard stats', {
            status: error.status,
            message: error.message,
          });
        },
      });
  }

  loadBatchStats(): void {
    this.batchLoading = true;

    // In demo mode, return mock data
    if (this.demoModeService.isDemo()) {
      this.reprocessingStats = {
        projectImagesEligible: 3,
        articleImagesEligible: 2,
        totalEligible: 5,
        timestamp: new Date().toISOString(),
      };
      this.lastJobInfo = {
        lastJobId: 42,
        lastJobStatus: 'COMPLETED',
        lastJobDate: new Date(Date.now() - 86400000).toISOString(), // Yesterday
        exitCode: 'COMPLETED',
        processedCount: 12,
        errorCount: 0,
      };
      this.batchLoading = false;
      return;
    }

    forkJoin({
      stats: this.batchService.getReprocessingStats().pipe(catchError(() => of(null))),
      lastJob: this.batchService.getLastJob().pipe(catchError(() => of(null))),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ stats, lastJob }) => {
          this.reprocessingStats = stats;
          this.lastJobInfo = lastJob;
          this.batchLoading = false;
        },
        error: () => {
          this.batchLoading = false;
        },
      });
  }

  private loadCircuitBreakerStatus(): void {
    this.circuitBreakerLoading = true;

    // In demo mode, return mock data
    if (this.demoModeService.isDemo()) {
      this.circuitBreakers = [
        {
          name: 'emailService',
          state: 'CLOSED',
          metrics: {
            failureCount: 0,
            successCount: 156,
            bufferedCalls: 0,
            failureRate: 0,
            notPermittedCalls: 0,
          },
          timestamp: new Date().toISOString(),
        },
      ];
      this.circuitBreakerLoading = false;
      return;
    }

    this.circuitBreakerService
      .getAllCircuitBreakers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (statuses) => {
          this.circuitBreakers = statuses;
          this.circuitBreakerLoading = false;
        },
        error: () => {
          this.circuitBreakerLoading = false;
        },
      });
  }

  private loadDailyVisitorData(): void {
    this.activeUsersService
      .getDailyVisitorData()
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        this.dailyVisitorData = data;
      });
  }

  toggleVisitorsChart(): void {
    this.showVisitorsChart = !this.showVisitorsChart;
  }

  getCircuitBreakerStateClass(state: string): string {
    switch (state) {
      case 'CLOSED':
        return 'bg-success';
      case 'OPEN':
        return 'bg-danger';
      case 'HALF_OPEN':
        return 'bg-warning';
      default:
        return 'bg-secondary';
    }
  }

  openConfirmModal(): void {
    this.showConfirmModal = true;
    this.jobResult = null;
  }

  closeConfirmModal(): void {
    this.showConfirmModal = false;
  }

  runReprocessingJob(): void {
    if (this.demoModeService.isDemo()) {
      return;
    }
    this.isJobRunning = true;
    this.showConfirmModal = false;
    this.jobResult = null;

    this.batchService
      .runReprocessingJob()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.isJobRunning = false;
          this.jobResult = {
            success: result.exitCode === 'COMPLETED',
            message:
              result.exitCode === 'COMPLETED'
                ? this.translate.instant('admin.dashboard.jobSuccess')
                : this.translate.instant('admin.dashboard.jobFailed'),
          };
          this.loadBatchStats();
          this.logger.info('[BATCH] Reprocessing job completed', {
            jobId: result.jobId,
            status: result.status,
            exitCode: result.exitCode,
          });
        },
        error: (error) => {
          this.isJobRunning = false;
          this.jobResult = {
            success: false,
            message: this.translate.instant('admin.dashboard.jobError'),
          };
          this.logger.error('[BATCH] Reprocessing job failed', {
            status: error.status,
            message: error.message,
          });
        },
      });
  }

  ngOnDestroy(): void {
    this.activeUsersService.disconnect();
    this.destroy$.next();
    this.destroy$.complete();
  }
}
