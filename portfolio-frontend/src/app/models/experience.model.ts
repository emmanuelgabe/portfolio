export enum ExperienceType {
  WORK = 'WORK',
  STAGE = 'STAGE',
  EDUCATION = 'EDUCATION',
  CERTIFICATION = 'CERTIFICATION',
  VOLUNTEERING = 'VOLUNTEERING',
}

export interface ExperienceResponse {
  id: number;
  company?: string | null;
  role?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  description: string;
  type?: ExperienceType | null;
  showMonths: boolean;
  displayOrder?: number | null;
  createdAt: string;
  updatedAt: string;
  ongoing: boolean;
}

export interface CreateExperienceRequest {
  company?: string | null;
  role?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  description: string;
  type?: ExperienceType | null;
  showMonths: boolean;
}

export interface UpdateExperienceRequest {
  company?: string | null;
  role?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  description?: string;
  type?: ExperienceType | null;
  showMonths?: boolean;
}
