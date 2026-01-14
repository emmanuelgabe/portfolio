import {
  Component,
  OnInit,
  AfterViewInit,
  OnDestroy,
  inject,
  ElementRef,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ProjectCardComponent } from '../project-card/project-card.component';
import { ArticleCardComponent } from '../article-card/article-card.component';
import {
  SkeletonProjectCardComponent,
  SkeletonArticleCardComponent,
  SkeletonSkillCardComponent,
  SkeletonTimelineItemComponent,
} from '../shared/skeleton';
import { ProjectService, SkillService, ScrollService } from '../../services';
import { CvService } from '../../services/cv.service';
import { ExperienceService } from '../../services/experience.service';
import { ArticleService } from '../../services/article.service';
import { SiteConfigurationService } from '../../services/site-configuration.service';
import { SeoService } from '../../services/seo.service';
import { ProjectResponse, ExperienceResponse, ExperienceType } from '../../models';
import { Skill } from '../../models/skill.model';
import { CvResponse } from '../../models/cv.model';
import { ArticleResponse } from '../../models/article.model';
import { VERSION } from '../../../environments/version';
import { LoggerService } from '../../services/logger.service';
import { MarkdownService } from '../../services/markdown.service';
import { getLocaleFromLang } from '../../utils/locale.utils';
import { ToastrService } from 'ngx-toastr';
import { interval, Subject, Subscription } from 'rxjs';
import { take, takeUntil } from 'rxjs/operators';
import lottie, { AnimationItem } from 'lottie-web';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    ProjectCardComponent,
    ArticleCardComponent,
    RouterLink,
    SkeletonProjectCardComponent,
    SkeletonArticleCardComponent,
    SkeletonSkillCardComponent,
    SkeletonTimelineItemComponent,
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly projectService = inject(ProjectService);
  private readonly skillService = inject(SkillService);
  private readonly cvService = inject(CvService);
  private readonly experienceService = inject(ExperienceService);
  private readonly articleService = inject(ArticleService);
  private readonly siteConfigService = inject(SiteConfigurationService);
  private readonly seoService = inject(SeoService);
  private readonly scrollService = inject(ScrollService);
  private readonly logger = inject(LoggerService);
  private readonly markdownService = inject(MarkdownService);
  private readonly toastr = inject(ToastrService);
  private readonly translate = inject(TranslateService);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);

  @ViewChild('scrollAnimation', { static: false }) scrollAnimationContainer?: ElementRef;
  @ViewChild('birdsAnimation', { static: false }) birdsAnimationContainer?: ElementRef;
  private scrollAnimation?: AnimationItem;
  private birdsAnimation?: AnimationItem;

  allProjects: ProjectResponse[] = [];
  isLoadingProjects = false;
  projectsError?: string;
  showAllProjects = false;

  skills: Skill[] = [];
  isLoadingSkills = false;
  skillsError?: string;

  currentCv?: CvResponse;
  isLoadingCv = false;

  allExperiences: ExperienceResponse[] = [];
  isLoadingExperiences = false;
  experiencesError?: string;

  allArticles: ArticleResponse[] = [];
  isLoadingArticles = false;
  articlesError?: string;

  readonly ExperienceType = ExperienceType;

  version = VERSION;

  // Hero section loading state
  isLoadingHero = false;

  // Site configuration properties (social links, email, identity)
  githubUrl = 'https://github.com/emmanuelgabe';
  linkedinUrl = 'https://linkedin.com/in/egabe';
  contactEmail = 'contact@emmanuelgabe.com';
  fullName = 'Emmanuel Gabe';
  currentYear = new Date().getFullYear();

  // Typing animation properties
  fullText = '';
  displayedText = '';
  showCursor = true;
  private typingSubscription?: Subscription;

  // Description typing animation properties
  fullDescriptionLines: string[] = [];
  displayedDescriptionLines: string[] = [];
  private descriptionTypingSubscriptions: Subscription[] = [];

  // Cleanup
  private readonly destroy$ = new Subject<void>();
  private intersectionObserver?: IntersectionObserver;
  private pendingTimeouts: ReturnType<typeof setTimeout>[] = [];

  ngOnInit(): void {
    this.loadHeroSection();
    this.loadProjects();
    this.loadSkills();
    this.loadCv();
    this.loadAllExperiences();
    this.loadArticles();
    this.handleFragmentScroll();
  }

  /**
   * Handle scroll to section based on URL fragment
   */
  private handleFragmentScroll(): void {
    this.route.fragment.pipe(takeUntil(this.destroy$)).subscribe((fragment) => {
      if (!fragment) return;

      const timeout = setTimeout(() => {
        this.scrollService.scrollToSection(fragment, 'auto');
      }, 100);
      this.pendingTimeouts.push(timeout);
    });
  }

  /**
   * Load site configuration (hero section + social links) from API
   */
  loadHeroSection(): void {
    this.isLoadingHero = true;

    this.siteConfigService
      .getSiteConfiguration()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (config) => {
          this.fullText = config.heroTitle;
          this.fullDescriptionLines = this.splitDescription(config.heroDescription);
          this.displayedDescriptionLines = new Array(this.fullDescriptionLines.length).fill('');

          this.fullName = config.fullName;

          this.githubUrl = config.githubUrl;
          this.linkedinUrl = config.linkedinUrl;
          this.contactEmail = config.email;

          this.seoService.updateMetaTags({
            title: config.siteTitle,
            description: config.seoDescription,
            image: config.profileImageUrl,
            url: '/',
            type: 'website',
          });

          this.seoService.setHomePageSchema({
            name: config.fullName,
            jobTitle: config.heroTitle,
            description: config.seoDescription,
            image: config.profileImageUrl,
            email: config.email,
            github: config.githubUrl,
            linkedin: config.linkedinUrl,
          });

          this.isLoadingHero = false;
          this.cdr.markForCheck();
          this.startTypingAnimation();
        },
        error: (err) => {
          this.logger.error('[HTTP_ERROR] Failed to load site configuration', {
            error: err.message || err,
          });
          // Fallback to default values
          this.fullText = 'Développeur Backend';
          this.fullDescriptionLines = ['Je crée des applications web modernes et évolutives.'];
          this.displayedDescriptionLines = [''];
          this.isLoadingHero = false;
          this.cdr.markForCheck();
          this.startTypingAnimation();
        },
      });
  }

  /**
   * Split description into lines for typing animation
   * Splits at natural break points (periods followed by space, or ~80 chars)
   */
  private splitDescription(description: string): string[] {
    const maxLineLength = 80;
    const lines: string[] = [];
    let remaining = description.trim();

    while (remaining.length > 0) {
      if (remaining.length <= maxLineLength) {
        lines.push(remaining);
        break;
      }

      // Find a good break point (space near the max length)
      let breakIndex = remaining.lastIndexOf(' ', maxLineLength);
      if (breakIndex === -1) {
        breakIndex = maxLineLength;
      }

      lines.push(remaining.substring(0, breakIndex));
      remaining = remaining.substring(breakIndex).trim();
    }

    return lines;
  }

  ngAfterViewInit(): void {
    const timeout = setTimeout(() => {
      this.initIntersectionObserver();
      this.initScrollAnimation();
      this.initBirdsAnimation();
    }, 100);
    this.pendingTimeouts.push(timeout);
  }

  /**
   * Initialize Lottie scroll animation
   */
  private initScrollAnimation(): void {
    if (this.scrollAnimationContainer) {
      this.scrollAnimation = lottie.loadAnimation({
        container: this.scrollAnimationContainer.nativeElement,
        renderer: 'svg',
        loop: true,
        autoplay: true,
        path: '/animations/scroll-down.json',
      });
    }
  }

  /**
   * Initialize Lottie birds animation
   */
  private initBirdsAnimation(): void {
    if (this.birdsAnimationContainer) {
      this.birdsAnimation = lottie.loadAnimation({
        container: this.birdsAnimationContainer.nativeElement,
        renderer: 'svg',
        loop: false,
        autoplay: true,
        path: '/animations/birds.json',
        rendererSettings: {
          preserveAspectRatio: 'xMidYMid slice',
        },
      });
    }
  }

  loadAllExperiences(): void {
    this.isLoadingExperiences = true;
    this.experiencesError = undefined;

    this.experienceService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (experiences) => {
          this.allExperiences = experiences;
          this.isLoadingExperiences = false;
          this.cdr.detectChanges();
          this.observeTimelineItems();
        },
        error: (err) => {
          this.logger.error('[HTTP_ERROR] Failed to load experiences', {
            error: err.message || err,
          });
          this.experiencesError = this.translate.instant('errors.loadExperiences');
          this.isLoadingExperiences = false;
          this.cdr.markForCheck();
        },
      });
  }

  getExperienceIcon(type: ExperienceType): string {
    const icons: Record<ExperienceType, string> = {
      [ExperienceType.WORK]: 'bi-briefcase',
      [ExperienceType.EDUCATION]: 'bi-mortarboard',
      [ExperienceType.CERTIFICATION]: 'bi-award',
      [ExperienceType.VOLUNTEERING]: 'bi-heart',
    };
    return icons[type] || 'bi-circle';
  }

  formatExperienceDateRange(startDate: string, endDate?: string): string {
    const currentLang = this.translate.currentLang || 'fr';
    const locale = getLocaleFromLang(currentLang);

    const start = new Date(startDate);
    const startStr = start.toLocaleDateString(locale, {
      month: 'short',
      year: 'numeric',
    });

    if (!endDate) {
      const presentLabel = this.translate.instant('experiences.ongoing');
      return `${startStr} - ${presentLabel}`;
    }

    const end = new Date(endDate);
    const endStr = end.toLocaleDateString(locale, {
      month: 'short',
      year: 'numeric',
    });

    return `${startStr} - ${endStr}`;
  }

  getExperienceTypeLabel(type: ExperienceType): string {
    const translationKey = `experiences.types.${type}`;
    return this.translate.instant(translationKey);
  }

  renderDescription(description: string): ReturnType<MarkdownService['toSafeHtml']> {
    return this.markdownService.toSafeHtml(description);
  }

  private initIntersectionObserver(): void {
    const options: IntersectionObserverInit = {
      threshold: 0.2,
      rootMargin: '0px',
    };

    this.intersectionObserver = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
          this.intersectionObserver?.unobserve(entry.target);
        }
      });
    }, options);

    this.observeTimelineItems();
  }

  /**
   * Observe timeline items for scroll animation
   * Called after initial render and after experiences are loaded
   */
  private observeTimelineItems(): void {
    if (!this.intersectionObserver) return;

    const timelineItems = document.querySelectorAll('.timeline-item:not(.visible)');
    timelineItems.forEach((item) => this.intersectionObserver?.observe(item));
  }

  /**
   * Load skills from API
   */
  loadSkills(): void {
    this.isLoadingSkills = true;
    this.skillsError = undefined;

    this.skillService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (skills) => {
          this.skills = skills;
          this.isLoadingSkills = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.logger.error('[HTTP_ERROR] Failed to load skills', { error: err.message || err });
          this.skillsError = this.translate.instant('errors.loadSkills');
          this.isLoadingSkills = false;
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Load all projects from API
   */
  loadProjects(): void {
    this.isLoadingProjects = true;
    this.projectsError = undefined;

    this.projectService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (projects) => {
          this.allProjects = projects.sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
          this.isLoadingProjects = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.logger.error('[HTTP_ERROR] Failed to load projects', { error: err.message || err });
          this.projectsError = this.translate.instant('errors.loadProjects');
          this.isLoadingProjects = false;
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Toggle show all projects
   */
  toggleShowAllProjects(): void {
    this.showAllProjects = !this.showAllProjects;

    if (this.showAllProjects) {
      // Scroll to projects section smoothly after expansion
      const timeout = setTimeout(() => {
        this.scrollService.scrollToElement('projects', 'smooth');
      }, 100);
      this.pendingTimeouts.push(timeout);
    }
  }

  /**
   * Get featured projects (those marked as featured)
   */
  get featuredProjects(): ProjectResponse[] {
    return this.allProjects.filter((p) => p.featured);
  }

  /**
   * Get non-featured projects
   */
  get otherProjects(): ProjectResponse[] {
    return this.allProjects.filter((p) => !p.featured);
  }

  /**
   * Get projects to display based on current state
   */
  get displayedProjects(): ProjectResponse[] {
    if (this.showAllProjects) {
      return this.allProjects;
    }
    return this.featuredProjects;
  }

  /**
   * Check if there are projects
   */
  get hasProjects(): boolean {
    return this.allProjects.length > 0;
  }

  /**
   * Check if there are more projects to show
   */
  get hasMoreProjects(): boolean {
    return this.otherProjects.length > 0;
  }

  /**
   * Load current CV from API
   * Returns null (via 204 No Content) when no CV has been uploaded
   */
  loadCv(): void {
    this.isLoadingCv = true;

    this.cvService
      .getCurrentCv()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (cv) => {
          this.currentCv = cv ?? undefined;
          this.isLoadingCv = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.logger.error('[HTTP_ERROR] Failed to load CV', {
            status: err.status,
            message: err.message,
          });
          this.currentCv = undefined;
          this.isLoadingCv = false;
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Download CV
   */
  downloadCv(): void {
    if (!this.currentCv) {
      this.toastr.info(this.translate.instant('cv.notAvailable'));
      return;
    }
    this.logger.info('[USER_ACTION] User clicked download CV button');
    this.cvService.downloadCv();
  }

  /**
   * Scroll to skills section with offset for navbar
   */
  scrollToSkills(event: Event): void {
    event.preventDefault();
    this.scrollService.scrollToElement('skills', 'smooth');
  }

  /**
   * Load published articles from API
   */
  loadArticles(): void {
    this.isLoadingArticles = true;
    this.articlesError = undefined;

    this.articleService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (articles) => {
          this.allArticles = articles.sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
          this.isLoadingArticles = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.logger.error('[HTTP_ERROR] Failed to load articles', { error: err.message || err });
          this.articlesError = this.translate.instant('errors.loadArticles');
          this.isLoadingArticles = false;
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Get latest 3 published articles
   */
  get latestArticles(): ArticleResponse[] {
    return this.allArticles.slice(0, 3);
  }

  /**
   * Check if there are articles
   */
  get hasArticles(): boolean {
    return this.allArticles.length > 0;
  }

  /**
   * Start typing animation for the hero title
   */
  private startTypingAnimation(): void {
    if (this.fullText.length === 0) {
      this.displayedText = '';
      this.showCursor = true;
      this.startDescriptionTypingAnimation();
      return;
    }

    const totalDuration = 1000;
    const typingSpeed = totalDuration / this.fullText.length;
    let currentIndex = 0;

    this.typingSubscription = interval(typingSpeed)
      .pipe(take(this.fullText.length))
      .subscribe({
        next: () => {
          currentIndex++;
          this.displayedText = this.fullText.substring(0, currentIndex);
          this.cdr.markForCheck();
        },
        complete: () => {
          this.showCursor = true;
          this.cdr.markForCheck();
        },
      });

    this.startDescriptionTypingAnimation();
  }

  /**
   * Start typing animation for the description paragraph
   */
  private startDescriptionTypingAnimation(): void {
    if (this.fullDescriptionLines.length === 0) {
      return;
    }

    const totalDuration = 1000;

    this.displayedDescriptionLines = new Array(this.fullDescriptionLines.length).fill('');

    this.fullDescriptionLines.forEach((line, lineIndex) => {
      if (line.length === 0) {
        this.displayedDescriptionLines[lineIndex] = '';
        return;
      }

      const typingSpeed = totalDuration / line.length;
      let currentIndex = 0;

      const subscription = interval(typingSpeed)
        .pipe(take(line.length))
        .subscribe({
          next: () => {
            currentIndex++;
            this.displayedDescriptionLines[lineIndex] = line.substring(0, currentIndex);
            this.cdr.markForCheck();
          },
        });

      this.descriptionTypingSubscriptions.push(subscription);
    });
  }

  /**
   * Clean up subscriptions and animations
   */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();

    this.pendingTimeouts.forEach((timeout) => clearTimeout(timeout));

    if (this.typingSubscription) {
      this.typingSubscription.unsubscribe();
    }
    this.descriptionTypingSubscriptions.forEach((sub) => {
      if (sub) {
        sub.unsubscribe();
      }
    });
    if (this.scrollAnimation) {
      this.scrollAnimation.destroy();
    }
    if (this.birdsAnimation) {
      this.birdsAnimation.destroy();
    }
    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
    }
  }
}
