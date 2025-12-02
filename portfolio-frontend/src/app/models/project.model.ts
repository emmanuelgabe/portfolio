import { ProjectImageResponse } from './project-image.model';
import { TagResponse } from './tag.model';

export interface ProjectResponse {
  id: number;
  title: string;
  description: string;
  techStack: string;
  githubUrl?: string;
  imageUrl?: string;
  thumbnailUrl?: string;
  demoUrl?: string;
  createdAt: string;
  updatedAt: string;
  featured: boolean;
  hasDetails: boolean;
  tags: TagResponse[];
  images: ProjectImageResponse[];
}

export interface CreateProjectRequest {
  title: string;
  description: string;
  techStack: string;
  githubUrl?: string;
  demoUrl?: string;
  featured?: boolean;
  hasDetails?: boolean;
  tagIds?: number[];
}

export interface UpdateProjectRequest {
  title?: string;
  description?: string;
  techStack?: string;
  githubUrl?: string;
  demoUrl?: string;
  featured?: boolean;
  hasDetails?: boolean;
  tagIds?: number[];
}
