import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ArticleResponse } from '../../models/article.model';
import { SiteConfigurationService } from '../../services/site-configuration.service';

@Component({
  selector: 'app-article-card',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './article-card.component.html',
  styleUrl: './article-card.component.css',
})
export class ArticleCardComponent implements OnInit {
  private readonly siteConfigService = inject(SiteConfigurationService);

  @Input({ required: true }) article!: ArticleResponse;

  authorName = '';

  ngOnInit(): void {
    this.siteConfigService.getSiteConfiguration().subscribe({
      next: (config) => {
        this.authorName = config.fullName;
      },
      error: () => {
        this.authorName = '';
      },
    });
  }
}
