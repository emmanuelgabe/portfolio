import { Component, HostListener, inject, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { ArticleService } from '../../services/article.service';
import { SiteConfigurationService } from '../../services/site-configuration.service';
import { User } from '../../models/auth.model';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class NavbarComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly articleService = inject(ArticleService);
  private readonly siteConfigService = inject(SiteConfigurationService);
  private readonly toastr = inject(ToastrService);
  private readonly router = inject(Router);
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

  scrollToSection(event: Event, sectionId: string) {
    event.preventDefault();
    this.closeNavbar();

    // If not on home page, navigate to home with fragment
    if (this.router.url !== '/' && !this.router.url.startsWith('/#')) {
      const fragment = sectionId === 'hero' ? undefined : sectionId;
      this.router.navigate(['/'], { fragment });
      return;
    }

    // For hero section, scroll to top of page
    if (sectionId === 'hero') {
      window.scrollTo({
        top: 0,
        behavior: 'smooth',
      });
      return;
    }

    // For other sections, calculate offset manually
    const element = document.getElementById(sectionId);
    if (element) {
      const navbarHeight = window.innerWidth > 1199 ? 76 : 66;
      const margin = 20;
      const elementPosition = element.getBoundingClientRect().top + window.pageYOffset;
      const offsetPosition = elementPosition - navbarHeight - margin;

      window.scrollTo({
        top: offsetPosition,
        behavior: 'smooth',
      });
    }
  }

  logout() {
    const username = this.currentUser?.username;
    this.authService.logout();
    this.closeNavbar();
    this.toastr.success(`À bientôt ${username} !`, 'Déconnexion réussie');
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
