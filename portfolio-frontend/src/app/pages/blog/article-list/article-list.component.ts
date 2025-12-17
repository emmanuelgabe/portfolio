import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ArticleResponse } from '../../../models/article.model';
import { ArticleService } from '../../../services/article.service';
import { SeoService } from '../../../services/seo.service';
import { ArticleCardComponent } from '../../../components/article-card/article-card.component';
import { SkeletonArticleCardComponent } from '../../../components/shared/skeleton';

@Component({
  selector: 'app-article-list',
  standalone: true,
  imports: [CommonModule, TranslateModule, ArticleCardComponent, SkeletonArticleCardComponent],
  templateUrl: './article-list.component.html',
  styleUrl: './article-list.component.css',
})
export class ArticleListComponent implements OnInit, OnDestroy {
  private readonly articleService = inject(ArticleService);
  private readonly seoService = inject(SeoService);
  private readonly translate = inject(TranslateService);
  private readonly destroy$ = new Subject<void>();

  articles: ArticleResponse[] = [];
  isLoading = true;
  error: string | undefined;

  // Pagination properties
  currentPage = 0;
  totalPages = 0;
  totalElements = 0;
  pageSize = 10;
  isFirst = true;
  isLast = true;

  ngOnInit(): void {
    this.seoService.updateMetaTags({
      title: 'Blog',
      description:
        'Articles sur le developpement web, Java, Spring Boot, Angular et les bonnes pratiques de programmation.',
      url: '/blog',
      type: 'website',
    });
    this.loadArticles();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadArticles(page: number = 0): void {
    this.isLoading = true;
    this.error = undefined;
    this.articleService
      .getAllPaginated(page, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.articles = response.content;
          this.currentPage = response.number;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          this.isFirst = response.first;
          this.isLast = response.last;
          this.isLoading = false;
        },
        error: () => {
          this.error = this.translate.instant('errors.loadingFailed');
          this.isLoading = false;
        },
      });
  }

  nextPage(): void {
    if (!this.isLast) {
      this.loadArticles(this.currentPage + 1);
    }
  }

  previousPage(): void {
    if (!this.isFirst) {
      this.loadArticles(this.currentPage - 1);
    }
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.loadArticles(page);
    }
  }
}
