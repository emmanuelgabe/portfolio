/**
 * Search result models for Elasticsearch full-text search.
 */

export interface ArticleSearchResult {
  id: number;
  title: string;
  slug: string;
  excerpt: string;
  draft: boolean;
  publishedAt: string;
  tags: string[];
}

export interface ProjectSearchResult {
  id: number;
  title: string;
  description: string;
  techStack: string;
  featured: boolean;
  tags: string[];
}

export interface ExperienceSearchResult {
  id: number;
  company: string;
  role: string;
  type: string;
  startDate: string;
  endDate?: string;
}
