import { Injectable, inject, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { interval, Subscription } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';

/**
 * Visitor Tracker Service
 * Sends periodic heartbeats to track active visitors on the public site.
 * Uses sessionStorage to maintain a consistent session ID across page refreshes.
 */
@Injectable({
  providedIn: 'root',
})
export class VisitorTrackerService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);

  private readonly SESSION_KEY = 'visitor_session_id';
  private readonly HEARTBEAT_INTERVAL_MS = 30000; // 30 seconds
  private readonly apiUrl = environment.apiUrl;

  private heartbeatSubscription?: Subscription;
  private isTracking = false;

  /**
   * Start tracking visitor activity.
   * Sends an immediate heartbeat, then continues every 30 seconds.
   */
  startTracking(): void {
    if (this.isTracking) {
      this.logger.debug('[VISITOR_TRACKER] Already tracking');
      return;
    }

    const sessionId = this.getOrCreateSessionId();
    this.isTracking = true;

    this.logger.info('[VISITOR_TRACKER] Started tracking', { sessionId });

    // Send immediate heartbeat
    this.sendHeartbeat(sessionId);

    // Send heartbeat every 30 seconds
    this.heartbeatSubscription = interval(this.HEARTBEAT_INTERVAL_MS).subscribe(() => {
      this.sendHeartbeat(sessionId);
    });
  }

  /**
   * Stop tracking visitor activity.
   */
  stopTracking(): void {
    if (this.heartbeatSubscription) {
      this.heartbeatSubscription.unsubscribe();
      this.heartbeatSubscription = undefined;
    }
    this.isTracking = false;
    this.logger.info('[VISITOR_TRACKER] Stopped tracking');
  }

  ngOnDestroy(): void {
    this.stopTracking();
  }

  /**
   * Send a heartbeat to the backend.
   */
  private sendHeartbeat(sessionId: string): void {
    this.http
      .post(`${this.apiUrl}/api/visitors/heartbeat`, null, {
        headers: { 'X-Session-Id': sessionId },
      })
      .subscribe({
        next: () => {
          this.logger.debug('[VISITOR_TRACKER] Heartbeat sent');
        },
        error: (error) => {
          this.logger.debug('[VISITOR_TRACKER] Heartbeat failed', {
            status: error.status,
          });
        },
      });
  }

  /**
   * Get existing session ID or create a new one.
   */
  private getOrCreateSessionId(): string {
    let sessionId = sessionStorage.getItem(this.SESSION_KEY);
    if (!sessionId) {
      sessionId = crypto.randomUUID();
      sessionStorage.setItem(this.SESSION_KEY, sessionId);
      this.logger.debug('[VISITOR_TRACKER] Created new session', { sessionId });
    }
    return sessionId;
  }
}
