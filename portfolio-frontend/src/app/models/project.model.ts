import { TagResponse } from './tag.model';

export interface ProjectResponse {
  id: number;
  title: string;
  description: string;
  techStack: string;
  githubUrl?: string;
  imageUrl?: string;
  demoUrl?: string;
  createdAt: string;
  updatedAt: string;
  featured: boolean;
  tags: TagResponse[];
}

export interface CreateProjectRequest {
  title: string;
  description: string;
  techStack: string;
  githubUrl?: string;
  imageUrl?: string;
  demoUrl?: string;
  featured?: boolean;
  tagIds?: number[];
}

export interface UpdateProjectRequest {
  title?: string;
  description?: string;
  techStack?: string;
  githubUrl?: string;
  imageUrl?: string;
  demoUrl?: string;
  featured?: boolean;
  tagIds?: number[];
}
