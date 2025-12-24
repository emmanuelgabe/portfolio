import { Component, OnInit, AfterViewChecked, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ArticleResponse } from '../../../models/article.model';
import { ArticleService } from '../../../services/article.service';
import { SeoService } from '../../../services/seo.service';
import { SkeletonArticleDetailComponent } from '../../../components/shared/skeleton';

declare const Prism: typeof import('prismjs');

@Component({
  selector: 'app-article-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, SkeletonArticleDetailComponent],
  templateUrl: './article-detail.component.html',
  styleUrl: './article-detail.component.css',
})
export class ArticleDetailComponent implements OnInit, AfterViewChecked {
  private readonly route = inject(ActivatedRoute);
  private readonly articleService = inject(ArticleService);
  private readonly seoService = inject(SeoService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly translate = inject(TranslateService);

  article: ArticleResponse | undefined;
  isLoading = true;
  error: string | undefined;
  notFound = false;
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
      error: (err) => {
        if (err.status === 404) {
          this.notFound = true;
        } else {
          this.error = this.translate.instant('errors.loadingFailed');
        }
        this.isLoading = false;
      },
    });
  }

  updateMetaTags(article: ArticleResponse): void {
    const readyImage = article.images?.find((img) => img.status === 'READY');
    const articleImage = readyImage?.imageUrl;
    const articleUrl = `/blog/${article.slug}`;
    const tagNames = article.tags?.map((t) => t.name) || [];

    this.seoService.updateMetaTags({
      title: article.title,
      description: article.excerpt,
      image: articleImage,
      url: articleUrl,
      type: 'article',
      article: {
        publishedTime: article.publishedAt,
        modifiedTime: article.updatedAt,
        author: article.authorName,
        tags: tagNames,
      },
    });

    this.seoService.setArticleSchema({
      title: article.title,
      description: article.excerpt,
      image: articleImage,
      url: articleUrl,
      publishedDate: article.publishedAt,
      modifiedDate: article.updatedAt,
      authorName: article.authorName,
      tags: tagNames,
    });
  }

  highlightCode(): void {
    if (typeof Prism !== 'undefined') {
      Prism.highlightAll();
      this.highlighted = true;
    }
  }
}
