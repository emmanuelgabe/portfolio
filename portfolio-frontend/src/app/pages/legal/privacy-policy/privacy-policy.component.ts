import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { SiteConfigurationService } from '../../../services/site-configuration.service';

@Component({
  selector: 'app-privacy-policy',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './privacy-policy.component.html',
  styleUrls: ['./privacy-policy.component.css'],
})
export class PrivacyPolicyComponent implements OnInit {
  private readonly siteConfigService = inject(SiteConfigurationService);

  contactEmail = '';
  fullName = '';
  lastUpdated = new Date().toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  ngOnInit(): void {
    this.siteConfigService.getSiteConfiguration().subscribe({
      next: (config) => {
        this.contactEmail = config.email;
        this.fullName = config.fullName;
      },
    });
  }
}
