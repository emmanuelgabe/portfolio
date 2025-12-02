import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProjectResponse } from '../../models';

@Component({
  selector: 'app-project-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './project-card.component.html',
  styleUrls: ['./project-card.component.css'],
})
export class ProjectCardComponent {
  @Input() project!: ProjectResponse;

  /**
   * Truncate description to specified length
   * @param text Text to truncate
   * @param maxLength Maximum length
   * @returns Truncated text with ellipsis
   */
  truncateDescription(text: string, maxLength: number = 150): string {
    if (!text || text.length <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength).trim() + '...';
  }

  /**
   * Check if project has a valid GitHub URL
   */
  get hasGithubUrl(): boolean {
    return !!this.project.githubUrl && this.project.githubUrl.trim() !== '';
  }

  /**
   * Check if project has a valid demo URL
   */
  get hasDemoUrl(): boolean {
    return !!this.project.demoUrl && this.project.demoUrl.trim() !== '';
  }

  /**
   * Check if project has an image
   */
  get hasImage(): boolean {
    return !!this.project.thumbnailUrl || !!this.project.imageUrl;
  }

  /**
   * Get image source - prefers thumbnail for cards (optimized), falls back to full image or placeholder
   */
  get imageSrc(): string {
    return (
      this.project.thumbnailUrl || this.project.imageUrl || 'assets/images/project-placeholder.svg'
    );
  }
}
