import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';
import { ContactFormComponent } from '../../components/contact-form/contact-form.component';
import { SiteConfigurationService } from '../../services/site-configuration.service';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, ContactFormComponent, TranslateModule],
  templateUrl: './contact.component.html',
  styleUrl: './contact.component.css',
})
export class ContactComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly siteConfigService = inject(SiteConfigurationService);
  private readonly logger = inject(LoggerService);
  private readonly destroy$ = new Subject<void>();

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

    this.siteConfigService
      .getSiteConfiguration()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
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

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  goBack(): void {
    this.router.navigate(['/']);
  }
}
