export enum SkillCategory {
  FRONTEND = 'FRONTEND',
  BACKEND = 'BACKEND',
  DATABASE = 'DATABASE',
  DEVOPS = 'DEVOPS',
  TOOLS = 'TOOLS',
}

export interface Skill {
  id: number;
  name: string;
  icon: string;
  color: string;
  category: SkillCategory;
  categoryDisplayName: string;
  level: number;
  displayOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSkillRequest {
  name: string;
  icon: string;
  color: string;
  category: SkillCategory;
  level: number;
  displayOrder: number;
}

export interface UpdateSkillRequest {
  name?: string;
  icon?: string;
  color?: string;
  category?: SkillCategory;
  level?: number;
  displayOrder?: number;
}
