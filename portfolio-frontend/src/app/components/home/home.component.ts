import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectCardComponent } from '../project-card/project-card.component';
import { ProjectService, SkillService } from '../../services';
import { CvService } from '../../services/cv.service';
import { ProjectResponse } from '../../models';
import { Skill } from '../../models/skill.model';
import { CvResponse } from '../../models/cv.model';
import { VERSION } from '../../../environments/version';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, ProjectCardComponent],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css'],
})
export class HomeComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly skillService = inject(SkillService);
  private readonly cvService = inject(CvService);
  private readonly logger = inject(LoggerService);

  allProjects: ProjectResponse[] = [];
  isLoadingProjects = false;
  projectsError: string | null = null;
  showAllProjects = false;

  skills: Skill[] = [];
  isLoadingSkills = false;
  skillsError: string | null = null;

  currentCv: CvResponse | null = null;
  isLoadingCv = false;

  version = VERSION;

  ngOnInit(): void {
    this.loadProjects();
    this.loadSkills();
    this.loadCv();
  }

  /**
   * Load skills from API
   */
  loadSkills(): void {
    this.isLoadingSkills = true;
    this.skillsError = null;

    this.skillService.getAll().subscribe({
      next: (skills) => {
        this.skills = skills;
        this.isLoadingSkills = false;
      },
      error: (err) => {
        this.logger.error('[HTTP_ERROR] Failed to load skills', { error: err.message || err });
        this.skillsError = 'Unable to load skills';
        this.isLoadingSkills = false;
      },
    });
  }

  /**
   * Load all projects from API
   */
  loadProjects(): void {
    this.isLoadingProjects = true;
    this.projectsError = null;

    this.projectService.getAll().subscribe({
      next: (projects) => {
        this.allProjects = projects;
        this.isLoadingProjects = false;
      },
      error: (err) => {
        this.logger.error('[HTTP_ERROR] Failed to load projects', { error: err.message || err });
        this.projectsError = 'Unable to load projects';
        this.isLoadingProjects = false;
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
      setTimeout(() => {
        const projectsSection = document.getElementById('projects');
        if (projectsSection) {
          projectsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      }, 100);
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
   */
  loadCv(): void {
    this.isLoadingCv = true;

    this.cvService.getCurrentCv().subscribe({
      next: (cv) => {
        this.currentCv = cv;
        this.isLoadingCv = false;
      },
      error: (err) => {
        this.logger.debug('[HTTP_ERROR] No CV available', { error: err.message || err });
        this.currentCv = null;
        this.isLoadingCv = false;
      },
    });
  }

  /**
   * Download CV
   */
  downloadCv(): void {
    this.logger.info('[USER_ACTION] User clicked download CV button');
    this.cvService.downloadCv();
  }
}
