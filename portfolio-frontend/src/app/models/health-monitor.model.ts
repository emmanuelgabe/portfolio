/**
 * TypeScript mirrors of the backend DTOs exposed under
 * {@code /api/admin/health-monitor/**}.
 *
 * Enums are modelled as string unions rather than TS enums so the compiler
 * catches any drift with the Java side without forcing a runtime import.
 */

export type ReportSeverity = 'OK' | 'WARNING' | 'CRITICAL' | 'UNKNOWN';

export type HealthCycleType = 'SCHEDULED' | 'ON_DEMAND' | 'PRECHECK_ONLY';

export type ActionLevel = 'GREEN' | 'ORANGE' | 'RED';

export type ActionStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXECUTED' | 'FAILED' | 'EXPIRED';

export type ActionType =
  | 'FLUSH_REDIS_CACHE'
  | 'RETRY_DLQ_MESSAGES'
  | 'REINDEX_ELASTICSEARCH'
  | 'TRIGGER_IMAGE_REPROCESSING'
  | 'RESTART_SERVICE';

/**
 * Matches backend {@code HealthReportResponse}. A diagnostic cycle snapshot.
 * {@code PRECHECK_ONLY} reports are generated when the cheap pre-check
 * succeeds, so {@code anomalies} and {@code toolCalls} are typically empty.
 */
export interface HealthReport {
  id: number;
  cycleType: HealthCycleType;
  severity: ReportSeverity;
  summary: string;
  anomalies: string[];
  toolCalls: string[];
  durationMs: number;
  createdAt: string;
}

/**
 * Matches backend {@code ProposedActionResponse}. Represents an action the
 * agent wants to execute, awaiting admin decision or already processed.
 */
export interface ProposedAction {
  id: number;
  reportId: number;
  level: ActionLevel;
  actionType: ActionType;
  params: Record<string, unknown>;
  justification: string;
  status: ActionStatus;
  approvedBy: string | undefined;
  approvedAt: string | undefined;
  executedAt: string | undefined;
  rejectionReason: string | undefined;
  resultMessage: string | undefined;
  expiresAt: string;
  createdAt: string;
}

/**
 * Body sent to approve/reject endpoints.
 * Reason is required for RED approvals and any rejection; the controller
 * returns HTTP 400 otherwise.
 */
export interface ActionDecisionRequest {
  reason: string | undefined;
}

/**
 * SSE event types emitted on {@code /api/admin/health-monitor/stream}.
 */
export type HealthMonitorSseEvent = 'new_report' | 'new_pending_action' | 'action_updated';

/**
 * Parsed SSE frame dispatched by the service. The {@code payload} type
 * depends on the event name:
 * - {@code new_report} -> {@link HealthReport}
 * - {@code new_pending_action} / {@code action_updated} -> {@link ProposedAction}
 */
export interface HealthMonitorStreamEvent {
  event: HealthMonitorSseEvent;
  payload: HealthReport | ProposedAction;
}
