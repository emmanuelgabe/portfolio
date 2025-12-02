/**
 * CV Response model
 * Represents a CV file with version control
 */
export interface CvResponse {
  id: number;
  fileName: string;
  originalFileName: string;
  fileUrl: string;
  fileSize: number;
  uploadedAt: string;
  current: boolean;
}
