import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SkeletonTableRowComponent } from '../../../../components/shared/skeleton';
import { ContactAdminService } from '../../../../services/contact-admin.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import {
  ContactStatsResponse,
  ContactHistoryResponse,
  ContactCategory,
} from '../../../../models/contact-moderation.model';
import { Page } from '../../../../models/audit.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-admin-contact-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, SkeletonTableRowComponent],
  templateUrl: './contact-list.component.html',
  styleUrls: ['./contact-list.component.scss'],
})
export class AdminContactListComponent implements OnInit {
  private readonly contactAdminService = inject(ContactAdminService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);
  readonly demoModeService = inject(DemoModeService);

  readonly Math = Math;

  // Stats
  stats: ContactStatsResponse | undefined;
  statsLoading = false;

  // History
  contacts: ContactHistoryResponse[] = [];
  historyLoading = false;
  error: string | undefined;

  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  // Filters
  categories: ContactCategory[] = [];
  selectedCategory: ContactCategory | undefined;
  selectedDays = 30;

  readonly periodOptions = [7, 30, 90, 365];

  ngOnInit(): void {
    if (this.demoModeService.isDemoRoute()) {
      this.loadDemoData();
      return;
    }

    this.loadCategories();
    this.loadStats();
    this.loadHistory();
  }

  loadStats(): void {
    this.statsLoading = true;

    this.contactAdminService.getStats(this.selectedDays).subscribe({
      next: (stats) => {
        this.stats = stats;
        this.statsLoading = false;
        this.logger.info('[HTTP_SUCCESS] Contact stats loaded', {
          total: stats.totalCount,
        });
      },
      error: () => {
        this.statsLoading = false;
        this.logger.error('[HTTP_ERROR] Failed to load contact stats');
      },
    });
  }

  loadHistory(): void {
    this.historyLoading = true;
    this.error = undefined;

    this.contactAdminService
      .getHistory(this.selectedCategory, this.currentPage, this.pageSize)
      .subscribe({
        next: (page: Page<ContactHistoryResponse>) => {
          this.contacts = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.historyLoading = false;
          this.logger.info('[HTTP_SUCCESS] Contact history loaded', {
            count: page.content.length,
          });
        },
        error: () => {
          this.error = this.translate.instant('admin.contacts.loadError');
          this.historyLoading = false;
          this.toastr.error(this.translate.instant('admin.contacts.loadError'));
        },
      });
  }

  loadCategories(): void {
    this.contactAdminService.getCategories().subscribe({
      next: (categories) => (this.categories = categories),
      error: (error) => this.logger.error('[HTTP_ERROR] Failed to load categories', { error }),
    });
  }

  onPeriodChange(): void {
    this.loadStats();
  }

  onCategoryChange(): void {
    this.currentPage = 0;
    this.loadHistory();
  }

  clearFilters(): void {
    this.selectedCategory = undefined;
    this.currentPage = 0;
    this.loadHistory();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadHistory();
    }
  }

  getCategoryBadgeClass(category: ContactCategory): string {
    switch (category) {
      case 'MISSION':
        return 'bg-primary';
      case 'QUESTION':
        return 'bg-info';
      case 'FEEDBACK':
        return 'bg-success';
      case 'SPAM':
        return 'bg-danger';
      case 'INAPPROPRIATE':
        return 'bg-dark';
      default:
        return 'bg-secondary';
    }
  }

  getVerdictBadgeClass(verdict: string): string {
    return verdict === 'APPROVED' ? 'bg-success' : 'bg-danger';
  }

  getConfidenceClass(score: number): string {
    if (score >= 0.8) return 'text-success';
    if (score >= 0.5) return 'text-warning';
    return 'text-danger';
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

  formatPercent(score: number): string {
    return `${Math.round(score * 100)}%`;
  }

  get approvedCount(): number {
    return this.stats?.verdictCounts?.['APPROVED'] ?? 0;
  }

  get rejectedCount(): number {
    return this.stats?.verdictCounts?.['REJECTED'] ?? 0;
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

  private loadDemoData(): void {
    this.categories = ['MISSION', 'QUESTION', 'FEEDBACK', 'SPAM', 'INAPPROPRIATE'];

    this.stats = {
      totalCount: 47,
      categoryCounts: {
        MISSION: 18,
        QUESTION: 12,
        FEEDBACK: 8,
        SPAM: 7,
        INAPPROPRIATE: 2,
      },
      verdictCounts: {
        APPROVED: 38,
        REJECTED: 9,
      },
      dailyActivity: {},
    };

    this.contacts = [
      {
        id: 1,
        name: 'Marie D.',
        subject: 'Proposition de mission freelance',
        message: 'Bonjour, je recherche un développeur fullstack pour un projet e-commerce...',
        category: 'MISSION',
        moderationVerdict: 'APPROVED',
        moderationReason: 'Legitimate business inquiry about freelance work',
        confidenceScore: 0.95,
        createdAt: '2026-04-05T14:30:00',
      },
      {
        id: 2,
        name: 'Thomas R.',
        subject: 'Question sur votre stack technique',
        message: 'Quel framework utilisez-vous pour le frontend de votre portfolio ?',
        category: 'QUESTION',
        moderationVerdict: 'APPROVED',
        moderationReason: 'Genuine technical question about the portfolio',
        confidenceScore: 0.92,
        createdAt: '2026-04-04T09:15:00',
      },
      {
        id: 3,
        name: 'Sophie L.',
        subject: 'Bravo pour le portfolio',
        message: "J'aime beaucoup le design et la qualité technique de votre site.",
        category: 'FEEDBACK',
        moderationVerdict: 'APPROVED',
        moderationReason: 'Positive feedback about portfolio quality',
        confidenceScore: 0.98,
        createdAt: '2026-04-03T16:45:00',
      },
      {
        id: 4,
        name: 'SEO Expert',
        subject: 'Boost your website ranking NOW!!!',
        message: 'Buy 10000 backlinks for only $99. Limited offer!',
        category: 'SPAM',
        moderationVerdict: 'REJECTED',
        moderationReason: 'Commercial spam offering SEO services',
        confidenceScore: 0.99,
        createdAt: '2026-04-02T11:20:00',
      },
      {
        id: 5,
        name: 'Pierre M.',
        subject: 'Recrutement CDI développeur senior',
        message: 'Notre entreprise recrute un lead dev Java/Angular. Seriez-vous intéressé ?',
        category: 'MISSION',
        moderationVerdict: 'APPROVED',
        moderationReason: 'Legitimate job recruitment offer',
        confidenceScore: 0.91,
        createdAt: '2026-04-01T08:00:00',
      },
      {
        id: 6,
        name: 'xxx_user',
        subject: 'Contenu inapproprié',
        message: '[Contenu masqué par la modération]',
        category: 'INAPPROPRIATE',
        moderationVerdict: 'REJECTED',
        moderationReason: 'Message contains abusive language',
        confidenceScore: 0.97,
        createdAt: '2026-03-30T22:10:00',
      },
      {
        id: 7,
        name: 'Julie B.',
        subject: 'Aide sur un projet Spring Boot',
        message: 'Comment gérez-vous la configuration multi-environnements ?',
        category: 'QUESTION',
        moderationVerdict: 'APPROVED',
        moderationReason: 'Technical question about Spring Boot configuration',
        confidenceScore: 0.88,
        createdAt: '2026-03-29T13:50:00',
      },
    ];

    this.totalElements = this.contacts.length;
    this.totalPages = 1;
  }
}
