import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ContactFormComponent } from '../../components/contact-form/contact-form.component';
import { SiteConfigurationService } from '../../services/site-configuration.service';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, ContactFormComponent],
  templateUrl: './contact.component.html',
  styleUrl: './contact.component.css',
})
export class ContactComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly siteConfigService = inject(SiteConfigurationService);
  private readonly logger = inject(LoggerService);

  // Site configuration properties
  contactEmail = 'contact@emmanuelgabe.com';
  linkedinUrl = 'https://linkedin.com/in/egabe';
  githubUrl = 'https://github.com/emmanuelgabe';
  isLoadingConfig = false;

  ngOnInit(): void {
    window.scrollTo({ top: 0, behavior: 'auto' });
    this.loadSiteConfiguration();
  }

  /**
   * Load site configuration from API
   */
  private loadSiteConfiguration(): void {
    this.isLoadingConfig = true;

    this.siteConfigService.getSiteConfiguration().subscribe({
      next: (config) => {
        this.contactEmail = config.email;
        this.linkedinUrl = config.linkedinUrl;
        this.githubUrl = config.githubUrl;
        this.isLoadingConfig = false;
      },
      error: (err) => {
        this.logger.error('[HTTP_ERROR] Failed to load site configuration', {
          error: err.message || err,
        });
        // Keep default values on error
        this.isLoadingConfig = false;
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/']);
  }
}
