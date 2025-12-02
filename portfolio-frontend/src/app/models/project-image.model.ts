/**
 * Response DTO for project image data.
 */
export interface ProjectImageResponse {
  id: number;
  imageUrl: string;
  thumbnailUrl: string;
  altText?: string;
  caption?: string;
  displayOrder: number;
  primary: boolean;
  uploadedAt: string;
}

/**
 * Request DTO for updating project image metadata.
 */
export interface UpdateProjectImageRequest {
  altText?: string;
  caption?: string;
}

/**
 * Request DTO for reordering project images.
 */
export interface ReorderProjectImagesRequest {
  imageIds: number[];
}
