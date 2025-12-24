import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { SiteConfigurationService } from '../../../services/site-configuration.service';

@Component({
  selector: 'app-legal-notice',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './legal-notice.component.html',
  styleUrls: ['./legal-notice.component.css'],
})
export class LegalNoticeComponent implements OnInit {
  private readonly siteConfigService = inject(SiteConfigurationService);

  contactEmail = '';
  fullName = '';
  currentYear = new Date().getFullYear();

  ngOnInit(): void {
    this.siteConfigService.getSiteConfiguration().subscribe({
      next: (config) => {
        this.contactEmail = config.email;
        this.fullName = config.fullName;
      },
    });
  }
}
