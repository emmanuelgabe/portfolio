import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SkeletonTableRowComponent } from '../../../../components/shared/skeleton';
import { SearchInputComponent } from '../../../../components/shared/search-input/search-input.component';
import { ArticleService } from '../../../../services/article.service';
import { SearchService } from '../../../../services/search.service';
import { ModalService } from '../../../../services/modal.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { ArticleResponse } from '../../../../models/article.model';
import { ArticleSearchResult } from '../../../../models/search.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-admin-article-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    TranslateModule,
    SkeletonTableRowComponent,
    SearchInputComponent,
  ],
  templateUrl: './article-list.component.html',
  styleUrls: ['./article-list.component.scss'],
})
export class AdminArticleListComponent implements OnInit, OnDestroy {
  private readonly articleService = inject(ArticleService);
  private readonly searchService = inject(SearchService);
  private readonly modalService = inject(ModalService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);
  private readonly destroy$ = new Subject<void>();
  readonly demoModeService = inject(DemoModeService);

  articles: ArticleResponse[] = [];
  loading = false;
  error: string | undefined;
  filterStatus: 'all' | 'published' | 'draft' = 'all';

  // Search state
  searchQuery = '';
  searchResults: ArticleSearchResult[] = [];
  isSearching = false;
  isSearchActive = false;

  ngOnInit(): void {
    this.loadArticles();
  }

  loadArticles(): void {
    this.loading = true;
    this.error = undefined;

    const isDemo = this.demoModeService.isDemo();
    this.logger.info('[HTTP_REQUEST] Loading articles', { isDemo });

    const articlesObservable = isDemo
      ? this.articleService.getAll()
      : this.articleService.getAllAdmin();

    articlesObservable.pipe(takeUntil(this.destroy$)).subscribe({
      next: (articles) => {
        this.articles = articles;
        this.loading = false;
        this.logger.info('[HTTP_SUCCESS] Articles loaded', { count: articles.length });
      },
      error: (error) => {
        this.error = this.translate.instant('admin.articles.loadError');
        this.loading = false;
        this.logger.error('[HTTP_ERROR] Failed to load articles', { error });
        this.toastr.error(this.translate.instant('admin.articles.loadError'));
      },
    });
  }

  get filteredArticles(): ArticleResponse[] {
    // If search is active, filter from search results
    if (this.isSearchActive) {
      if (this.searchResults.length === 0) {
        return [];
      }
      const searchIds = new Set(this.searchResults.map((r) => r.id));
      let filtered = this.articles.filter((a) => searchIds.has(a.id));

      // Apply status filter on search results
      if (this.filterStatus !== 'all') {
        filtered = filtered.filter((article) =>
          this.filterStatus === 'published' ? !article.draft : article.draft
        );
      }
      return filtered;
    }

    // No search active, apply only status filter
    if (this.filterStatus === 'all') {
      return this.articles;
    }
    return this.articles.filter((article) =>
      this.filterStatus === 'published' ? !article.draft : article.draft
    );
  }

  onSearch(query: string): void {
    this.searchQuery = query;
    this.isSearching = true;
    this.logger.info('[SEARCH] Searching articles', { query });

    this.searchService
      .searchArticles(query)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (results) => {
          this.searchResults = results;
          this.isSearchActive = true;
          this.isSearching = false;
          this.logger.info('[SEARCH] Search completed', { count: results.length });
        },
        error: (error) => {
          this.isSearching = false;
          this.logger.error('[SEARCH] Search failed', { error });
          this.toastr.error(this.translate.instant('admin.common.searchError'));
        },
      });
  }

  onSearchClear(): void {
    this.searchQuery = '';
    this.searchResults = [];
    this.isSearchActive = false;
    this.logger.info('[SEARCH] Search cleared');
  }

  get publishedCount(): number {
    return this.articles.filter((a) => !a.draft).length;
  }

  get draftCount(): number {
    return this.articles.filter((a) => a.draft).length;
  }

  deleteArticle(article: ArticleResponse): void {
    this.logger.info('[USER_ACTION] Delete requested', { id: article.id, title: article.title });

    this.modalService
      .confirmDelete(article.title, this.demoModeService.isDemo())
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (confirmed) => {
          if (confirmed) {
            if (this.demoModeService.isDemo()) {
              this.toastr.info(this.translate.instant('admin.common.demoModeDisabled'));
              return;
            }
            this.performDelete(article);
          } else {
            this.logger.info('[USER_ACTION] Delete cancelled', { id: article.id });
          }
        },
        error: () => {
          this.logger.info('[USER_ACTION] Delete modal dismissed', { id: article.id });
        },
      });
  }

  private performDelete(article: ArticleResponse): void {
    this.logger.info('[HTTP_REQUEST] Deleting article', { id: article.id });

    this.articleService
      .delete(article.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.articles = this.articles.filter((a) => a.id !== article.id);
          this.logger.info('[HTTP_SUCCESS] Article deleted', { id: article.id });
          this.toastr.success(this.translate.instant('admin.common.delete'));
        },
        error: (error) => {
          this.logger.error('[HTTP_ERROR] Failed to delete article', { id: article.id, error });
          this.toastr.error(this.translate.instant('admin.articles.updateError'));
        },
      });
  }

  togglePublish(article: ArticleResponse): void {
    if (this.demoModeService.isDemo()) {
      this.toastr.info(this.translate.instant('admin.common.demoModeDisabled'));
      return;
    }

    const action = article.draft ? 'publish' : 'unpublish';
    this.logger.info(`[USER_ACTION] ${action} requested`, { id: article.id });

    const serviceCall = article.draft
      ? this.articleService.publish(article.id)
      : this.articleService.unpublish(article.id);

    serviceCall.pipe(takeUntil(this.destroy$)).subscribe({
      next: (updatedArticle) => {
        const index = this.articles.findIndex((a) => a.id === article.id);
        if (index !== -1) {
          this.articles[index] = updatedArticle;
        }
        this.logger.info(`[HTTP_SUCCESS] Article ${action}ed`, { id: article.id });
        this.toastr.success(this.translate.instant('admin.articles.updatedPublished'));
      },
      error: (error) => {
        this.logger.error(`[HTTP_ERROR] Failed to ${action} article`, { id: article.id, error });
        this.toastr.error(this.translate.instant('admin.articles.updateError'));
      },
    });
  }

  getStatusBadgeClass(article: ArticleResponse): string {
    return article.draft ? 'bg-warning text-dark' : 'bg-success';
  }

  getStatusText(article: ArticleResponse): string {
    return article.draft
      ? this.translate.instant('admin.articles.draft')
      : this.translate.instant('admin.articles.published');
  }

  getEmptyMessage(): string {
    if (this.filterStatus === 'all') {
      return this.translate.instant('admin.articles.noArticles');
    } else if (this.filterStatus === 'published') {
      return this.translate.instant('admin.articles.noPublished');
    }
    return this.translate.instant('admin.articles.noDrafts');
  }

  formatDate(dateString: string | null): string {
    if (!dateString) {
      return '-';
    }
    return new Date(dateString).toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
