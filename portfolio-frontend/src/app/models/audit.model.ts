/**
 * Audit log response from backend API
 */
export interface AuditLogResponse {
  id: number;
  action: string;
  actionDescription?: string;
  entityType: string;
  entityId?: number;
  entityName?: string;
  username: string;
  userRole?: string;
  ipAddress?: string;
  userAgent?: string;
  oldValues?: Record<string, unknown>;
  newValues?: Record<string, unknown>;
  changedFields?: string[];
  requestMethod?: string;
  requestUri?: string;
  success: boolean;
  errorMessage?: string;
  createdAt: string;
}

/**
 * Filter parameters for audit log queries
 */
export interface AuditLogFilter {
  action?: string;
  entityType?: string;
  entityId?: number;
  username?: string;
  success?: boolean;
  startDate?: string;
  endDate?: string;
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
