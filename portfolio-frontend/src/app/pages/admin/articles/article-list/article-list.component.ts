import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ArticleService } from '../../../../services/article.service';
import { ModalService } from '../../../../services/modal.service';
import { LoggerService } from '../../../../services/logger.service';
import { I18nService } from '../../../../services/i18n.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { ArticleResponse } from '../../../../models/article.model';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-admin-article-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './article-list.component.html',
  styleUrls: ['./article-list.component.scss'],
})
export class AdminArticleListComponent implements OnInit {
  private readonly articleService = inject(ArticleService);
  private readonly modalService = inject(ModalService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  private readonly i18n = inject(I18nService);
  readonly demoModeService = inject(DemoModeService);

  articles: ArticleResponse[] = [];
  loading = false;
  error: string | undefined;
  filterStatus: 'all' | 'published' | 'draft' = 'all';

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

    articlesObservable.subscribe({
      next: (articles) => {
        this.articles = articles;
        this.loading = false;
        this.logger.info('[HTTP_SUCCESS] Articles loaded', { count: articles.length });
      },
      error: (error) => {
        this.error = 'Failed to load articles';
        this.loading = false;
        this.logger.error('[HTTP_ERROR] Failed to load articles', { error });
        this.toastr.error(this.i18n.get('article.list.loadError'));
      },
    });
  }

  get filteredArticles(): ArticleResponse[] {
    if (this.filterStatus === 'all') {
      return this.articles;
    }
    return this.articles.filter((article) =>
      this.filterStatus === 'published' ? !article.draft : article.draft
    );
  }

  get publishedCount(): number {
    return this.articles.filter((a) => !a.draft).length;
  }

  get draftCount(): number {
    return this.articles.filter((a) => a.draft).length;
  }

  deleteArticle(article: ArticleResponse): void {
    this.logger.info('[USER_ACTION] Delete requested', { id: article.id, title: article.title });

    this.modalService.confirmDelete(article.title, this.demoModeService.isDemo()).subscribe({
      next: (confirmed) => {
        if (confirmed) {
          if (this.demoModeService.isDemo()) {
            this.toastr.info('Action non disponible en mode démonstration');
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

    this.articleService.delete(article.id).subscribe({
      next: () => {
        this.articles = this.articles.filter((a) => a.id !== article.id);
        this.logger.info('[HTTP_SUCCESS] Article deleted', { id: article.id });
        this.toastr.success(
          `Article "${article.title}" ${this.i18n.get('article.list.deleteConfirm')}`,
          this.i18n.get('article.list.deleteSuccess')
        );
      },
      error: (error) => {
        this.logger.error('[HTTP_ERROR] Failed to delete article', { id: article.id, error });
        this.toastr.error(
          this.i18n.get('article.list.deleteError'),
          this.i18n.get('article.error')
        );
      },
    });
  }

  togglePublish(article: ArticleResponse): void {
    if (this.demoModeService.isDemo()) {
      this.toastr.info('Action non disponible en mode démonstration');
      return;
    }

    const action = article.draft ? 'publish' : 'unpublish';
    this.logger.info(`[USER_ACTION] ${action} requested`, { id: article.id });

    const serviceCall = article.draft
      ? this.articleService.publish(article.id)
      : this.articleService.unpublish(article.id);

    serviceCall.subscribe({
      next: (updatedArticle) => {
        const index = this.articles.findIndex((a) => a.id === article.id);
        if (index !== -1) {
          this.articles[index] = updatedArticle;
        }
        this.logger.info(`[HTTP_SUCCESS] Article ${action}ed`, { id: article.id });
        const statusKey = article.draft ? 'article.list.published' : 'article.list.unpublished';
        this.toastr.success(
          `Article "${article.title}" ${this.i18n.get(statusKey)}`,
          this.i18n.get('article.list.publishSuccess')
        );
      },
      error: (error) => {
        this.logger.error(`[HTTP_ERROR] Failed to ${action} article`, { id: article.id, error });
        const errorKey = article.draft
          ? 'article.list.publishError'
          : 'article.list.unpublishError';
        this.toastr.error(
          `Erreur lors de la ${this.i18n.get(errorKey)}`,
          this.i18n.get('article.error')
        );
      },
    });
  }

  getStatusBadgeClass(article: ArticleResponse): string {
    return article.draft ? 'bg-warning text-dark' : 'bg-success';
  }

  getStatusText(article: ArticleResponse): string {
    return article.draft
      ? this.i18n.get('article.list.statusDraft')
      : this.i18n.get('article.list.statusPublished');
  }

  formatDate(dateString: string | null): string {
    if (!dateString) {
      return this.i18n.get('article.list.notPublished');
    }
    return new Date(dateString).toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }
}
