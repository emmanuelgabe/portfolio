import { HttpClient } from '@angular/common/http';
import { Injectable, NgZone, inject } from '@angular/core';
import { BehaviorSubject, Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ActionDecisionRequest,
  HealthMonitorSseEvent,
  HealthReport,
  ProposedAction,
} from '../models/health-monitor.model';
import { LoggerService } from './logger.service';
import { TokenStorageService } from './token-storage.service';

/**
 * Admin service for the AI Health Monitor agent.
 *
 * Responsibilities:
 * - REST calls to {@code /api/admin/health-monitor/**}
 * - Authenticated SSE stream consumption via fetch streaming
 *   (EventSource cannot send the Authorization header)
 * - Real-time broadcasting via BehaviorSubjects so the page can render
 *   incoming reports and action updates without polling
 */
@Injectable({
  providedIn: 'root',
})
export class HealthMonitorService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly ngZone = inject(NgZone);

  private readonly apiUrl = `${environment.apiUrl}/api/admin/health-monitor`;

  private readonly reportsSubject = new BehaviorSubject<HealthReport[]>([]);
  private readonly pendingActionsSubject = new BehaviorSubject<ProposedAction[]>([]);
  private readonly connectedSubject = new BehaviorSubject<boolean>(false);

  private abortController: AbortController | undefined;
  private reconnectAttempts = 0;
  private disposed = false;
  private readonly MAX_RECONNECT_ATTEMPTS = 5;
  private readonly RECONNECT_DELAY_MS = 5000;

  readonly reports$: Observable<HealthReport[]> = this.reportsSubject.asObservable();
  readonly pendingActions$: Observable<ProposedAction[]> =
    this.pendingActionsSubject.asObservable();
  readonly connected$: Observable<boolean> = this.connectedSubject.asObservable();

  // ========== Snapshots ==========

  getReportsSnapshot(): HealthReport[] {
    return this.reportsSubject.getValue();
  }

  getPendingActionsSnapshot(): ProposedAction[] {
    return this.pendingActionsSubject.getValue();
  }

  // ========== HTTP ==========

  /**
   * Load the latest {@code limit} reports (server-side sorted desc by date).
   */
  loadRecentReports(limit = 20): Observable<HealthReport[]> {
    const url = `${this.apiUrl}/reports?limit=${limit}`;
    return this.http.get<HealthReport[]>(url).pipe(
      catchError((error) => {
        this.logger.error('[HEALTH_MONITOR] Fetch reports failed', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Load all pending actions (awaiting admin decision).
   */
  loadPendingActions(): Observable<ProposedAction[]> {
    const url = `${this.apiUrl}/actions/pending`;
    return this.http.get<ProposedAction[]>(url).pipe(
      catchError((error) => {
        this.logger.error('[HEALTH_MONITOR] Fetch pending actions failed', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Load the full action history (all statuses, newest first).
   * Client-side filtering by status/level is applied in the component
   * because the backend endpoint does not support query filters.
   */
  loadActionHistory(limit = 50): Observable<ProposedAction[]> {
    const url = `${this.apiUrl}/actions?limit=${limit}`;
    return this.http.get<ProposedAction[]>(url).pipe(
      catchError((error) => {
        this.logger.error('[HEALTH_MONITOR] Fetch action history failed', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Trigger an on-demand diagnostic cycle. The backend may return
   * HTTP 204 when the pre-check was clean and the LLM was skipped;
   * in that case {@code null} is emitted.
   */
  triggerAnalysis(): Observable<HealthReport | undefined> {
    const url = `${this.apiUrl}/analyze-now`;
    this.logger.info('[HEALTH_MONITOR] POST analyze-now');
    return this.http.post<HealthReport>(url, {}, { observe: 'response' }).pipe(
      // Backend returns HTTP 204 when the pre-check is clean and the LLM
      // was skipped; normalise that into an absent report so the caller
      // can show a "no anomaly" toast without special-casing HttpResponse.
      map((response) => (response.status === 204 || !response.body ? undefined : response.body)),
      catchError((error) => {
        this.logger.error('[HEALTH_MONITOR] analyze-now failed', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  approveAction(id: number, request: ActionDecisionRequest): Observable<ProposedAction> {
    const url = `${this.apiUrl}/actions/${id}/approve`;
    this.logger.info('[HEALTH_MONITOR] POST approve', { id });
    return this.http.post<ProposedAction>(url, request).pipe(
      catchError((error) => {
        this.logger.error('[HEALTH_MONITOR] Approve failed', {
          id,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  rejectAction(id: number, request: ActionDecisionRequest): Observable<ProposedAction> {
    const url = `${this.apiUrl}/actions/${id}/reject`;
    this.logger.info('[HEALTH_MONITOR] POST reject', { id });
    return this.http.post<ProposedAction>(url, request).pipe(
      catchError((error) => {
        this.logger.error('[HEALTH_MONITOR] Reject failed', {
          id,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  // ========== Local state management ==========

  /**
   * Replace the in-memory report list (after initial load).
   */
  setReports(reports: HealthReport[]): void {
    this.reportsSubject.next(reports);
  }

  /**
   * Replace the in-memory pending actions list (after initial load).
   */
  setPendingActions(actions: ProposedAction[]): void {
    this.pendingActionsSubject.next(actions);
  }

  /**
   * Insert a new report at the top of the list.
   */
  prependReport(report: HealthReport): void {
    const current = this.reportsSubject.getValue();
    this.reportsSubject.next([report, ...current]);
  }

  /**
   * Insert a new pending action at the top of the list.
   */
  prependPendingAction(action: ProposedAction): void {
    const current = this.pendingActionsSubject.getValue();
    this.pendingActionsSubject.next([action, ...current]);
  }

  /**
   * Update an existing action in place or remove it from the pending list
   * if its new status is not {@code PENDING}.
   */
  upsertAction(action: ProposedAction): void {
    const current = this.pendingActionsSubject.getValue();
    const filtered = current.filter((a) => a.id !== action.id);

    if (action.status === 'PENDING') {
      this.pendingActionsSubject.next([action, ...filtered]);
    } else {
      this.pendingActionsSubject.next(filtered);
    }
  }

  // ========== SSE streaming ==========

  /**
   * Open an authenticated SSE stream and forward parsed events through the
   * BehaviorSubjects. The backend emits three named events: new_report,
   * new_pending_action, action_updated.
   */
  connect(): void {
    if (this.abortController) {
      this.logger.debug('[HEALTH_MONITOR] SSE already connected');
      return;
    }

    const token = this.tokenStorage.getAccessToken();
    if (!token) {
      this.logger.warn('[HEALTH_MONITOR] SSE skipped - no access token');
      return;
    }

    this.disposed = false;
    this.openStream(token);
  }

  /**
   * Close the SSE stream and prevent further reconnection attempts.
   */
  disconnect(): void {
    this.disposed = true;
    this.reconnectAttempts = this.MAX_RECONNECT_ATTEMPTS;
    if (this.abortController) {
      this.abortController.abort();
      this.abortController = undefined;
    }
    this.connectedSubject.next(false);
    this.logger.info('[HEALTH_MONITOR] SSE disconnected');
  }

  private async openStream(token: string): Promise<void> {
    try {
      this.abortController = new AbortController();
      const response = await fetch(`${this.apiUrl}/stream`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: 'text/event-stream',
        },
        signal: this.abortController.signal,
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      if (!response.body) {
        throw new Error('No response body');
      }

      this.ngZone.run(() => this.connectedSubject.next(true));
      this.reconnectAttempts = 0;
      this.logger.info('[HEALTH_MONITOR] SSE connected');

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          this.logger.info('[HEALTH_MONITOR] SSE stream ended');
          break;
        }

        buffer += decoder.decode(value, { stream: true });
        const blocks = buffer.split('\n\n');
        buffer = blocks.pop() ?? '';

        for (const block of blocks) {
          if (block.trim().length > 0) {
            this.parseEventBlock(block);
          }
        }
      }

      this.handleDisconnect();
    } catch (error) {
      if ((error as Error).name === 'AbortError') {
        this.logger.debug('[HEALTH_MONITOR] SSE aborted');
        return;
      }
      this.logger.error('[HEALTH_MONITOR] SSE connection error', {
        message: (error as Error).message,
      });
      this.handleDisconnect();
    }
  }

  private parseEventBlock(block: string): void {
    const lines = block.split('\n');
    let eventName = '';
    const dataParts: string[] = [];

    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim();
      } else if (line.startsWith('data:')) {
        dataParts.push(line.slice(5).trimStart());
      }
    }

    if (!eventName || dataParts.length === 0) {
      return;
    }

    try {
      const payload = JSON.parse(dataParts.join('\n')) as HealthReport | ProposedAction;
      this.dispatchEvent(eventName as HealthMonitorSseEvent, payload);
    } catch (err) {
      this.logger.warn('[HEALTH_MONITOR] Failed to parse SSE payload', {
        event: eventName,
        error: (err as Error).message,
      });
    }
  }

  private dispatchEvent(
    event: HealthMonitorSseEvent,
    payload: HealthReport | ProposedAction
  ): void {
    this.ngZone.run(() => {
      switch (event) {
        case 'new_report':
          this.prependReport(payload as HealthReport);
          break;
        case 'new_pending_action':
          this.prependPendingAction(payload as ProposedAction);
          break;
        case 'action_updated':
          this.upsertAction(payload as ProposedAction);
          break;
      }
    });
  }

  private handleDisconnect(): void {
    this.connectedSubject.next(false);
    this.abortController = undefined;

    if (this.disposed) {
      return;
    }

    if (this.reconnectAttempts < this.MAX_RECONNECT_ATTEMPTS) {
      this.reconnectAttempts++;
      this.logger.info('[HEALTH_MONITOR] SSE reconnecting', {
        attempt: this.reconnectAttempts,
        maxAttempts: this.MAX_RECONNECT_ATTEMPTS,
      });
      setTimeout(() => {
        const token = this.tokenStorage.getAccessToken();
        if (token && !this.disposed) {
          this.openStream(token);
        }
      }, this.RECONNECT_DELAY_MS);
    } else {
      this.logger.warn('[HEALTH_MONITOR] SSE max reconnect attempts reached');
    }
  }
}
