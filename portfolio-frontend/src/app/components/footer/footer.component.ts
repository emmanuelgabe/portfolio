import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SiteConfigurationService } from '../../services/site-configuration.service';
import { CvService } from '../../services/cv.service';
import { CvResponse } from '../../models/cv.model';
import { LoggerService } from '../../services/logger.service';
import { ToastrService } from 'ngx-toastr';
import { VERSION } from '../../../environments/version';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.css'],
})
export class FooterComponent implements OnInit {
  private readonly siteConfigService = inject(SiteConfigurationService);
  private readonly cvService = inject(CvService);
  private readonly logger = inject(LoggerService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);

  githubUrl = 'https://github.com/emmanuelgabe';
  linkedinUrl = 'https://linkedin.com/in/egabe';
  contactEmail = 'contact@emmanuelgabe.com';
  fullName = 'Emmanuel Gabe';
  currentYear = new Date().getFullYear();
  version = VERSION;
  currentCv?: CvResponse;

  ngOnInit(): void {
    this.loadSiteConfiguration();
    this.loadCv();
  }

  private loadSiteConfiguration(): void {
    this.siteConfigService.getSiteConfiguration().subscribe({
      next: (config) => {
        this.fullName = config.fullName;
        this.githubUrl = config.githubUrl;
        this.linkedinUrl = config.linkedinUrl;
        this.contactEmail = config.email;
      },
      error: (err) => {
        this.logger.error('[HTTP_ERROR] Failed to load site configuration for footer', {
          error: err.message || err,
        });
      },
    });
  }

  private loadCv(): void {
    this.cvService.getCurrentCv().subscribe({
      next: (cv) => {
        this.currentCv = cv ?? undefined;
      },
      error: (err) => {
        this.logger.error('[HTTP_ERROR] Failed to load CV for footer', {
          status: err.status,
          message: err.message,
        });
        this.currentCv = undefined;
      },
    });
  }

  downloadCv(): void {
    if (!this.currentCv) {
      this.toastr.info(this.translate.instant('cv.notAvailable'));
      return;
    }
    this.logger.info('[USER_ACTION] User clicked download CV button from footer');
    this.cvService.downloadCv();
  }
}
