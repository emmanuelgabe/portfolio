import { Component, OnInit, AfterViewChecked, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Meta, Title } from '@angular/platform-browser';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ArticleResponse } from '../../../models/article.model';
import { ArticleService } from '../../../services/article.service';

declare const Prism: any;

@Component({
  selector: 'app-article-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './article-detail.component.html',
  styleUrl: './article-detail.component.css',
})
export class ArticleDetailComponent implements OnInit, AfterViewChecked {
  private readonly route = inject(ActivatedRoute);
  private readonly articleService = inject(ArticleService);
  private readonly meta = inject(Meta);
  private readonly titleService = inject(Title);
  private readonly sanitizer = inject(DomSanitizer);

  article: ArticleResponse | undefined;
  isLoading = true;
  error: string | undefined;
  safeHtml: SafeHtml | undefined;
  private highlighted = false;

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (slug) {
      this.loadArticle(slug);
    }
  }

  ngAfterViewChecked(): void {
    if (this.article && !this.highlighted) {
      this.highlightCode();
    }
  }

  loadArticle(slug: string): void {
    this.articleService.getBySlug(slug).subscribe({
      next: (data) => {
        this.article = data;
        this.safeHtml = this.sanitizer.bypassSecurityTrustHtml(data.contentHtml);
        this.updateMetaTags(data);
        this.isLoading = false;
      },
      error: () => {
        this.error = 'Article non trouvé';
        this.isLoading = false;
      },
    });
  }

  updateMetaTags(article: ArticleResponse): void {
    this.titleService.setTitle(`${article.title} - Portfolio`);
    this.meta.updateTag({ name: 'description', content: article.excerpt });
    this.meta.updateTag({ property: 'og:title', content: article.title });
    this.meta.updateTag({ property: 'og:description', content: article.excerpt });
    this.meta.updateTag({ property: 'og:type', content: 'article' });
    this.meta.updateTag({ property: 'article:published_time', content: article.publishedAt });
    this.meta.updateTag({ property: 'article:author', content: article.authorName });
  }

  highlightCode(): void {
    if (typeof Prism !== 'undefined') {
      Prism.highlightAll();
      this.highlighted = true;
    }
  }
}
