/**
 * Response model for image upload operations
 * Contains URLs and metadata for uploaded optimized images
 */
export interface ImageUploadResponse {
  /**
   * URL to the full-size optimized image (max 1200px width, WebP format)
   */
  imageUrl: string;

  /**
   * URL to the thumbnail image (300x300px square crop, WebP format)
   */
  thumbnailUrl: string;

  /**
   * File size of the optimized image in bytes
   */
  fileSize: number;

  /**
   * Timestamp when the image was uploaded
   */
  uploadedAt: string;
}
