import { Component, HostListener, inject, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { ArticleService } from '../../services/article.service';
import { SiteConfigurationService } from '../../services/site-configuration.service';
import { ScrollService } from '../../services/scroll.service';
import { LanguageSelectorComponent } from '../shared/language-selector/language-selector.component';
import { User } from '../../models/auth.model';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule, LanguageSelectorComponent],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class NavbarComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly articleService = inject(ArticleService);
  private readonly siteConfigService = inject(SiteConfigurationService);
  private readonly scrollService = inject(ScrollService);
  private readonly toastr = inject(ToastrService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  private readonly destroy$ = new Subject<void>();

  isCollapsed = true;
  isScrolled = false;
  currentUser: User | undefined;
  hasArticles = false;
  fullName = 'Emmanuel Gabe';

  ngOnInit(): void {
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      this.currentUser = user ?? undefined;
    });
    this.checkArticlesAvailability();
    this.loadSiteConfiguration();
  }

  private loadSiteConfiguration(): void {
    this.siteConfigService
      .getSiteConfiguration()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (config) => {
          this.fullName = config.fullName;
        },
      });
  }

  private checkArticlesAvailability(): void {
    this.articleService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (articles) => {
          this.hasArticles = articles.length > 0;
        },
        error: () => {
          this.hasArticles = false;
        },
      });
  }

  toggleNavbar() {
    this.isCollapsed = !this.isCollapsed;
  }

  closeNavbar() {
    this.isCollapsed = true;
  }

  scrollToSection(event: Event, sectionId: string): void {
    event.preventDefault();
    this.closeNavbar();

    if (this.router.url !== '/' && !this.router.url.startsWith('/#')) {
      const fragment = sectionId === 'hero' ? undefined : sectionId;
      this.router.navigate(['/'], { fragment });
      return;
    }

    this.scrollService.scrollToSection(sectionId, 'smooth');
  }

  logout() {
    const username = this.currentUser?.username;
    this.authService.logout();
    this.closeNavbar();
    this.toastr.success(
      `${this.translate.instant('nav.logoutSuccess')} ${username} !`,
      this.translate.instant('nav.logoutSuccessTitle')
    );
  }

  get isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  @HostListener('window:scroll', [])
  onWindowScroll() {
    this.isScrolled = window.scrollY > 50;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
