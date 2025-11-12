import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProjectCardComponent } from '../project-card/project-card.component';
import { ProjectService, SkillService } from '../../services';
import { ProjectResponse } from '../../models';
import { Skill } from '../../models/skill.model';
import { VERSION } from '../../../environments/version';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, ProjectCardComponent],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly skillService = inject(SkillService);

  featuredProjects: ProjectResponse[] = [];
  isLoadingProjects = false;
  projectsError: string | null = null;

  skills: Skill[] = [];
  isLoadingSkills = false;
  skillsError: string | null = null;

  version = VERSION;

  ngOnInit(): void {
    this.loadFeaturedProjects();
    this.loadSkills();
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
        console.error('Error loading skills:', err);
        this.skillsError = 'Unable to load skills';
        this.isLoadingSkills = false;
      }
    });
  }

  /**
   * Load featured projects from API
   */
  loadFeaturedProjects(): void {
    this.isLoadingProjects = true;
    this.projectsError = null;

    this.projectService.getFeatured().subscribe({
      next: (projects) => {
        this.featuredProjects = projects;
        this.isLoadingProjects = false;
      },
      error: (err) => {
        console.error('Error loading featured projects:', err);
        this.projectsError = 'Unable to load featured projects';
        this.isLoadingProjects = false;
      }
    });
  }

  /**
   * Check if there are featured projects
   */
  get hasFeaturedProjects(): boolean {
    return this.featuredProjects.length > 0;
  }
}
