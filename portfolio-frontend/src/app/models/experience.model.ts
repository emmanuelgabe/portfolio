export enum ExperienceType {
  WORK = 'WORK',
  EDUCATION = 'EDUCATION',
  CERTIFICATION = 'CERTIFICATION',
  VOLUNTEERING = 'VOLUNTEERING',
}

export interface ExperienceResponse {
  id: number;
  company: string;
  role: string;
  startDate: string;
  endDate?: string;
  description: string;
  type: ExperienceType;
  createdAt: string;
  updatedAt: string;
  ongoing: boolean;
}

export interface CreateExperienceRequest {
  company: string;
  role: string;
  startDate: string;
  endDate?: string;
  description: string;
  type: ExperienceType;
}

export interface UpdateExperienceRequest {
  company?: string;
  role?: string;
  startDate?: string;
  endDate?: string;
  description?: string;
  type?: ExperienceType;
}
