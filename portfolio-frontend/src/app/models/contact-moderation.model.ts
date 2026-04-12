/**
 * Contact categories assigned by AI moderation.
 * Mirrors backend enum ContactCategory.
 */
export type ContactCategory = 'MISSION' | 'QUESTION' | 'FEEDBACK' | 'SPAM' | 'INAPPROPRIATE';

/**
 * Moderation verdict for contact messages.
 * Mirrors backend enum ModerationVerdict.
 */
export type ModerationVerdict = 'APPROVED' | 'REJECTED';

/**
 * Aggregated statistics for contact moderation (admin dashboard).
 * Maps to backend ContactStatsResponse.
 */
export interface ContactStatsResponse {
  totalCount: number;
  categoryCounts: Record<string, number>;
  verdictCounts: Record<string, number>;
  dailyActivity: Record<string, number>;
}

/**
 * Individual contact entry in admin history.
 * Maps to backend ContactHistoryResponse.
 */
export interface ContactHistoryResponse {
  id: number;
  name: string;
  subject: string;
  message: string;
  category: ContactCategory;
  moderationVerdict: ModerationVerdict;
  moderationReason: string;
  confidenceScore: number;
  createdAt: string;
}
