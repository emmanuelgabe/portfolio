export enum SkillCategory {
  FRONTEND = 'FRONTEND',
  BACKEND = 'BACKEND',
  DATABASE = 'DATABASE',
  DEVOPS = 'DEVOPS',
  TOOLS = 'TOOLS',
}

export enum IconType {
  FONT_AWESOME = 'FONT_AWESOME',
  CUSTOM_SVG = 'CUSTOM_SVG',
}

export interface Skill {
  id: number;
  name: string;
  icon?: string;
  iconType: IconType;
  customIconUrl?: string;
  color: string;
  category: SkillCategory;
  categoryDisplayName: string;
  displayOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSkillRequest {
  name: string;
  icon?: string;
  iconType: IconType;
  customIconUrl?: string;
  color: string;
  category: SkillCategory;
  displayOrder: number;
}

export interface UpdateSkillRequest {
  name?: string;
  icon?: string;
  iconType?: IconType;
  customIconUrl?: string;
  color?: string;
  category?: SkillCategory;
  displayOrder?: number;
}
