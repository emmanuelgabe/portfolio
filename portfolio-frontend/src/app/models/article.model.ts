import { ImageStatus } from './project-image.model';
import { TagResponse } from './tag.model';

/**
 * Article response from backend API
 */
export interface ArticleResponse {
  id: number;
  title: string;
  slug: string;
  content: string;
  contentHtml: string;
  excerpt: string;
  authorName: string;
  draft: boolean;
  publishedAt: string;
  createdAt: string;
  updatedAt: string;
  readingTimeMinutes: number;
  displayOrder: number;
  tags: TagResponse[];
  images: ArticleImageResponse[];
}

/**
 * Article image response
 */
export interface ArticleImageResponse {
  id: number;
  imageUrl: string;
  thumbnailUrl: string;
  uploadedAt: string;
  status: ImageStatus;
}

/**
 * Request to create a new article
 */
export interface CreateArticleRequest {
  title: string;
  content: string;
  excerpt?: string;
  draft?: boolean;
  tagIds?: number[];
}

/**
 * Request to update an existing article
 */
export interface UpdateArticleRequest {
  title?: string;
  content?: string;
  excerpt?: string;
  draft?: boolean;
  tagIds?: number[];
}

/**
 * Paginated response from Spring Data
 */
export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
