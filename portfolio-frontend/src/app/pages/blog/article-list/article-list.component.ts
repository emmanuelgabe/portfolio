import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ArticleResponse } from '../../../models/article.model';
import { ArticleService } from '../../../services/article.service';
import { ArticleCardComponent } from '../../../components/article-card/article-card.component';

@Component({
  selector: 'app-article-list',
  standalone: true,
  imports: [CommonModule, ArticleCardComponent],
  templateUrl: './article-list.component.html',
  styleUrl: './article-list.component.css',
})
export class ArticleListComponent implements OnInit {
  private readonly articleService = inject(ArticleService);

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
    this.loadArticles();
  }

  loadArticles(page: number = 0): void {
    this.isLoading = true;
    this.error = undefined;
    this.articleService.getAllPaginated(page, this.pageSize).subscribe({
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
        this.error = 'Erreur lors du chargement des articles';
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
