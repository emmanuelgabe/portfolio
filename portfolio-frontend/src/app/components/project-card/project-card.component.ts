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
   * Removes Markdown syntax before truncating for clean display
   * @param text Text to truncate (may contain Markdown)
   * @param maxLength Maximum length
   * @returns Truncated text with ellipsis
   */
  truncateDescription(text: string, maxLength: number = 150): string {
    if (!text) return '';

    // Remove Markdown syntax for clean preview
    const plainText = text
      .replace(/#{1,6}\s+/g, '') // Remove headers
      .replace(/\*\*(.+?)\*\*/g, '$1') // Remove bold
      .replace(/\*(.+?)\*/g, '$1') // Remove italic
      .replace(/\[(.+?)\]\(.+?\)/g, '$1') // Remove links, keep text
      .replace(/`(.+?)`/g, '$1') // Remove inline code
      .replace(/^[-*+]\s+/gm, '') // Remove list markers
      .replace(/^\d+\.\s+/gm, '') // Remove numbered list markers
      .replace(/\n+/g, ' ') // Replace newlines with spaces
      .replace(/\s+/g, ' ') // Normalize whitespace
      .trim();

    if (plainText.length <= maxLength) {
      return plainText;
    }

    // Truncate at word boundary
    const truncated = plainText.substring(0, maxLength);
    const lastSpace = truncated.lastIndexOf(' ');
    return (lastSpace > 0 ? truncated.substring(0, lastSpace) : truncated).trim() + '...';
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
