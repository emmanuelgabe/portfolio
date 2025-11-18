import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ProjectService } from '../../../services/project.service';
import { SkillService } from '../../../services/skill.service';
import { LoggerService } from '../../../services/logger.service';

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
export class DashboardComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly skillService = inject(SkillService);
  private readonly logger = inject(LoggerService);

  stats: DashboardStat[] = [
    {
      title: 'Projects',
      count: 0,
      icon: 'bi-folder',
      color: 'primary',
      link: '/admin/projects',
    },
    {
      title: 'Skills',
      count: 0,
      icon: 'bi-tools',
      color: 'success',
      link: '/admin/skills',
    },
  ];

  loading = false;

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;
    this.logger.info('[HTTP_REQUEST] Loading dashboard stats');

    forkJoin({
      projects: this.projectService.getAll(),
      skills: this.skillService.getAll(),
    }).subscribe({
      next: ({ projects, skills }) => {
        this.stats[0].count = projects.length;
        this.stats[1].count = skills.length;
        this.loading = false;
        this.logger.info('[HTTP_SUCCESS] Dashboard stats loaded', {
          projectCount: projects.length,
          skillCount: skills.length,
        });
      },
      error: (error) => {
        this.stats[0].count = 0;
        this.stats[1].count = 0;
        this.loading = false;
        this.logger.error('[HTTP_ERROR] Failed to load dashboard stats', {
          status: error.status,
          message: error.message,
        });
      },
    });
  }
}
