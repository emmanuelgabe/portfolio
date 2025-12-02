import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, of, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ProjectService } from '../../../services/project.service';
import { SkillService } from '../../../services/skill.service';
import { CvService } from '../../../services/cv.service';
import { TagService } from '../../../services/tag.service';
import { ExperienceService } from '../../../services/experience.service';
import { ArticleService } from '../../../services/article.service';
import { LoggerService } from '../../../services/logger.service';
import { DemoModeService } from '../../../services/demo-mode.service';

interface DashboardStat {
  title: string;
  count: number;
  icon: string;
  color: string;
  link: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly projectService = inject(ProjectService);
  private readonly skillService = inject(SkillService);
  private readonly cvService = inject(CvService);
  private readonly tagService = inject(TagService);
  private readonly experienceService = inject(ExperienceService);
  private readonly articleService = inject(ArticleService);
  private readonly logger = inject(LoggerService);
  readonly demoModeService = inject(DemoModeService);
  private readonly destroy$ = new Subject<void>();

  stats: DashboardStat[] = [];
  loading = false;

  ngOnInit(): void {
    const prefix = this.demoModeService.isDemo() ? '/admindemo' : '/admin';

    this.stats = [
      {
        title: 'Projects',
        count: 0,
        icon: 'bi-folder',
        color: 'primary',
        link: `${prefix}/projects`,
      },
      {
        title: 'Compétences',
        count: 0,
        icon: 'bi-tools',
        color: 'success',
        link: `${prefix}/skills`,
      },
      {
        title: 'Tags',
        count: 0,
        icon: 'bi-tags',
        color: 'warning',
        link: `${prefix}/tags`,
      },
      {
        title: 'Experiences',
        count: 0,
        icon: 'bi-clock-history',
        color: 'secondary',
        link: `${prefix}/experiences`,
      },
      {
        title: 'Articles',
        count: 0,
        icon: 'bi-file-earmark-text',
        color: 'danger',
        link: `${prefix}/articles`,
      },
      {
        title: 'CVs',
        count: 0,
        icon: 'bi-file-earmark-pdf',
        color: 'info',
        link: `${prefix}/cv`,
      },
    ];

    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;
    this.logger.info('[HTTP_REQUEST] Loading dashboard stats');

    const isDemo = this.demoModeService.isDemo();

    forkJoin({
      projects: this.projectService.getAll(),
      skills: this.skillService.getAll(),
      tags: this.tagService.getAll(),
      experiences: this.experienceService.getAll(),
      articles: isDemo ? this.articleService.getAll() : this.articleService.getAllAdmin(),
      cvs: isDemo ? of([]) : this.cvService.getAllCvs(),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ projects, skills, tags, experiences, articles, cvs }) => {
          this.stats[0].count = projects.length;
          this.stats[1].count = skills.length;
          this.stats[2].count = tags.length;
          this.stats[3].count = experiences.length;
          this.stats[4].count = articles.length;
          this.stats[5].count = cvs.length;
          this.loading = false;
          this.logger.info('[HTTP_SUCCESS] Dashboard stats loaded', {
            projectCount: projects.length,
            skillCount: skills.length,
            tagCount: tags.length,
            experienceCount: experiences.length,
            articleCount: articles.length,
            cvCount: cvs.length,
          });
        },
        error: (error) => {
          this.stats[0].count = 0;
          this.stats[1].count = 0;
          this.stats[2].count = 0;
          this.stats[3].count = 0;
          this.stats[4].count = 0;
          this.stats[5].count = 0;
          this.loading = false;
          this.logger.error('[HTTP_ERROR] Failed to load dashboard stats', {
            status: error.status,
            message: error.message,
          });
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
