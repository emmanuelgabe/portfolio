import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { filter } from 'rxjs/operators';
import { environment } from '../environments/environment';
import { NavbarComponent } from './components/navbar/navbar';
import { FooterComponent } from './components/footer/footer.component';
import { OfflineBannerComponent } from './components/offline-banner/offline-banner.component';
import { LoggerService } from './services/logger.service';
import { NavigationHistoryService } from './services/navigation-history.service';
import { LanguageService } from './services/language.service';
import { WebVitalsService } from './services/web-vitals.service';
import { VisitorTrackerService } from './services/visitor-tracker.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    TranslateModule,
    NavbarComponent,
    FooterComponent,
    OfflineBannerComponent,
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent implements OnInit {
  private readonly logger = inject(LoggerService);
  private readonly router = inject(Router);
  private readonly navigationHistory = inject(NavigationHistoryService);
  private readonly languageService = inject(LanguageService);
  private readonly webVitals = inject(WebVitalsService);
  private readonly visitorTracker = inject(VisitorTrackerService);

  version = environment.version;
  isAdminRoute = false;

  ngOnInit(): void {
    this.logger.info('[APP_INIT] Application started', { version: this.version });

    this.languageService.initialize();
    this.navigationHistory.initialize();
    this.webVitals.init();

    if (environment.visitorTracking.enabled) {
      this.visitorTracker.startTracking();
    }

    this.checkRoute(this.router.url);

    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.checkRoute(event.urlAfterRedirects);
        this.resetFocusToMainContent();
      });
  }

  private checkRoute(url: string): void {
    this.isAdminRoute = url.startsWith('/admin');
  }

  private resetFocusToMainContent(): void {
    const mainContent = document.getElementById('main-content');
    if (mainContent) {
      mainContent.focus();
    }
  }
}
